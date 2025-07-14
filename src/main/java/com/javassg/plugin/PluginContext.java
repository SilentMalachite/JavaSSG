package com.javassg.plugin;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.SiteConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * プラグインの実行コンテキスト
 */
public class PluginContext {
    
    private final SiteConfig siteConfig;
    private final List<Page> pages;
    private final List<Post> posts;
    private final Path outputDirectory;
    private final Map<String, Object> metadata;
    private final PluginPhase phase;
    
    public PluginContext(SiteConfig siteConfig, List<Page> pages, List<Post> posts, 
                        Path outputDirectory, Map<String, Object> metadata, PluginPhase phase) {
        this.siteConfig = siteConfig;
        this.pages = pages;
        this.posts = posts;
        this.outputDirectory = outputDirectory;
        this.metadata = metadata;
        this.phase = phase;
    }
    
    public SiteConfig getSiteConfig() {
        return siteConfig;
    }
    
    public List<Page> getPages() {
        return pages;
    }
    
    public List<Post> getPosts() {
        return posts;
    }
    
    public Path getOutputDirectory() {
        return outputDirectory;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public PluginPhase getPhase() {
        return phase;
    }
    
    /**
     * プラグイン実行のフェーズ
     */
    public enum PluginPhase {
        PRE_BUILD,      // ビルド前
        POST_CONTENT,   // コンテンツ処理後
        POST_TEMPLATE,  // テンプレート処理後
        POST_BUILD      // ビルド後
    }
}