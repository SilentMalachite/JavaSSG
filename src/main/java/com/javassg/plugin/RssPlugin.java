package com.javassg.plugin;

import com.javassg.model.Post;
import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * RSSフィード生成プラグイン
 */
public class RssPlugin implements Plugin {
    
    private static final Logger logger = LoggerFactory.getLogger(RssPlugin.class);
    
    private boolean enabled = true;
    private SiteConfig siteConfig;
    
    @Override
    public String getName() {
        return "rss";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "ブログのRSSフィードを生成します";
    }
    
    @Override
    public void initialize(SiteConfig config, Map<String, Object> settings) {
        this.siteConfig = config;
        logger.debug("RSSプラグインを初期化しました");
    }
    
    @Override
    public void execute(PluginContext context) {
        if (context.getPhase() != PluginContext.PluginPhase.POST_BUILD) {
            return;
        }
        
        try {
            generateRssFeed(context);
            logger.info("RSSフィードを生成しました");
        } catch (Exception e) {
            logger.error("RSSフィード生成エラー", e);
        }
    }
    
    private void generateRssFeed(PluginContext context) throws IOException {
        if (context.getPosts().isEmpty()) {
            logger.debug("投稿がないため、RSSフィードはスキップされました");
            return;
        }
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escapeXml(siteConfig.getTitle())).append("</title>\n");
        xml.append("    <link>").append(siteConfig.getUrl()).append("</link>\n");
        xml.append("    <description>").append(escapeXml(siteConfig.getDescription())).append("</description>\n");
        xml.append("    <language>").append(siteConfig.getLanguage()).append("</language>\n");
        xml.append("    <atom:link href=\"").append(siteConfig.getUrl()).append("/rss.xml\" rel=\"self\" type=\"application/rss+xml\" />\n");
        
        final String baseUrl = siteConfig.getUrl().endsWith("/") ? 
            siteConfig.getUrl().substring(0, siteConfig.getUrl().length() - 1) : 
            siteConfig.getUrl();
        
        // 最新の投稿を最大20件まで出力
        context.getPosts().stream()
            .limit(20)
            .forEach(post -> {
                xml.append("    <item>\n");
                xml.append("      <title>").append(escapeXml(post.title())).append("</title>\n");
                xml.append("      <link>").append(baseUrl).append("/").append(post.slug()).append(".html</link>\n");
                xml.append("      <description>").append(escapeXml(post.getExcerpt(200))).append("</description>\n");
                xml.append("      <pubDate>").append(formatRssDate(post.publishedAt())).append("</pubDate>\n");
                xml.append("      <guid>").append(baseUrl).append("/").append(post.slug()).append(".html</guid>\n");
                
                // カテゴリの追加
                if (post.categories() != null && !post.categories().isEmpty()) {
                    for (String category : post.categories()) {
                        xml.append("      <category>").append(escapeXml(category)).append("</category>\n");
                    }
                }
                
                xml.append("    </item>\n");
            });
        
        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        
        Path rssPath = context.getOutputDirectory().resolve("rss.xml");
        Files.writeString(rssPath, xml.toString());
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private String formatRssDate(java.time.LocalDateTime dateTime) {
        // LocalDateTimeをZonedDateTimeに変換してRFC822形式にフォーマット
        return dateTime.atZone(java.time.ZoneId.systemDefault())
                       .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z"));
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