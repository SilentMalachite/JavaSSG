package com.javassg.plugin;

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

class RssPluginTest {

    private RssPlugin plugin;
    private SiteConfig siteConfig;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        plugin = new RssPlugin();
        siteConfig = SiteConfig.defaultConfig();
    }

    @Test
    void shouldInitializeCorrectly() {
        plugin.initialize(siteConfig, null);
        
        assertThat(plugin.getName()).isEqualTo("rss");
        assertThat(plugin.getVersion()).isEqualTo("1.0.0");
        assertThat(plugin.getDescription()).contains("RSSフィード");
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldGenerateRssFeedInPostBuildPhase() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        // テストデータの作成
        Post post1 = new Post(
            "post1.md",
            "post-1",
            Map.of("title", "Test Post 1"),
            "# Test Post 1\nFirst test content",
            "<h1>Test Post 1</h1><p>First test content</p>",
            LocalDateTime.now(),
            LocalDateTime.now().minusDays(1),
            List.of("tech", "java"),
            List.of("programming")
        );
        
        Post post2 = new Post(
            "post2.md",
            "post-2",
            Map.of("title", "Test Post 2"),
            "# Test Post 2\nSecond test content",
            "<h1>Test Post 2</h1><p>Second test content</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of("web"),
            List.of("css")
        );
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(post1, post2),
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // RSSファイルが生成されることを確認
        Path rssPath = outputDir.resolve("rss.xml");
        assertThat(Files.exists(rssPath)).isTrue();
        
        String rssContent = Files.readString(rssPath);
        assertThat(rssContent).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(rssContent).contains("<rss version=\"2.0\"");
        assertThat(rssContent).contains("<channel>");
        assertThat(rssContent).contains("<title>" + siteConfig.getTitle() + "</title>");
        assertThat(rssContent).contains("<description>" + siteConfig.getDescription() + "</description>");
        assertThat(rssContent).contains("<item>");
        assertThat(rssContent).contains("Test Post 1");
        assertThat(rssContent).contains("Test Post 2");
        assertThat(rssContent).contains("<category>tech</category>");
        assertThat(rssContent).contains("<category>java</category>");
        assertThat(rssContent).contains("</channel>");
        assertThat(rssContent).contains("</rss>");
    }

    @Test
    void shouldSkipExecutionWithNoPosts() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(), // 空の投稿リスト
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // 投稿がない場合はRSSファイルが生成されない
        Path rssPath = outputDir.resolve("rss.xml");
        assertThat(Files.exists(rssPath)).isFalse();
    }

    @Test
    void shouldSkipExecutionInOtherPhases() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        Post post = new Post(
            "test.md",
            "test",
            Map.of("title", "Test Post"),
            "# Test\nContent",
            "<h1>Test</h1><p>Content</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of(),
            List.of()
        );
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(post),
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.PRE_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        // PRE_BUILDフェーズではRSSが生成されない
        Path rssPath = outputDir.resolve("rss.xml");
        assertThat(Files.exists(rssPath)).isFalse();
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
    void shouldLimitPostsTo20Items() throws IOException {
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        // 25個の投稿を作成
        List<Post> posts = List.of(
            createTestPost("post-1", 1),
            createTestPost("post-2", 2),
            createTestPost("post-3", 3),
            createTestPost("post-4", 4),
            createTestPost("post-5", 5),
            createTestPost("post-6", 6),
            createTestPost("post-7", 7),
            createTestPost("post-8", 8),
            createTestPost("post-9", 9),
            createTestPost("post-10", 10),
            createTestPost("post-11", 11),
            createTestPost("post-12", 12),
            createTestPost("post-13", 13),
            createTestPost("post-14", 14),
            createTestPost("post-15", 15),
            createTestPost("post-16", 16),
            createTestPost("post-17", 17),
            createTestPost("post-18", 18),
            createTestPost("post-19", 19),
            createTestPost("post-20", 20),
            createTestPost("post-21", 21),
            createTestPost("post-22", 22),
            createTestPost("post-23", 23),
            createTestPost("post-24", 24),
            createTestPost("post-25", 25)
        );
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            posts,
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        plugin.execute(context);
        
        Path rssPath = outputDir.resolve("rss.xml");
        String rssContent = Files.readString(rssPath);
        
        // itemタグの数をカウント（最大20個まで）
        long itemCount = rssContent.lines()
            .filter(line -> line.trim().equals("<item>"))
            .count();
        
        assertThat(itemCount).isEqualTo(20);
    }
    
    private Post createTestPost(String slug, int number) {
        return new Post(
            slug + ".md",
            slug,
            Map.of("title", "Test Post " + number),
            "# Test Post " + number + "\nContent " + number,
            "<h1>Test Post " + number + "</h1><p>Content " + number + "</p>",
            LocalDateTime.now(),
            LocalDateTime.now().minusDays(number),
            List.of("test"),
            List.of("tag" + number)
        );
    }
}