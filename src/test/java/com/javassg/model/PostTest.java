package com.javassg.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Post Tests")
class PostTest {

    @Test
    @DisplayName("基本的なブログ記事が作成できること")
    void shouldCreateBasicPost() {
        var frontMatter = Map.<String, Object>of(
            "title", "テスト記事",
            "date", "2024-01-01T10:00:00",
            "categories", List.of("技術", "Java"),
            "tags", List.of("Java21", "SSG")
        );
        
        var post = new Post(
            "test-post.md",
            "test-post",
            frontMatter,
            "# テスト記事\n\nこれはテスト記事です。",
            "<h1>テスト記事</h1>\n<p>これはテスト記事です。</p>",
            LocalDateTime.now(),
            LocalDateTime.of(2024, 1, 1, 10, 0),
            List.of("技術", "Java"),
            List.of("Java21", "SSG")
        );
        
        assertThat(post.title()).isEqualTo("テスト記事");
        assertThat(post.publishedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(post.categories()).containsExactly("技術", "Java");
        assertThat(post.tags()).containsExactly("Java21", "SSG");
    }

    @Test
    @DisplayName("記事の日付が正しくパースされること")
    void shouldParseDateCorrectly() {
        var frontMatter = Map.<String, Object>of(
            "title", "日付テスト",
            "date", "2024-12-25T15:30:45"
        );
        
        var post = Post.fromPage(new Page(
            "date-test.md",
            "date-test",
            frontMatter,
            "# 日付テスト",
            "<h1>日付テスト</h1>",
            LocalDateTime.now()
        ));
        
        assertThat(post.publishedAt()).isEqualTo(LocalDateTime.of(2024, 12, 25, 15, 30, 45));
    }

    @Test
    @DisplayName("記事の公開状態が正しく判定されること")
    void shouldDeterminePublishStatus() {
        var publishedPost = Post.fromPage(new Page(
            "published.md",
            "published",
            Map.of("draft", false, "date", "2023-01-01T10:00:00"),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        var draftPost = Post.fromPage(new Page(
            "draft.md",
            "draft",
            Map.of("draft", true, "date", "2023-01-01T10:00:00"),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        var futurePost = Post.fromPage(new Page(
            "future.md",
            "future",
            Map.of("date", LocalDateTime.now().plusDays(1).toString()),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        assertThat(publishedPost.isPublished()).isTrue();
        assertThat(draftPost.isPublished()).isFalse();
        assertThat(futurePost.isPublished()).isFalse();
    }

    @Test
    @DisplayName("記事の抜粋が正しく生成されること")
    void shouldGenerateExcerptCorrectly() {
        var longContent = "これは非常に長いコンテンツです。" + "文章を追加します。".repeat(20);
        
        var post = new Post(
            "long-post.md",
            "long-post",
            Map.of("title", "長い記事"),
            longContent,
            "<p>" + longContent + "</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of(),
            List.of()
        );
        
        String excerpt = post.getExcerpt(100);
        
        assertThat(excerpt).hasSizeLessThanOrEqualTo(100);
        assertThat(excerpt).endsWith("...");
        assertThat(excerpt).startsWith("これは非常に長い");
    }

    @Test
    @DisplayName("カテゴリが正しく解析されること")
    void shouldParseCategories() {
        // リスト形式
        var listPost = Post.fromPage(new Page(
            "list-categories.md",
            "list-categories",
            Map.of("categories", List.of("技術", "プログラミング", "Java")),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        // 文字列形式（カンマ区切り）
        var stringPost = Post.fromPage(new Page(
            "string-categories.md",
            "string-categories",
            Map.of("categories", "技術, プログラミング, Java"),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        assertThat(listPost.categories()).containsExactly("技術", "プログラミング", "Java");
        assertThat(stringPost.categories()).containsExactly("技術", "プログラミング", "Java");
    }

    @Test
    @DisplayName("タグが正しく解析されること")
    void shouldParseTags() {
        // リスト形式
        var listPost = Post.fromPage(new Page(
            "list-tags.md",
            "list-tags",
            Map.of("tags", List.of("Java21", "SSG", "Markdown")),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        // 文字列形式（カンマ区切り）
        var stringPost = Post.fromPage(new Page(
            "string-tags.md",
            "string-tags",
            Map.of("tags", "Java21, SSG, Markdown"),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        assertThat(listPost.tags()).containsExactly("Java21", "SSG", "Markdown");
        assertThat(stringPost.tags()).containsExactly("Java21", "SSG", "Markdown");
    }

    @Test
    @DisplayName("タイトルがない場合はファイル名から生成されること")
    void shouldGenerateTitleFromFilename() {
        var post = Post.fromPage(new Page(
            "my-awesome-post.md",
            "my-awesome-post",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        assertThat(post.title()).isEqualTo("My Awesome Post");
    }

    @Test
    @DisplayName("無効な日付の場合は現在時刻が使用されること")
    void shouldUseCurrentTimeForInvalidDate() {
        var before = LocalDateTime.now();
        
        var post = Post.fromPage(new Page(
            "invalid-date.md",
            "invalid-date",
            Map.of("date", "invalid-date-format"),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now()
        ));
        
        var after = LocalDateTime.now();
        
        assertThat(post.publishedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("必須フィールドがnullの場合はエラーになること")
    void shouldThrowExceptionForNullFields() {
        assertThatThrownBy(() -> new Post(
            null, // filename
            "test",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of(),
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ファイル名は空にできません");

        assertThatThrownBy(() -> new Post(
            "test.md",
            "test",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now(),
            null, // publishedAt
            List.of(),
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("公開日時は null にできません");

        assertThatThrownBy(() -> new Post(
            "test.md",
            "test",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            null, // categories
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("カテゴリーは null にできません");

        assertThatThrownBy(() -> new Post(
            "test.md",
            "test",
            Map.of(),
            "コンテンツ",
            "<p>コンテンツ</p>",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of(),
            null // tags
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("タグは null にできません");
    }
}