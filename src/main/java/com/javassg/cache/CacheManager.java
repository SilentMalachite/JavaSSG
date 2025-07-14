package com.javassg.cache;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

public class CacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    private final ContentCache contentCache;
    private final TemplateCache templateCache;
    private final RenderCache renderCache;
    
    public CacheManager() {
        this.contentCache = new ContentCache();
        this.templateCache = new TemplateCache();
        this.renderCache = new RenderCache();
    }
    
    public void cachePage(String key, Page page) {
        contentCache.putPage(key, page);
        logger.debug("ページをキャッシュしました: {}", key);
    }
    
    public Optional<Page> getPage(String key) {
        return contentCache.getPage(key);
    }
    
    public void cachePost(String key, Post post) {
        contentCache.putPost(key, post);
        logger.debug("投稿をキャッシュしました: {}", key);
    }
    
    public Optional<Post> getPost(String key) {
        return contentCache.getPost(key);
    }
    
    public void cacheTemplate(String name, Template template) {
        templateCache.putTemplate(name, template);
        
        for (String dependency : template.getDependencies()) {
            templateCache.addDependency(name, dependency);
        }
        
        logger.debug("テンプレートをキャッシュしました: {}", name);
    }
    
    public Optional<Template> getTemplate(String name) {
        return templateCache.getTemplate(name);
    }
    
    public void addTemplateDependency(String template, String dependency) {
        templateCache.addDependency(template, dependency);
    }
    
    public void cacheRendered(String key, String content, LocalDateTime timestamp) {
        renderCache.putRendered(key, content, timestamp);
        logger.debug("レンダリング結果をキャッシュしました: {}", key);
    }
    
    public Optional<String> getRendered(String key) {
        return renderCache.getRendered(key);
    }
    
    public boolean isContentValid(String key, LocalDateTime lastModified) {
        return contentCache.isValid(key, lastModified);
    }
    
    public boolean isTemplateValid(String name, LocalDateTime lastModified) {
        return templateCache.isValid(name, lastModified);
    }
    
    public boolean isRenderValid(String key, LocalDateTime lastModified) {
        return renderCache.isValid(key, lastModified);
    }
    
    public void invalidateContentAndRelated(String key) {
        contentCache.remove(key);
        renderCache.invalidateByPattern("*" + key + "*");
        logger.debug("コンテンツと関連するキャッシュを無効化しました: {}", key);
    }
    
    public void invalidateTemplateAndDependents(String templateName) {
        templateCache.invalidateDependents(templateName);
        templateCache.remove(templateName);
        renderCache.invalidateByPattern("*");
        logger.debug("テンプレートと依存関係を無効化しました: {}", templateName);
    }
    
    public boolean isContentStale(String key, LocalDateTime currentTime) {
        return !contentCache.isValid(key, currentTime);
    }
    
    public void clearAll() {
        contentCache.clear();
        templateCache.clear();
        renderCache.clear();
        logger.info("全てのキャッシュをクリアしました");
    }
    
    public int getTemplateCount() {
        return templateCache.size();
    }
    
    public GlobalCacheStatistics getStatistics() {
        var renderStats = renderCache.getStatistics();
        return new GlobalCacheStatistics(
            contentCache.size(),
            templateCache.size(),
            renderStats.totalEntries(),
            renderStats.hitCount(),
            renderStats.missCount()
        );
    }
    
    public void warmUp(Path contentDir, Path templatesDir) {
        logger.info("キャッシュのウォームアップを開始します");
        
        try {
            if (Files.exists(contentDir)) {
                try (Stream<Path> paths = Files.walk(contentDir)) {
                    long contentCount = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        .count();
                    logger.debug("{}個のコンテンツファイルを発見しました", contentCount);
                }
            }
            
            if (Files.exists(templatesDir)) {
                try (Stream<Path> paths = Files.walk(templatesDir)) {
                    long templateCount = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".html"))
                        .count();
                    logger.debug("{}個のテンプレートファイルを発見しました", templateCount);
                }
            }
            
            logger.info("キャッシュのウォームアップが完了しました");
        } catch (Exception e) {
            logger.warn("キャッシュのウォームアップ中にエラーが発生しました", e);
        }
    }
    
    public record GlobalCacheStatistics(
        int contentCacheSize,
        int templateCacheSize,
        int renderCacheSize,
        long renderHits,
        long renderMisses
    ) {
        public double getRenderHitRatio() {
            long total = renderHits + renderMisses;
            return total > 0 ? (double) renderHits / total : 0.0;
        }
    }
}