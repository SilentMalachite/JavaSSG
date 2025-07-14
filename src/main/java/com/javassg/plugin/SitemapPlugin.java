package com.javassg.plugin;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * サイトマップ生成プラグイン
 */
public class SitemapPlugin implements Plugin {
    
    private static final Logger logger = LoggerFactory.getLogger(SitemapPlugin.class);
    
    private boolean enabled = true;
    private SiteConfig siteConfig;
    
    @Override
    public String getName() {
        return "sitemap";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "サイトマップ（sitemap.xml）を生成します";
    }
    
    @Override
    public void initialize(SiteConfig config, Map<String, Object> settings) {
        this.siteConfig = config;
        logger.debug("サイトマッププラグインを初期化しました");
    }
    
    @Override
    public void execute(PluginContext context) {
        if (context.getPhase() != PluginContext.PluginPhase.POST_BUILD) {
            return;
        }
        
        try {
            generateSitemap(context);
            logger.info("サイトマップを生成しました");
        } catch (Exception e) {
            logger.error("サイトマップ生成エラー", e);
        }
    }
    
    private void generateSitemap(PluginContext context) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        String baseUrl = siteConfig.getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // ページのURL
        for (Page page : context.getPages()) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/").append(page.slug()).append(".html</loc>\n");
            xml.append("    <lastmod>").append(formatDate(page.lastModified())).append("</lastmod>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }
        
        // 投稿のURL
        for (Post post : context.getPosts()) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/").append(post.slug()).append(".html</loc>\n");
            xml.append("    <lastmod>").append(formatDate(post.lastModified())).append("</lastmod>\n");
            xml.append("    <changefreq>monthly</changefreq>\n");
            xml.append("    <priority>0.6</priority>\n");
            xml.append("  </url>\n");
        }
        
        xml.append("</urlset>\n");
        
        Path sitemapPath = context.getOutputDirectory().resolve("sitemap.xml");
        Files.writeString(sitemapPath, xml.toString());
    }
    
    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    @Override
    public void cleanup() {
        // 特にクリーンアップは不要
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public int getExecutionOrder() {
        return 900; // 後半に実行
    }
}