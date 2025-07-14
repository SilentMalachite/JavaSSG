package com.javassg.model;

import java.util.List;
import java.util.Map;

public record SiteConfig(
    SiteInfo site,
    BuildConfig build,
    ServerConfig server,
    BlogConfig blog,
    SecurityLimits limits,
    List<PluginConfig> plugins
) {
    public static SiteConfig defaultConfig() {
        return new SiteConfig(
            new SiteInfo(
                "My Site",
                "A static site generated with JavaSSG",
                "https://example.com",
                "en-US",
                new Author("Site Author", "author@example.com")
            ),
            new BuildConfig(
                "content",
                "_site",
                "static",
                "templates"
            ),
            new ServerConfig(8080, true),
            new BlogConfig(10, true, true, true),
            SecurityLimits.defaultLimits(),
            List.of()
        );
    }
    
    // Convenience methods for external access
    public String getTitle() {
        return site.title();
    }
    
    public String getDescription() {
        return site.description();
    }
    
    public String getUrl() {
        return site.url();
    }
    
    public String getLanguage() {
        return site.language();
    }
    
    public Object getAuthor() {
        return site.author();
    }
    
    public String getContentDirectory() {
        return build.contentDirectory();
    }
    
    public String getOutputDirectory() {
        return build.outputDirectory();
    }
    
    public String getStaticDirectory() {
        return build.staticDirectory();
    }
    
    public String getTemplatesDirectory() {
        return build.templatesDirectory();
    }
    
    public int getServerPort() {
        return server != null ? server.port() : 8080;
    }
}