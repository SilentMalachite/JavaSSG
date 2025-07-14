package com.javassg.cache;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheManagerTest {

    private CacheManager cacheManager;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheManager();
    }

    @Test
    void shouldCoordinateAllCaches() {
        var page = Page.builder()
                .filename("test.md")
                .slug("test-page")
                .rawContent("# Test Page\nTest content")
                .renderedContent("<h1>Test Page</h1><p>Test content</p>")
                .lastModified(LocalDateTime.now())
                .build();

        var template = new Template(
                "page",
                "<html><body>{{ content }}</body></html>"
        );

        String renderedHtml = "<html><body><p>Test content</p></body></html>";

        cacheManager.cachePage("test-page", page);
        cacheManager.cacheTemplate("page", template);
        cacheManager.cacheRendered("page:test-page", renderedHtml, LocalDateTime.now());

        assertThat(cacheManager.getPage("test-page")).isPresent();
        assertThat(cacheManager.getTemplate("page")).isPresent();
        assertThat(cacheManager.getRendered("page:test-page")).isPresent();
    }

    @Test
    void shouldInvalidateRelatedCaches() {
        var page = Page.builder()
                .filename("test.md")
                .slug("test-page")
                .rawContent("# Test Page\nTest content")
                .renderedContent("<h1>Test Page</h1><p>Test content</p>")
                .lastModified(LocalDateTime.now())
                .build();

        String renderedHtml = "<html><body><p>Test content</p></body></html>";

        cacheManager.cachePage("test-page", page);
        cacheManager.cacheRendered("page:test-page", renderedHtml, LocalDateTime.now());

        cacheManager.invalidateContentAndRelated("test-page");

        assertThat(cacheManager.getPage("test-page")).isEmpty();
        assertThat(cacheManager.getRendered("page:test-page")).isEmpty();
    }

    @Test
    void shouldInvalidateTemplateAndDependents() {
        var baseTemplate = new Template(
                "base",
                "<!DOCTYPE html><html><body>{% block content %}{% endblock %}</body></html>"
        );

        var pageTemplate = new Template(
                "page",
                "{% extends 'base' %}{% block content %}{{ content }}{% endblock %}"
        );

        cacheManager.cacheTemplate("base", baseTemplate);
        cacheManager.cacheTemplate("page", pageTemplate);
        cacheManager.addTemplateDependency("page", "base");

        cacheManager.cacheRendered("page:test1", "<html>Test 1</html>", LocalDateTime.now());
        cacheManager.cacheRendered("page:test2", "<html>Test 2</html>", LocalDateTime.now());

        cacheManager.invalidateTemplateAndDependents("base");

        assertThat(cacheManager.getTemplate("base")).isEmpty();
        assertThat(cacheManager.getRendered("page:test1")).isEmpty();
        assertThat(cacheManager.getRendered("page:test2")).isEmpty();
    }

    @Test
    void shouldProvideGlobalCacheStatistics() {
        var page = Page.builder()
                .filename("test.md")
                .slug("test-page")
                .rawContent("# Test Page\nTest content")
                .renderedContent("<h1>Test Page</h1><p>Test content</p>")
                .lastModified(LocalDateTime.now())
                .build();

        cacheManager.cachePage("test-page", page);
        cacheManager.cacheRendered("test:1", "<html>1</html>", LocalDateTime.now());
        cacheManager.cacheRendered("test:2", "<html>2</html>", LocalDateTime.now());

        var stats = cacheManager.getStatistics();
        assertThat(stats.contentCacheSize()).isEqualTo(1);
        assertThat(stats.renderCacheSize()).isEqualTo(2);
        assertThat(stats.templateCacheSize()).isEqualTo(0);
    }

    @Test
    void shouldClearAllCaches() {
        var page = Page.builder()
                .filename("test.md")
                .slug("test-page")
                .rawContent("# Test Page\nTest content")
                .renderedContent("<h1>Test Page</h1><p>Test content</p>")
                .lastModified(LocalDateTime.now())
                .build();

        var template = new Template(
                "test",
                "<div>{{ content }}</div>"
        );

        cacheManager.cachePage("test-page", page);
        cacheManager.cacheTemplate("test", template);
        cacheManager.cacheRendered("test:rendered", "<html>Test</html>", LocalDateTime.now());

        cacheManager.clearAll();

        var stats = cacheManager.getStatistics();
        assertThat(stats.contentCacheSize()).isEqualTo(0);
        assertThat(stats.renderCacheSize()).isEqualTo(0);
        assertThat(stats.templateCacheSize()).isEqualTo(0);
    }

    @Test
    void shouldDetectStaleContent() {
        var page = Page.builder()
                .filename("test.md")
                .slug("test-page")
                .rawContent("# Test Page\nTest content")
                .renderedContent("<h1>Test Page</h1><p>Test content</p>")
                .lastModified(LocalDateTime.now().minusHours(1))
                .build();

        cacheManager.cachePage("test-page", page);

        boolean isStale = cacheManager.isContentStale("test-page", LocalDateTime.now());
        assertThat(isStale).isTrue();
    }

    @Test
    void shouldWarmUpCache() {
        Path contentDir = tempDir.resolve("content");
        Path templatesDir = tempDir.resolve("templates");

        cacheManager.warmUp(contentDir, templatesDir);

        var stats = cacheManager.getStatistics();
        assertThat(stats).isNotNull();
    }
}