package com.javassg.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * newコマンドのテスト
 */
class NewCommandTest {

    private NewCommand newCommand;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        newCommand = new NewCommand();
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void shouldCreateNewPost() throws Exception {
        String[] args = {"new", "post", "私の新しい記事"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        // posts ディレクトリが作成されることを確認
        Path postsDir = tempDir.resolve("content/posts");
        assertThat(postsDir).exists();
        
        // 記事ファイルが作成されることを確認
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path postFile = postsDir.resolve(todayStr + "-私の新しい記事.md");
        assertThat(postFile).exists();
        
        // ファイル内容を確認
        String content = Files.readString(postFile);
        assertThat(content).contains("title: \"私の新しい記事\"");
        assertThat(content).contains("published: false");
        assertThat(content).contains("categories: []");
        assertThat(content).contains("tags: []");
        assertThat(content).contains("# 私の新しい記事");
        
        String output = outputStream.toString();
        assertThat(output).contains("新しい記事を作成しました");
        assertThat(output).contains(postFile.toString());
    }

    @Test
    void shouldCreateNewPage() throws Exception {
        String[] args = {"new", "page", "新しいページ"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        // ページファイルが作成されることを確認
        Path pageFile = tempDir.resolve("content/新しいページ.md");
        assertThat(pageFile).exists();
        
        // ファイル内容を確認
        String content = Files.readString(pageFile);
        assertThat(content).contains("title: \"新しいページ\"");
        assertThat(content).contains("layout: \"page\"");
        assertThat(content).contains("# 新しいページ");
        
        String output = outputStream.toString();
        assertThat(output).contains("新しいページを作成しました");
    }

    @Test
    void shouldCreatePostWithCustomOptions() throws Exception {
        String[] args = {"new", "post", "カスタム記事", 
                        "--category", "tech", 
                        "--tag", "java,programming",
                        "--published"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path postFile = tempDir.resolve("content/posts/" + todayStr + "-カスタム記事.md");
        assertThat(postFile).exists();
        
        String content = Files.readString(postFile);
        assertThat(content).contains("published: true");
        assertThat(content).contains("categories: [\"tech\"]");
        assertThat(content).contains("tags: [\"java\", \"programming\"]");
    }

    @Test
    void shouldCreatePostWithCustomDate() throws Exception {
        String[] args = {"new", "post", "過去の記事", "--date", "2023-12-01"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path postFile = tempDir.resolve("content/posts/2023-12-01-過去の記事.md");
        assertThat(postFile).exists();
        
        String content = Files.readString(postFile);
        assertThat(content).contains("date: 2023-12-01");
    }

    @Test
    void shouldCreatePageWithCustomLayout() throws Exception {
        String[] args = {"new", "page", "カスタムページ", "--layout", "custom"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path pageFile = tempDir.resolve("content/カスタムページ.md");
        String content = Files.readString(pageFile);
        assertThat(content).contains("layout: \"custom\"");
    }

    @Test
    void shouldCreatePageInSubdirectory() throws Exception {
        String[] args = {"new", "page", "docs/API仕様書"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path pageFile = tempDir.resolve("content/docs/API仕様書.md");
        assertThat(pageFile).exists();
        assertThat(pageFile.getParent()).exists();
        
        String output = outputStream.toString();
        assertThat(output).contains("サブディレクトリを作成");
    }

    @Test
    void shouldCreateSiteStructure() throws Exception {
        String[] args = {"new", "site", "my-blog"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path siteDir = tempDir.resolve("my-blog");
        assertThat(siteDir).exists();
        
        // 基本ディレクトリ構造
        assertThat(siteDir.resolve("content")).exists();
        assertThat(siteDir.resolve("templates")).exists();
        assertThat(siteDir.resolve("static")).exists();
        assertThat(siteDir.resolve("config.yaml")).exists();
        
        // デフォルトファイル
        assertThat(siteDir.resolve("content/index.md")).exists();
        assertThat(siteDir.resolve("templates/base.html")).exists();
        assertThat(siteDir.resolve("static/css/style.css")).exists();
        
        String output = outputStream.toString();
        assertThat(output).contains("新しいサイト 'my-blog' を作成しました");
        assertThat(output).contains("cd my-blog");
        assertThat(output).contains("javassg serve");
    }

    @Test
    void shouldCreateSiteWithTemplate() throws Exception {
        String[] args = {"new", "site", "tech-blog", "--template", "blog"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path siteDir = tempDir.resolve("tech-blog");
        
        // ブログテンプレート固有のファイル
        assertThat(siteDir.resolve("content/posts")).exists();
        assertThat(siteDir.resolve("templates/post.html")).exists();
        assertThat(siteDir.resolve("templates/archive.html")).exists();
        
        // サンプル記事
        assertThat(siteDir.resolve("content/posts")).exists();
        
        String configContent = Files.readString(siteDir.resolve("config.yaml"));
        assertThat(configContent).contains("blog:");
        assertThat(configContent).contains("postsPerPage: 10");
    }

    @Test
    void shouldHandleExistingFile() throws Exception {
        // 既存ファイルを作成
        Files.createDirectories(tempDir.resolve("content"));
        Path existingFile = tempDir.resolve("content/existing.md");
        Files.write(existingFile, "# Existing Content".getBytes());
        
        String[] args = {"new", "page", "existing"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(1);
        
        String output = outputStream.toString();
        String error = errorStream.toString();
        String combined = output + error;
        assertThat(combined).contains("ファイルが既に存在します");
        assertThat(combined).contains("existing.md");
    }

    @Test
    void shouldForceOverwriteExistingFile() throws Exception {
        // 既存ファイルを作成
        Files.createDirectories(tempDir.resolve("content"));
        Path existingFile = tempDir.resolve("content/existing.md");
        Files.write(existingFile, "# Existing Content".getBytes());
        
        String[] args = {"new", "page", "existing", "--force"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        String content = Files.readString(existingFile);
        assertThat(content).contains("title: \"existing\"");
        
        String output = outputStream.toString();
        assertThat(output).contains("既存ファイルを上書きしました");
    }

    @Test
    void shouldHandleInvalidContentType() throws Exception {
        String[] args = {"new", "invalid", "test"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(1);
        
        String output = outputStream.toString();
        String error = errorStream.toString();
        String combined = output + error;
        assertThat(combined).contains("サポートされていないコンテンツタイプ");
        assertThat(combined).contains("invalid");
        assertThat(combined).contains("post, page, site");
    }

    @Test
    void shouldHandleMissingTitle() throws Exception {
        String[] args = {"new", "post"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(1);
        
        String output = outputStream.toString();
        String error = errorStream.toString();
        String combined = output + error;
        assertThat(combined).contains("タイトルが指定されていません");
    }

    @Test
    void shouldHandleInvalidDate() throws Exception {
        String[] args = {"new", "post", "テスト記事", "--date", "invalid-date"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(1);
        
        String output = outputStream.toString();
        String error = errorStream.toString();
        String combined = output + error;
        assertThat(combined).contains("無効な日付形式");
        assertThat(combined).contains("YYYY-MM-DD");
    }

    @Test
    void shouldSanitizeFilename() throws Exception {
        String[] args = {"new", "post", "記事/with\\special*chars?"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        // 特殊文字がサニタイズされることを確認
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path postFile = tempDir.resolve("content/posts/" + todayStr + "-記事-with-special-chars-.md");
        assertThat(postFile).exists();
        
        String output = outputStream.toString();
        assertThat(output).contains("ファイル名をサニタイズしました");
    }

    @Test
    void shouldCreateDraftsFolder() throws Exception {
        String[] args = {"new", "post", "下書き記事", "--draft"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        Path draftsDir = tempDir.resolve("content/drafts");
        assertThat(draftsDir).exists();
        
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path draftFile = draftsDir.resolve(todayStr + "-下書き記事.md");
        assertThat(draftFile).exists();
        
        String content = Files.readString(draftFile);
        assertThat(content).contains("published: false");
        
        String output = outputStream.toString();
        assertThat(output).contains("下書きとして作成");
    }

    @Test
    void shouldDisplayAvailableTemplates() throws Exception {
        String[] args = {"new", "site", "--list-templates"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        String output = outputStream.toString();
        assertThat(output).contains("利用可能なテンプレート:");
        assertThat(output).contains("default");
        assertThat(output).contains("blog");
        assertThat(output).contains("portfolio");
        assertThat(output).contains("documentation");
    }

    @Test
    void shouldShowNewCommandHelp() throws Exception {
        String[] args = {"new", "--help"};
        
        int exitCode = newCommand.execute(args, tempDir);
        
        assertThat(exitCode).isEqualTo(0);
        
        String output = outputStream.toString();
        assertThat(output).contains("Usage: javassg new");
        assertThat(output).contains("post <title>");
        assertThat(output).contains("page <title>");
        assertThat(output).contains("site <name>");
        assertThat(output).contains("Options:");
        assertThat(output).contains("--category");
        assertThat(output).contains("--tag");
        assertThat(output).contains("--published");
    }
}