package com.javassg.build;

import com.javassg.cache.CacheManager;
import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.SiteConfig;
import com.javassg.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);
    
    private final SiteConfig siteConfig;
    private final CacheManager cacheManager;
    
    public HtmlGenerator(SiteConfig siteConfig, CacheManager cacheManager) {
        this.siteConfig = siteConfig;
        this.cacheManager = cacheManager;
    }
    
    public String generatePageHtml(Page page, String templateName) {
        Template template = getTemplate(templateName);
        
        Map<String, Object> context = new HashMap<>();
        context.put("site", createSiteContext());
        context.put("page", createPageContext(page));
        
        String rendered = template.render(context);
        
        // レンダリング結果をキャッシュ
        String cacheKey = "page:" + page.slug();
        cacheManager.cacheRendered(cacheKey, rendered, LocalDateTime.now());
        
        return rendered;
    }
    
    public String generatePostHtml(Post post, String templateName) {
        Template template = getTemplate(templateName);
        
        Map<String, Object> context = new HashMap<>();
        context.put("site", createSiteContext());
        context.put("post", createPostContext(post));
        
        String rendered = template.render(context);
        
        // レンダリング結果をキャッシュ
        String cacheKey = "post:" + post.slug();
        cacheManager.cacheRendered(cacheKey, rendered, LocalDateTime.now());
        
        return rendered;
    }
    
    public String generateIndexPage(List<Post> posts, String templateName) {
        Template template = getTemplate(templateName);
        
        Map<String, Object> context = new HashMap<>();
        context.put("site", createSiteContext());
        context.put("posts", posts.stream()
            .map(this::createPostContext)
            .collect(Collectors.toList()));
        
        return template.render(context);
    }
    
    public String generateArchivePage(List<Post> posts, String templateName) {
        Template template = getTemplate(templateName);
        
        // 年別にグループ化
        Map<Integer, List<Post>> postsByYear = posts.stream()
            .collect(Collectors.groupingBy(post -> post.publishedAt().getYear()));
        
        List<Map<String, Object>> years = postsByYear.entrySet().stream()
            .sorted(Map.Entry.<Integer, List<Post>>comparingByKey().reversed())
            .map(entry -> {
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", entry.getKey());
                yearData.put("posts", entry.getValue().stream()
                    .map(this::createPostContext)
                    .collect(Collectors.toList()));
                return yearData;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> context = new HashMap<>();
        context.put("site", createSiteContext());
        context.put("years", years);
        
        return template.render(context);
    }
    
    public Map<String, String> generateCategoryPages(List<Post> posts, String templateName) {
        Template template = getTemplate(templateName);
        Map<String, String> categoryPages = new HashMap<>();
        
        // カテゴリ別にグループ化
        Map<String, List<Post>> postsByCategory = new HashMap<>();
        for (Post post : posts) {
            for (String category : post.categories()) {
                postsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(post);
            }
        }
        
        for (Map.Entry<String, List<Post>> entry : postsByCategory.entrySet()) {
            String category = entry.getKey();
            List<Post> categoryPosts = entry.getValue();
            
            Map<String, Object> context = new HashMap<>();
            context.put("site", createSiteContext());
            context.put("category", category);
            context.put("posts", categoryPosts.stream()
                .map(this::createPostContext)
                .collect(Collectors.toList()));
            
            String html = template.render(context);
            categoryPages.put(category, html);
        }
        
        return categoryPages;
    }
    
    public Map<String, String> generatePaginatedPages(List<Post> posts, String templateName, int postsPerPage) {
        Template template = getTemplate(templateName);
        Map<String, String> pages = new HashMap<>();
        
        int totalPages = (int) Math.ceil((double) posts.size() / postsPerPage);
        
        for (int page = 1; page <= totalPages; page++) {
            int startIndex = (page - 1) * postsPerPage;
            int endIndex = Math.min(startIndex + postsPerPage, posts.size());
            
            List<Post> pagePosts = posts.subList(startIndex, endIndex);
            
            Map<String, Object> context = new HashMap<>();
            context.put("site", createSiteContext());
            context.put("posts", pagePosts.stream()
                .map(this::createPostContext)
                .collect(Collectors.toList()));
            context.put("currentPage", page);
            context.put("totalPages", totalPages);
            context.put("hasPrevious", page > 1);
            context.put("hasNext", page < totalPages);
            context.put("previousPage", page - 1);
            context.put("nextPage", page + 1);
            
            String html = template.render(context);
            pages.put("page" + page, html);
        }
        
        return pages;
    }
    
    public void writeHtmlToFile(String html, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, html);
        logger.debug("HTMLファイルを書き込みました: {}", outputPath);
    }
    
    public boolean validateHtml(String html) {
        // 簡易HTML検証
        // より厳密な検証には外部ライブラリを使用することを推奨
        Stack<String> tagStack = new Stack<>();
        
        // 基本的なタグの開始・終了をチェック
        String[] selfClosingTags = {"br", "hr", "img", "input", "meta", "link"};
        Set<String> selfClosing = Set.of(selfClosingTags);
        
        boolean inTag = false;
        StringBuilder currentTag = new StringBuilder();
        
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            
            if (c == '<') {
                inTag = true;
                currentTag.setLength(0);
            } else if (c == '>' && inTag) {
                inTag = false;
                String tag = currentTag.toString().trim();
                
                if (tag.startsWith("/")) {
                    // 終了タグ
                    String tagName = tag.substring(1).toLowerCase();
                    if (tagStack.isEmpty() || !tagStack.pop().equals(tagName)) {
                        return false;
                    }
                } else if (!tag.endsWith("/") && !selfClosing.contains(tag.split(" ")[0].toLowerCase())) {
                    // 開始タグ
                    String tagName = tag.split(" ")[0].toLowerCase();
                    tagStack.push(tagName);
                }
            } else if (inTag) {
                currentTag.append(c);
            }
        }
        
        return tagStack.isEmpty();
    }
    
    public String minifyHtml(String html) {
        return html
            .replaceAll("<!--.*?-->", "") // コメント除去
            .replaceAll("\\s*\\n\\s*", "") // 改行と前後の空白除去
            .replaceAll("\\s{2,}", " ") // 複数の空白を1つに
            .replaceAll(">\\s+<", "><") // タグ間の空白除去
            .trim();
    }
    
    public String generateSitemap(List<Page> pages, List<Post> posts) {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        String baseUrl = siteConfig.getUrl();
        
        // ページ
        for (Page page : pages) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append("/").append(page.slug()).append(".html</loc>\n");
            sitemap.append("    <lastmod>").append(page.lastModified().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
            sitemap.append("  </url>\n");
        }
        
        // 投稿
        for (Post post : posts) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append("/").append(post.slug()).append(".html</loc>\n");
            sitemap.append("    <lastmod>").append(post.lastModified().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
            sitemap.append("  </url>\n");
        }
        
        sitemap.append("</urlset>");
        return sitemap.toString();
    }
    
    public String generateRssFeed(List<Post> posts) {
        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\">\n");
        rss.append("  <channel>\n");
        rss.append("    <title>").append(escapeXml(siteConfig.getTitle())).append("</title>\n");
        rss.append("    <link>").append(siteConfig.getUrl()).append("</link>\n");
        rss.append("    <description>").append(escapeXml(siteConfig.getDescription())).append("</description>\n");
        rss.append("    <language>").append(siteConfig.getLanguage()).append("</language>\n");
        
        // 最新10件の投稿
        posts.stream()
            .limit(10)
            .forEach(post -> {
                rss.append("    <item>\n");
                rss.append("      <title>").append(escapeXml(post.title())).append("</title>\n");
                rss.append("      <link>").append(siteConfig.getUrl()).append("/").append(post.slug()).append(".html</link>\n");
                rss.append("      <description>").append(escapeXml(post.description())).append("</description>\n");
                rss.append("      <pubDate>").append(post.publishedAt().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>\n");
                rss.append("    </item>\n");
            });
        
        rss.append("  </channel>\n");
        rss.append("</rss>");
        return rss.toString();
    }
    
    private Template getTemplate(String templateName) {
        return cacheManager.getTemplate(templateName)
            .orElseThrow(() -> new HtmlGenerationException("テンプレートが見つかりません: " + templateName));
    }
    
    private Map<String, Object> createSiteContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("title", siteConfig.getTitle());
        context.put("description", siteConfig.getDescription());
        context.put("url", siteConfig.getUrl());
        context.put("language", siteConfig.getLanguage());
        context.put("author", siteConfig.getAuthor());
        return context;
    }
    
    private Map<String, Object> createPageContext(Page page) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", page.title());
        context.put("slug", page.slug());
        context.put("description", page.description());
        context.put("content", page.renderedContent());
        context.put("lastModified", page.lastModified());
        context.putAll(page.frontMatter());
        return context;
    }
    
    private Map<String, Object> createPostContext(Post post) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", post.title());
        context.put("slug", post.slug());
        context.put("description", post.description());
        context.put("content", post.renderedContent());
        context.put("publishedAt", post.publishedAt());
        context.put("lastModified", post.lastModified());
        context.put("categories", post.categories());
        context.put("tags", post.tags());
        context.put("published", post.isPublished());
        context.putAll(post.frontMatter());
        return context;
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}