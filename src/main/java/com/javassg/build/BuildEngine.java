package com.javassg.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javassg.cache.CacheManager;
import com.javassg.model.*;
import com.javassg.parser.MarkdownParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BuildEngine implements BuildEngineInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(BuildEngine.class);
    
    private final SiteConfig siteConfig;
    private final CacheManager cacheManager;
    private final MarkdownParser markdownParser;
    private final StaticFileProcessor staticFileProcessor;
    private final HtmlGenerator htmlGenerator;
    private final ObjectMapper objectMapper;
    
    private WatchService watchService;
    private volatile boolean watching = false;
    
    public BuildEngine(SiteConfig siteConfig, CacheManager cacheManager, MarkdownParser markdownParser, 
                      StaticFileProcessor staticFileProcessor, HtmlGenerator htmlGenerator) {
        this.siteConfig = siteConfig;
        this.cacheManager = cacheManager;
        this.markdownParser = markdownParser;
        this.staticFileProcessor = staticFileProcessor;
        this.htmlGenerator = htmlGenerator;
        this.objectMapper = new ObjectMapper();
    }
    
    // CLI用の簡易コンストラクタ
    public BuildEngine(SiteConfig siteConfig, Path workingDir) {
        this.siteConfig = siteConfig;
        this.cacheManager = new CacheManager();
        this.markdownParser = new MarkdownParser();
        this.staticFileProcessor = new StaticFileProcessor();
        this.htmlGenerator = new HtmlGenerator(siteConfig, this.cacheManager);
        this.objectMapper = new ObjectMapper();
    }
    
    public BuildResult build() {
        return buildInternal(false, false, false);
    }
    
    public BuildResult buildIncremental(LocalDateTime lastBuild) {
        return buildInternal(true, false, false, lastBuild);
    }
    
    public BuildResult buildWithDrafts() {
        return buildInternal(false, true, false);
    }
    
    public BuildResult buildForProduction() {
        return buildInternal(false, false, true);
    }
    
    public BuildResult buildWithValidation() {
        return buildInternal(false, false, false);
    }
    
    private BuildResult buildInternal(boolean incremental, boolean includeDrafts, boolean production, LocalDateTime... lastBuild) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            logger.info("ビルドを開始します (incremental={}, drafts={}, production={})", incremental, includeDrafts, production);
            
            // プロジェクト構造の検証
            validateProjectStructure();
            
            Path contentDir = getContentDirectory();
            Path templatesDir = getTemplatesDirectory();
            Path staticDir = getStaticDirectory();
            Path outputDir = getOutputDirectory();
            
            // 出力ディレクトリの準備
            if (!incremental) {
                clean();
            }
            Files.createDirectories(outputDir);
            
            // テンプレートの読み込み
            loadTemplates(templatesDir);
            
            // コンテンツの解析
            List<Page> pages = loadPages(contentDir, incremental ? lastBuild[0] : null);
            List<Post> posts = loadPosts(contentDir.resolve("posts"), includeDrafts, incremental ? lastBuild[0] : null);
            
            // 静的ファイルの処理
            StaticFileProcessor.ProcessingStatistics staticStats = 
                staticFileProcessor.processStaticFiles(staticDir, outputDir);
            
            // HTMLの生成
            int generatedFiles = generateAllHtml(pages, posts, outputDir, production);
            
            // マニフェストファイルの生成
            generateAssetManifest(outputDir, staticStats);
            
            // 検索インデックスの生成
            generateSearchIndex(pages, posts, outputDir);
            
            // サイトマップとRSSの生成
            generateSitemapAndRss(pages, posts, outputDir);
            
            long buildTime = System.currentTimeMillis() - startTime;
            
            BuildStatistics stats = new BuildStatistics(
                pages.size() + posts.size(), // totalFiles
                pages.size(), // contentFiles
                staticStats.processedFiles(), // staticFiles
                getTemplateCount(), // templateFiles
                calculateOutputSize(outputDir) // outputSize
            );
            
            logger.info("ビルド完了: {}ms, {}ページ, {}投稿, {}ファイル生成", 
                       buildTime, pages.size(), posts.size(), generatedFiles);
            
            return new BuildResult(
                true, // success
                buildTime,
                pages.size(),
                posts.size(),
                generatedFiles,
                incremental ? getModifiedFileCount(lastBuild[0]) : 0,
                incremental,
                production,
                stats,
                errors,
                warnings
            );
            
        } catch (Exception e) {
            long buildTime = System.currentTimeMillis() - startTime;
            logger.error("ビルドエラー", e);
            errors.add("ビルドエラー: " + e.getMessage());
            
            return new BuildResult(
                false, // success
                buildTime,
                0, 0, 0, 0,
                incremental,
                production,
                null,
                errors,
                warnings
            );
        }
    }
    
    public void clean() throws IOException {
        Path outputDir = getOutputDirectory();
        if (Files.exists(outputDir)) {
            Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (!dir.equals(outputDir)) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            logger.info("出力ディレクトリをクリアしました: {}", outputDir);
        }
    }
    
    public void startWatching() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watching = true;
        
        Path contentDir = getContentDirectory();
        Path templatesDir = getTemplatesDirectory();
        Path staticDir = getStaticDirectory();
        
        registerDirectoryRecursively(contentDir);
        registerDirectoryRecursively(templatesDir);
        registerDirectoryRecursively(staticDir);
        
        Thread watchThread = new Thread(() -> {
            try {
                while (watching) {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        
                        logger.info("ファイル変更を検知しました: {}", event.context());
                        
                        // 変更を検知したら増分ビルドを実行
                        CompletableFuture.runAsync(() -> {
                            try {
                                buildIncremental(LocalDateTime.now().minusMinutes(1));
                            } catch (Exception e) {
                                logger.error("増分ビルドエラー", e);
                            }
                        });
                    }
                    
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("ファイル監視が中断されました");
            } catch (Exception e) {
                logger.error("ファイル監視エラー", e);
            }
        });
        
        watchThread.setDaemon(true);
        watchThread.start();
        
        logger.info("ファイル監視を開始しました");
    }
    
    public void stopWatching() {
        this.watching = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            logger.error("ファイル監視停止エラー", e);
        }
        logger.info("ファイル監視を停止しました");
    }
    
    private void validateProjectStructure() {
        Path contentDir = getContentDirectory();
        if (!Files.exists(contentDir)) {
            throw new BuildException("contentディレクトリが存在しません: " + contentDir);
        }
        
        Path templatesDir = getTemplatesDirectory();
        if (!Files.exists(templatesDir)) {
            throw new BuildException("templatesディレクトリが存在しません: " + templatesDir);
        }
    }
    
    private void loadTemplates(Path templatesDir) throws IOException {
        if (!Files.exists(templatesDir)) {
            logger.warn("テンプレートディレクトリが存在しません: {}", templatesDir);
            return;
        }
        
        Files.walkFileTree(templatesDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".html")) {
                    String name = templatesDir.relativize(file).toString();
                    name = name.substring(0, name.lastIndexOf('.'));
                    
                    String content = Files.readString(file);
                    Template template = new Template(name, content);
                    
                    cacheManager.cacheTemplate(name, template);
                    logger.debug("テンプレートを読み込みました: {}", name);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private List<Page> loadPages(Path contentDir, LocalDateTime lastBuild) throws IOException {
        List<Page> pages = new ArrayList<>();
        
        Files.walkFileTree(contentDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md") && !file.getParent().endsWith("posts")) {
                    
                    // 増分ビルドの場合、最終ビルド以降に変更されたファイルのみ処理
                    if (lastBuild != null && attrs.lastModifiedTime().toInstant()
                        .isBefore(lastBuild.atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    try {
                        Page page = markdownParser.parsePage(file);
                        pages.add(page);
                        cacheManager.cachePage(page.slug(), page);
                        logger.debug("ページを読み込みました: {}", page.slug());
                    } catch (Exception e) {
                        logger.error("ページ解析エラー: " + file, e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return pages;
    }
    
    private List<Post> loadPosts(Path postsDir, boolean includeDrafts, LocalDateTime lastBuild) throws IOException {
        List<Post> posts = new ArrayList<>();
        
        if (!Files.exists(postsDir)) {
            logger.info("投稿ディレクトリが存在しません: {}", postsDir);
            return posts;
        }
        
        Files.walkFileTree(postsDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".md")) {
                    
                    // 増分ビルドの場合、最終ビルド以降に変更されたファイルのみ処理
                    if (lastBuild != null && attrs.lastModifiedTime().toInstant()
                        .isBefore(lastBuild.atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    try {
                        Post post = markdownParser.parsePost(file);
                        
                        // 下書きの処理
                        if (!includeDrafts && !post.isPublished()) {
                            return FileVisitResult.CONTINUE;
                        }
                        
                        posts.add(post);
                        cacheManager.cachePost(post.slug(), post);
                        logger.debug("投稿を読み込みました: {}", post.slug());
                    } catch (Exception e) {
                        logger.error("投稿解析エラー: " + file, e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        // 投稿を公開日時でソート
        posts.sort((a, b) -> b.publishedAt().compareTo(a.publishedAt()));
        
        return posts;
    }
    
    private int generateAllHtml(List<Page> pages, List<Post> posts, Path outputDir, boolean production) throws IOException {
        int generatedFiles = 0;
        
        // ページのHTML生成
        for (Page page : pages) {
            String html = htmlGenerator.generatePageHtml(page, "page");
            if (production) {
                html = htmlGenerator.minifyHtml(html);
            }
            
            Path outputPath = outputDir.resolve(page.slug() + ".html");
            htmlGenerator.writeHtmlToFile(html, outputPath);
            generatedFiles++;
        }
        
        // 投稿のHTML生成
        for (Post post : posts) {
            String html = htmlGenerator.generatePostHtml(post, "post");
            if (production) {
                html = htmlGenerator.minifyHtml(html);
            }
            
            Path outputPath = outputDir.resolve(post.slug() + ".html");
            htmlGenerator.writeHtmlToFile(html, outputPath);
            generatedFiles++;
        }
        
        // インデックスページの生成
        if (!posts.isEmpty()) {
            String indexHtml = htmlGenerator.generateIndexPage(posts, "index");
            if (production) {
                indexHtml = htmlGenerator.minifyHtml(indexHtml);
            }
            
            Path indexPath = outputDir.resolve("index.html");
            htmlGenerator.writeHtmlToFile(indexHtml, indexPath);
            generatedFiles++;
        }
        
        // アーカイブページの生成
        if (!posts.isEmpty()) {
            String archiveHtml = htmlGenerator.generateArchivePage(posts, "archive");
            if (production) {
                archiveHtml = htmlGenerator.minifyHtml(archiveHtml);
            }
            
            Path archivePath = outputDir.resolve("archive.html");
            htmlGenerator.writeHtmlToFile(archiveHtml, archivePath);
            generatedFiles++;
        }
        
        // カテゴリページの生成
        Map<String, String> categoryPages = htmlGenerator.generateCategoryPages(posts, "category");
        for (Map.Entry<String, String> entry : categoryPages.entrySet()) {
            String html = entry.getValue();
            if (production) {
                html = htmlGenerator.minifyHtml(html);
            }
            
            Path categoryPath = outputDir.resolve("category").resolve(entry.getKey() + ".html");
            htmlGenerator.writeHtmlToFile(html, categoryPath);
            generatedFiles++;
        }
        
        return generatedFiles;
    }
    
    private void generateAssetManifest(Path outputDir, StaticFileProcessor.ProcessingStatistics staticStats) throws IOException {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("version", "1.0.0");
        manifest.put("buildTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        manifest.put("staticFiles", staticStats.processedFiles());
        manifest.put("totalSize", staticStats.totalSizeBytes());
        
        Path manifestPath = outputDir.resolve("manifest.json");
        objectMapper.writeValue(manifestPath.toFile(), manifest);
        logger.debug("アセットマニフェストを生成しました: {}", manifestPath);
    }
    
    private void generateSearchIndex(List<Page> pages, List<Post> posts, Path outputDir) throws IOException {
        List<Map<String, Object>> searchIndex = new ArrayList<>();
        
        // ページのインデックス
        for (Page page : pages) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", page.title());
            item.put("url", "/" + page.slug() + ".html");
            item.put("content", page.rawContent());
            item.put("type", "page");
            searchIndex.add(item);
        }
        
        // 投稿のインデックス
        for (Post post : posts) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", post.title());
            item.put("url", "/" + post.slug() + ".html");
            item.put("content", post.rawContent());
            item.put("type", "post");
            item.put("categories", post.categories());
            item.put("tags", post.tags());
            searchIndex.add(item);
        }
        
        Path searchIndexPath = outputDir.resolve("search-index.json");
        objectMapper.writeValue(searchIndexPath.toFile(), searchIndex);
        logger.debug("検索インデックスを生成しました: {}", searchIndexPath);
    }
    
    private void generateSitemapAndRss(List<Page> pages, List<Post> posts, Path outputDir) throws IOException {
        // サイトマップ
        String sitemap = htmlGenerator.generateSitemap(pages, posts);
        Files.writeString(outputDir.resolve("sitemap.xml"), sitemap);
        
        // RSS
        String rss = htmlGenerator.generateRssFeed(posts);
        Files.writeString(outputDir.resolve("rss.xml"), rss);
        
        logger.debug("サイトマップとRSSフィードを生成しました");
    }
    
    private void registerDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private Path getContentDirectory() {
        String contentDir = siteConfig.getContentDirectory();
        return Paths.get(contentDir != null ? contentDir : "content");
    }
    
    private Path getTemplatesDirectory() {
        String templatesDir = siteConfig.getTemplatesDirectory();
        return Paths.get(templatesDir != null ? templatesDir : "templates");
    }
    
    private Path getStaticDirectory() {
        String staticDir = siteConfig.getStaticDirectory();
        return Paths.get(staticDir != null ? staticDir : "static");
    }
    
    private Path getOutputDirectory() {
        String outputDir = siteConfig.getOutputDirectory();
        return Paths.get(outputDir != null ? outputDir : "_site");
    }
    
    private int getTemplateCount() {
        return cacheManager.getTemplateCount();
    }
    
    private long calculateOutputSize(Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            return 0;
        }
        
        return Files.walk(outputDir)
            .filter(Files::isRegularFile)
            .mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }
    
    private int getModifiedFileCount(LocalDateTime lastBuild) {
        try {
            Path contentDir = getContentDirectory();
            Path templatesDir = getTemplatesDirectory();
            Path staticDir = getStaticDirectory();
            
            return (int) Files.walk(contentDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isAfter(lastBuild.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .count() +
                (int) Files.walk(templatesDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isAfter(lastBuild.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .count() +
                (int) Files.walk(staticDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isAfter(lastBuild.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .count();
        } catch (IOException e) {
            logger.error("変更ファイル数の計算エラー", e);
            return 0;
        }
    }
    
    // Record classes for return types
    public record BuildResult(
        boolean success,
        long buildTimeMs,
        int totalPages,
        int totalPosts,
        int generatedFiles,
        int modifiedFiles,
        boolean incremental,
        boolean optimized,
        BuildStatistics statistics,
        List<String> errors,
        List<String> warnings
    ) {}
    
    public record BuildStatistics(
        int totalFiles,
        int contentFiles,
        int staticFiles,
        int templateFiles,
        long outputSize
    ) {}
}