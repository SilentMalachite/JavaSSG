package com.javassg.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Page Tests")
class PageTest {

    @Test
    @DisplayName("基本的なページが作成できること")
    void shouldCreateBasicPage() {
        var frontMatter = Map.<String, Object>of(
            "title", "テストページ",
            "description", "テストページの説明"
        );
        
        var page = new Page(
            "test.md",
            "test",
            frontMatter,
            "# テストページ\n\nこれはテストページです。",
            "<h1>テストページ</h1>\n<p>これはテストページです。</p>",
            LocalDateTime.now()
        );
        
        assertThat(page.filename()).isEqualTo("test.md");
        assertThat(page.slug()).isEqualTo("test");
        assertThat(page.title()).isEqualTo("テストページ");
        assertThat(page.description()).isEqualTo("テストページの説明");
        assertThat(page.rawContent()).contains("# テストページ");
        assertThat(page.renderedContent()).contains("<h1>テストページ</h1>");
        assertThat(page.lastModified()).isNotNull();
    }

    @Test
    @DisplayName("フロントマターからタイトルと説明が取得できること")
    void shouldExtractTitleAndDescriptionFromFrontMatter() {
        var frontMatter = Map.<String, Object>of(
            "title", "カスタムタイトル",
            "description", "カスタム説明",
            "author", "テスト太郎"
        );
        
        var page = new Page(
            "custom.md",
            "custom",
            frontMatter,
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        );
        
        assertThat(page.title()).isEqualTo("カスタムタイトル");
        assertThat(page.description()).isEqualTo("カスタム説明");
        assertThat(page.getFrontMatterValue("author")).isEqualTo("テスト太郎");
    }

    @Test
    @DisplayName("タイトルが設定されていない場合はファイル名から生成されること")
    void shouldGenerateTitleFromFilenameWhenNotSet() {
        var frontMatter = Map.<String, Object>of();
        
        var page = new Page(
            "about-us.md",
            "about-us",
            frontMatter,
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        );
        
        assertThat(page.title()).isEqualTo("About Us");
    }

    @Test
    @DisplayName("無効なファイル名でエラーが発生すること")
    void shouldThrowExceptionForInvalidFilename() {
        assertThatThrownBy(() -> new Page(
            "",
            "test",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ファイル名は空にできません");
    }

    @Test
    @DisplayName("スラグが正しく生成されること")
    void shouldGenerateSlugCorrectly() {
        var page = Page.builder()
            .filename("テスト ページ.md")
            .rawContent("コンテンツ")
            .renderedContent("<p>コンテンツ</p>")
            .lastModified(LocalDateTime.now())
            .build();
        
        assertThat(page.slug()).isEqualTo("tesuto-peeji");
    }

    @Test
    @DisplayName("ページが公開状態かどうか判定できること")
    void shouldDetermineIfPageIsPublished() {
        var publishedPage = new Page(
            "published.md",
            "published",
            Map.of("draft", false),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        );
        
        var draftPage = new Page(
            "draft.md",
            "draft",
            Map.of("draft", true),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        );
        
        assertThat(publishedPage.isPublished()).isTrue();
        assertThat(draftPage.isPublished()).isFalse();
    }
}