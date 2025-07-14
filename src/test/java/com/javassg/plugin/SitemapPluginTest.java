package com.javassg.plugin;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.SiteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapPluginTest {

    private SitemapPlugin plugin;
    private SiteConfig siteConfig;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        plugin = new SitemapPlugin();
        siteConfig = SiteConfig.defaultConfig();
    }

    @Test
    void shouldInitializeCorrectly() {
        plugin.initialize(siteConfig, null);
        
        assertThat(plugin.getName()).isEqualTo("sitemap");
        assertThat(plugin.getVersion()).isEqualTo("1.0.0");
        assertThat(plugin.getDescription()).contains("サイトマップ");
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldGenerateSitemapInPostBuildPhase() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        // テストデータの作成
        Page page = Page.builder()
            .filename("about.md")
            .slug("about")
            .rawContent("# About\nThis is about page")
            .renderedContent("<h1>About</h1><p>This is about page</p>")
            .lastModified(LocalDateTime.now())
            .build();
            
        Post post = new Post(
            "test-post.md",
            "test-post",
            Map.of("title", "Test Post"),
            "# Test Post\nTest content",
            "<h1>Test Post</h1><p>Test content</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of("tech"),
            List.of("java")
        );
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(page),
            List.of(post),
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // サイトマップファイルが生成されることを確認
        Path sitemapPath = outputDir.resolve("sitemap.xml");
        assertThat(Files.exists(sitemapPath)).isTrue();
        
        String sitemapContent = Files.readString(sitemapPath);
        assertThat(sitemapContent).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(sitemapContent).contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        assertThat(sitemapContent).contains("/about.html");
        assertThat(sitemapContent).contains("/test-post.html");
        assertThat(sitemapContent).contains("</urlset>");
    }

    @Test
    void shouldSkipExecutionInOtherPhases() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(),
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.PRE_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // PRE_BUILDフェーズではサイトマップが生成されない
        Path sitemapPath = outputDir.resolve("sitemap.xml");
        assertThat(Files.exists(sitemapPath)).isFalse();
    }

    @Test
    void shouldHaveCorrectExecutionOrder() {
        assertThat(plugin.getExecutionOrder()).isEqualTo(900);
    }

    @Test
    void shouldCleanupWithoutErrors() {
        plugin.initialize(siteConfig, null);
        plugin.cleanup();
        
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldHandleEmptyContentLists() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(), // 空のページリスト
            List.of(), // 空の投稿リスト
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // 空のコンテンツでもサイトマップが生成される
        Path sitemapPath = outputDir.resolve("sitemap.xml");
        assertThat(Files.exists(sitemapPath)).isTrue();
        
        String sitemapContent = Files.readString(sitemapPath);
        assertThat(sitemapContent).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(sitemapContent).contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        assertThat(sitemapContent).contains("</urlset>");
    }
}