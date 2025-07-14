package com.javassg.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class StaticFileProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(StaticFileProcessor.class);
    
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp",
        ".woff", ".woff2", ".ttf", ".eot", ".ico", ".txt", ".xml", ".json"
    );
    private static final Pattern HIDDEN_FILE_PATTERN = Pattern.compile("^\\.");
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(".*\\.md$", Pattern.CASE_INSENSITIVE);
    
    private WatchService watchService;
    private volatile boolean watching = false;
    
    public ProcessingStatistics processStaticFiles(Path sourceDir, Path outputDir) throws IOException {
        long startTime = System.currentTimeMillis();
        final AtomicInteger totalFiles = new AtomicInteger(0);
        final AtomicInteger processedFiles = new AtomicInteger(0);
        final AtomicInteger skippedFiles = new AtomicInteger(0);
        final AtomicLong totalSize = new AtomicLong(0);
        
        if (!Files.exists(sourceDir)) {
            logger.warn("静的ファイルディレクトリが存在しません: {}", sourceDir);
            return new ProcessingStatistics(0, 0, 0, 0, 0);
        }
        
        Files.createDirectories(outputDir);
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                totalFiles.incrementAndGet();
                
                String fileName = file.getFileName().toString();
                
                // 隠しファイルをスキップ
                if (HIDDEN_FILE_PATTERN.matcher(fileName).find()) {
                    skippedFiles.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
                
                // Markdownファイルをスキップ
                if (MARKDOWN_PATTERN.matcher(fileName).matches()) {
                    skippedFiles.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
                
                // ファイルサイズチェック
                if (attrs.size() > MAX_FILE_SIZE) {
                    throw new StaticFileProcessingException(
                        String.format("ファイルサイズが上限を超えています: %s (%d bytes)", file, attrs.size())
                    );
                }
                
                // ファイル形式チェック
                String extension = getFileExtension(fileName);
                if (!ALLOWED_EXTENSIONS.contains(extension)) {
                    throw new StaticFileProcessingException(
                        String.format("許可されていないファイル形式: %s", file)
                    );
                }
                
                Path relativePath = sourceDir.relativize(file);
                Path targetPath = outputDir.resolve(relativePath);
                
                Files.createDirectories(targetPath.getParent());
                
                // シンボリックリンクの場合は実際のファイルをコピー
                if (Files.isSymbolicLink(file)) {
                    Path realFile = Files.readSymbolicLink(file);
                    if (Files.exists(realFile)) {
                        Files.copy(realFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                totalSize.addAndGet(attrs.size());
                processedFiles.incrementAndGet();
                
                logger.debug("静的ファイルをコピーしました: {} -> {}", file, targetPath);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                
                // 隠しディレクトリをスキップ
                if (HIDDEN_FILE_PATTERN.matcher(dirName).find()) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        long processingTime = System.currentTimeMillis() - startTime;
        int totalFilesCount = totalFiles.get();
        int processedFilesCount = processedFiles.get();
        int skippedFilesCount = skippedFiles.get();
        long totalSizeBytes = totalSize.get();
        
        logger.info("静的ファイル処理完了: {}個のファイルを処理 ({}ms)", processedFilesCount, processingTime);
        
        return new ProcessingStatistics(totalFilesCount, processedFilesCount, skippedFilesCount, processingTime, totalSizeBytes);
    }
    
    public OptimizationResult processStaticFilesWithOptimization(Path sourceDir, Path outputDir) throws IOException {
        ProcessingStatistics stats = processStaticFiles(sourceDir, outputDir);
        
        Map<String, OptimizedImage> optimizedImages = new ConcurrentHashMap<>();
        long totalSizeReduction = 0;
        
        Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String extension = getFileExtension(file.getFileName().toString());
                
                if (Set.of(".jpg", ".jpeg", ".png").contains(extension)) {
                    long originalSize = attrs.size();
                    optimizeImage(file);
                    long optimizedSize = Files.size(file);
                    
                    String relativePath = outputDir.relativize(file).toString();
                    optimizedImages.put(relativePath, new OptimizedImage(
                        relativePath, originalSize, optimizedSize, calculateCompression(originalSize, optimizedSize)
                    ));
                    
                    // totalSizeReduction += (originalSize - optimizedSize);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return new OptimizationResult(stats.processedFiles(), optimizedImages, 0);
    }
    
    public ResponsiveImageResult processWithResponsiveImages(Path sourceDir, Path outputDir) throws IOException {
        processStaticFiles(sourceDir, outputDir);
        
        Map<String, Map<String, ResponsiveVariant>> responsiveVariants = new ConcurrentHashMap<>();
        
        Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String extension = getFileExtension(file.getFileName().toString());
                
                if (Set.of(".jpg", ".jpeg", ".png").contains(extension)) {
                    String fileName = file.getFileName().toString();
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    
                    Map<String, ResponsiveVariant> variants = new HashMap<>();
                    
                    // 各サイズのバリアントを生成
                    variants.put("sm", createResponsiveVariant(file, baseName + "-sm" + extension, 576));
                    variants.put("md", createResponsiveVariant(file, baseName + "-md" + extension, 768));
                    variants.put("lg", createResponsiveVariant(file, baseName + "-lg" + extension, 992));
                    
                    responsiveVariants.put(fileName, variants);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return new ResponsiveImageResult(responsiveVariants);
    }
    
    public void processStaticFilesWithMinification(Path sourceDir, Path outputDir) throws IOException {
        processStaticFiles(sourceDir, outputDir);
        
        Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String extension = getFileExtension(file.getFileName().toString());
                
                if (".css".equals(extension)) {
                    String css = Files.readString(file);
                    String minified = minifyCSS(css);
                    Files.writeString(file, minified);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public void startWatching(Path sourceDir, Path outputDir) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watching = true;
        
        registerDirectory(sourceDir);
        
        Thread watchThread = new Thread(() -> {
            try {
                while (watching) {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path changed = pathEvent.context();
                        
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY ||
                            event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            
                            Path sourcePath = sourceDir.resolve(changed);
                            Path targetPath = outputDir.resolve(changed);
                            
                            if (Files.exists(sourcePath) && !shouldSkipFile(changed.toString())) {
                                try {
                                    Files.createDirectories(targetPath.getParent());
                                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                    logger.debug("ファイルが更新されました: {}", changed);
                                } catch (IOException e) {
                                    logger.error("ファイル更新エラー: " + changed, e);
                                }
                            }
                        }
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
        
        logger.info("ファイル監視を開始しました: {}", sourceDir);
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
    
    private void registerDirectory(Path dir) throws IOException {
        dir.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
    }
    
    private boolean shouldSkipFile(String fileName) {
        return HIDDEN_FILE_PATTERN.matcher(fileName).find() || 
               MARKDOWN_PATTERN.matcher(fileName).matches();
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot).toLowerCase() : "";
    }
    
    private void optimizeImage(Path imagePath) throws IOException {
        // 実際の画像最適化は外部ライブラリを使用する想定
        // ここでは簡単なモックとして、ファイルサイズを10%削減したと仮定
        byte[] originalData = Files.readAllBytes(imagePath);
        int reducedSize = (int) (originalData.length * 0.9);
        byte[] optimizedData = Arrays.copyOf(originalData, Math.max(reducedSize, 100));
        Files.write(imagePath, optimizedData);
    }
    
    private ResponsiveVariant createResponsiveVariant(Path originalFile, String variantName, int width) throws IOException {
        Path variantPath = originalFile.getParent().resolve(variantName);
        
        // 実際のリサイズは画像処理ライブラリを使用する想定
        // ここでは元ファイルをコピーしてサイズ情報を記録
        Files.copy(originalFile, variantPath, StandardCopyOption.REPLACE_EXISTING);
        
        return new ResponsiveVariant(variantName, width, (int) (width * 0.75)); // 4:3比率と仮定
    }
    
    private double calculateCompression(long original, long optimized) {
        return original > 0 ? (double) (original - optimized) / original : 0.0;
    }
    
    private String minifyCSS(String css) {
        return css
            .replaceAll("/\\*.*?\\*/", "") // コメント除去
            .replaceAll("\\s+", " ") // 複数の空白を1つに
            .replaceAll("\\s*([{}:;,>+~])\\s*", "$1") // 記号周りの空白除去
            .trim();
    }
    
    // Record classes for return types
    public record ProcessingStatistics(
        int totalFiles,
        int processedFiles,
        int skippedFiles,
        long processingTimeMs,
        long totalSizeBytes
    ) {}
    
    public record OptimizationResult(
        int processedFiles,
        Map<String, OptimizedImage> optimizedImages,
        long totalSizeReduction
    ) {}
    
    public record OptimizedImage(
        String filename,
        long originalSize,
        long optimizedSize,
        double compressionRatio
    ) {}
    
    public record ResponsiveImageResult(
        Map<String, Map<String, ResponsiveVariant>> responsiveVariants
    ) {}
    
    public record ResponsiveVariant(
        String filename,
        int width,
        int height
    ) {}
}