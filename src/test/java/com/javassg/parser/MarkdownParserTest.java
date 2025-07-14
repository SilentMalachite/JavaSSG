package com.javassg.parser;

import com.javassg.model.Page;
import com.javassg.model.Post;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

@DisplayName("MarkdownParser Tests")
class MarkdownParserTest {

    private MarkdownParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownParser();
    }

    @Test
    @DisplayName("基本的なMarkdownファイルが解析できること")
    void shouldParseBasicMarkdownFile() {
        var content = """
            ---
            title: テストページ
            description: テストページの説明
            date: 2024-01-01T10:00:00
            ---
            
            # テストページ
            
            これはテストページです。
            
            ## セクション1
            
            - リスト1
            - リスト2
            """;

        var page = parser.parseContent("test.md", content);

        assertThat(page.title()).isEqualTo("テストページ");
        assertThat(page.description()).isEqualTo("テストページの説明");
        assertThat(page.rawContent()).contains("# テストページ");
        assertThat(page.renderedContent()).contains("<h1>テストページ</h1>");
        assertThat(page.renderedContent()).contains("<ul>");
        assertThat(page.renderedContent()).contains("<li>リスト1</li>");
    }

    @Test
    @DisplayName("フロントマターなしのMarkdownファイルが解析できること")
    void shouldParseMarkdownWithoutFrontMatter() {
        var content = """
            # シンプルページ
            
            これはフロントマターなしのページです。
            """;

        var page = parser.parseContent("simple.md", content);

        assertThat(page.title()).isEqualTo("Simple");
        assertThat(page.rawContent()).contains("# シンプルページ");
        assertThat(page.renderedContent()).contains("<h1>シンプルページ</h1>");
        assertThat(page.frontMatter()).isEmpty();
    }

    @Test
    @DisplayName("複雑なフロントマターが解析できること")
    void shouldParseComplexFrontMatter() {
        var content = """
            ---
            title: 複雑なページ
            author: テスト太郎
            tags:
              - Java
              - SSG
              - Markdown
            categories:
              - 技術
              - プログラミング
            draft: false
            weight: 10
            ---
            
            # 複雑なページ
            
            複雑なフロントマターを持つページです。
            """;

        var page = parser.parseContent("complex.md", content);

        assertThat(page.title()).isEqualTo("複雑なページ");
        assertThat(page.getFrontMatterValue("author")).isEqualTo("テスト太郎");
        assertThat(page.getFrontMatterValue("tags")).asList().contains("Java", "SSG", "Markdown");
        assertThat(page.getFrontMatterValue("categories")).asList().contains("技術", "プログラミング");
        assertThat(page.getFrontMatterValue("draft")).isEqualTo(false);
        assertThat(page.getFrontMatterValue("weight")).isEqualTo(10);
    }

    @Test
    @DisplayName("Markdownの特殊記法が正しく処理されること")
    void shouldProcessMarkdownSyntax() {
        var content = """
            ---
            title: 記法テスト
            ---
            
            # 見出し1
            ## 見出し2
            ### 見出し3
            
            **太字** と *斜体* のテスト。
            
            `インラインコード` と以下のコードブロック：
            
            ```java
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            ```
            
            [リンクテスト](https://example.com)
            
            > 引用文のテスト
            > 複数行の引用
            
            1. 番号付きリスト1
            2. 番号付きリスト2
            
            - 箇条書き1
            - 箇条書き2
            """;

        var page = parser.parseContent("syntax.md", content);

        var html = page.renderedContent();
        assertThat(html).contains("<h1>見出し1</h1>");
        assertThat(html).contains("<h2>見出し2</h2>");
        assertThat(html).contains("<h3>見出し3</h3>");
        assertThat(html).contains("<strong>太字</strong>");
        assertThat(html).contains("<em>斜体</em>");
        assertThat(html).contains("<code>インラインコード</code>");
        assertThat(html).contains("<pre><code");
        assertThat(html).contains("public class Test");
        assertThat(html).contains("<a href=\"https://example.com\">リンクテスト</a>");
        assertThat(html).contains("<blockquote>");
        assertThat(html).contains("<ol>");
        assertThat(html).contains("<ul>");
    }

    @Test
    @DisplayName("日本語コンテンツが正しく処理されること")
    void shouldProcessJapaneseContent() {
        var content = """
            ---
            title: 日本語テスト
            description: 日本語の説明文
            ---
            
            # 日本語見出し
            
            これは日本語のコンテンツです。「引用符」や（括弧）も含まれます。
            
            ## リスト例
            
            - あいうえお
            - かきくけこ
            - さしすせそ
            
            **強調** や *斜体* も日本語で使用できます。
            """;

        var page = parser.parseContent("japanese.md", content);

        assertThat(page.title()).isEqualTo("日本語テスト");
        assertThat(page.description()).isEqualTo("日本語の説明文");
        assertThat(page.renderedContent()).contains("<h1>日本語見出し</h1>");
        assertThat(page.renderedContent()).contains("これは日本語のコンテンツです");
        assertThat(page.renderedContent()).contains("<strong>強調</strong>");
    }

    @Test
    @DisplayName("空のファイルでエラーが発生しないこと")
    void shouldHandleEmptyFile() {
        var page = parser.parseContent("empty.md", "");

        assertThat(page).isNotNull();
        assertThat(page.title()).isEqualTo("Empty");
        assertThat(page.rawContent()).isEmpty();
        assertThat(page.renderedContent()).isEmpty();
        assertThat(page.frontMatter()).isEmpty();
    }

    @Test
    @DisplayName("不正なフロントマターでもエラーが発生しないこと")
    void shouldHandleInvalidFrontMatter() {
        var content = """
            ---
            invalid yaml: [
            ---
            
            # ページタイトル
            
            コンテンツです。
            """;

        assertThatNoException().isThrownBy(() -> {
            var page = parser.parseContent("invalid.md", content);
            assertThat(page.title()).isEqualTo("Invalid");
            assertThat(page.renderedContent()).contains("<h1>ページタイトル</h1>");
        });
    }

    @Test
    @DisplayName("セキュリティ制限が適用されること")
    void shouldApplySecurityLimits() {
        var largeContent = "# 大きなファイル\n\n" + "x".repeat(20000000); // 20MB

        assertThatThrownBy(() -> parser.parseContent("large.md", largeContent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ファイルサイズが制限を超えています");
    }

    @Test
    @DisplayName("パストラバーサル攻撃が防止されること")
    void shouldPreventPathTraversal() {
        assertThatThrownBy(() -> parser.parseContent("../../../etc/passwd", "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("無効なファイル名です");

        assertThatThrownBy(() -> parser.parseContent("..\\windows\\system32\\config", "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("無効なファイル名です");
    }

    @Test
    @DisplayName("ファイルからページが読み込めること")
    void shouldLoadPageFromFile() {
        var tempFile = createTempMarkdownFile();
        
        var page = parser.parseFile(tempFile);
        
        assertThat(page.title()).isEqualTo("テストファイル");
        assertThat(page.renderedContent()).contains("<h1>テストファイル</h1>");
    }

    @Test
    @DisplayName("PostをPageから作成できること")
    void shouldCreatePostFromPage() {
        var content = """
            ---
            title: ブログ記事
            date: 2024-01-01T10:00:00
            tags:
              - Java
              - SSG
            categories:
              - 技術
            ---
            
            # ブログ記事
            
            これはブログ記事です。
            """;

        var page = parser.parseContent("blog-post.md", content);
        var post = Post.fromPage(page);

        assertThat(post.title()).isEqualTo("ブログ記事");
        assertThat(post.tags()).contains("Java", "SSG");
        assertThat(post.categories()).contains("技術");
        assertThat(post.isPublished()).isTrue();
    }

    @Test
    @DisplayName("長いファイル名はエラーになること")
    void shouldRejectTooLongFilename() {
        String longFilename = "a".repeat(300) + ".md";
        
        assertThatThrownBy(() -> parser.parseContent(longFilename, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ファイル名が長すぎます");
    }

    @Test
    @DisplayName("nullファイル名はエラーになること")
    void shouldRejectNullFilename() {
        assertThatThrownBy(() -> parser.parseContent(null, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ファイル名は空にできません");
    }

    @Test
    @DisplayName("空のファイル名はエラーになること")
    void shouldRejectEmptyFilename() {
        assertThatThrownBy(() -> parser.parseContent("", "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ファイル名は空にできません");
    }

    @Test
    @DisplayName("危険な文字を含むファイル名はエラーになること")
    void shouldRejectDangerousFilename() {
        String[] dangerousFilenames = {
            "file<>.md",
            "file|.md", 
            "file?.md",
            "file*.md",
            "file\".md"
        };

        for (String filename : dangerousFilenames) {
            assertThatThrownBy(() -> parser.parseContent(filename, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無効なファイル名です")
                .withFailMessage("Should reject: " + filename);
        }
    }

    @Test
    @DisplayName("スラグが正しく生成されること")
    void shouldGenerateSlugCorrectly() {
        var page1 = parser.parseContent("test-file.md", "# Test");
        var page2 = parser.parseContent("Another_File.md", "# Another");
        var page3 = parser.parseContent("Special---Characters.md", "# Special");

        assertThat(page1.slug()).isEqualTo("test-file");
        assertThat(page2.slug()).isEqualTo("another-file");
        assertThat(page3.slug()).isEqualTo("special-characters");
    }

    @Test
    @DisplayName("フロントマターのサイズ制限を検証すること")
    void shouldValidateFrontMatterSize() {
        // 大きなフロントマター（100KB超）
        String largeFrontMatter = "title: Test\n" + "large_field: " + "x".repeat(200000);
        String content = "---\n" + largeFrontMatter + "\n---\n\n# Content";

        assertThatThrownBy(() -> parser.parseContent("test.md", content))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("フロントマターのサイズが制限を超えています");
    }

    private Path createTempMarkdownFile() {
        try {
            var tempFile = java.nio.file.Files.createTempFile("test", ".md");
            var content = """
                ---
                title: テストファイル
                ---
                
                # テストファイル
                
                ファイルからの読み込みテストです。
                """;
            java.nio.file.Files.writeString(tempFile, content);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}