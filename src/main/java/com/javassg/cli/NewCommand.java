package com.javassg.cli;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * newコマンドの実装
 */
public class NewCommand {
    
    private static final Logger logger = LoggerFactory.getLogger(NewCommand.class);
    
    private static final List<String> AVAILABLE_TEMPLATES = Arrays.asList(
        "default", "blog", "portfolio", "documentation"
    );
    
    public int execute(String[] args, Path workingDir) throws Exception {
        NewOptions options = parseOptions(args);
        
        if (options.help) {
            showHelp();
            return 0;
        }
        
        if (options.listTemplates) {
            showAvailableTemplates();
            return 0;
        }
        
        if (options.contentType == null) {
            System.err.println("サポートされていないコンテンツタイプです");
            System.err.println("利用可能: post, page, site");
            return 1;
        }
        
        if (options.title == null || options.title.trim().isEmpty()) {
            System.err.println("タイトルが指定されていません");
            System.err.println("使用方法: javassg new " + options.contentType + " <title>");
            return 1;
        }
        
        try {
            switch (options.contentType) {
                case "post":
                    return createPost(options, workingDir);
                case "page":
                    return createPage(options, workingDir);
                case "site":
                    return createSite(options, workingDir);
                default:
                    System.err.println("サポートされていないコンテンツタイプ: " + options.contentType);
                    System.err.println("利用可能: post, page, site");
                    return 1;
            }
        } catch (Exception e) {
            logger.error("コンテンツ作成中にエラーが発生しました", e);
            System.err.println("エラー: " + e.getMessage());
            return 1;
        }
    }
    
    private NewOptions parseOptions(String[] args) {
        NewOptions options = new NewOptions();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h":
                    options.help = true;
                    break;
                case "--list-templates":
                    options.listTemplates = true;
                    break;
                case "--force":
                    options.force = true;
                    break;
                case "--draft":
                    options.draft = true;
                    break;
                case "--published":
                    options.published = true;
                    break;
                case "--category":
                    if (i + 1 < args.length) {
                        options.category = args[++i];
                    }
                    break;
                case "--tag":
                    if (i + 1 < args.length) {
                        options.tags = args[++i];
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        options.date = args[++i];
                    }
                    break;
                case "--layout":
                    if (i + 1 < args.length) {
                        options.layout = args[++i];
                    }
                    break;
                case "--template":
                    if (i + 1 < args.length) {
                        options.template = args[++i];
                    }
                    break;
                case "new":
                    // skip command itself
                    break;
                default:
                    if (options.contentType == null && !args[i].startsWith("--")) {
                        options.contentType = args[i];
                    } else if (options.title == null && !args[i].startsWith("--")) {
                        options.title = args[i];
                    }
                    break;
            }
        }
        
        return options;
    }
    
    private int createPost(NewOptions options, Path workingDir) throws IOException {
        // 日付の処理
        LocalDate postDate;
        if (options.date != null) {
            try {
                postDate = LocalDate.parse(options.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                System.err.println("無効な日付形式: " + options.date);
                System.err.println("正しい形式: YYYY-MM-DD");
                return 1;
            }
        } else {
            postDate = LocalDate.now();
        }
        
        // ファイル名のサニタイズ
        String sanitizedTitle = sanitizeFilename(options.title);
        if (!sanitizedTitle.equals(options.title)) {
            System.out.println("ファイル名をサニタイズしました: " + sanitizedTitle);
        }
        
        // ディレクトリの決定
        Path postsDir;
        if (options.draft) {
            postsDir = workingDir.resolve("content/drafts");
            System.out.println("下書きとして作成します");
        } else {
            postsDir = workingDir.resolve("content/posts");
        }
        
        Files.createDirectories(postsDir);
        
        // ファイル名の生成
        String dateStr = postDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filename = dateStr + "-" + sanitizedTitle + ".md";
        Path postFile = postsDir.resolve(filename);
        
        // 既存ファイルのチェック
        if (Files.exists(postFile) && !options.force) {
            System.err.println("ファイルが既に存在します: " + postFile.getFileName());
            System.err.println("上書きするには --force オプションを使用してください");
            return 1;
        }
        
        if (Files.exists(postFile) && options.force) {
            System.out.println("既存ファイルを上書きしました: " + postFile.getFileName());
        }
        
        // フロントマターの生成
        String frontMatter = createPostFrontMatter(options, postDate);
        String content = frontMatter + "\n\n# " + options.title + "\n\n記事の内容をここに書いてください。\n";
        
        Files.writeString(postFile, content, StandardCharsets.UTF_8);
        
        System.out.println("新しい記事を作成しました: " + postFile);
        
        return 0;
    }
    
    private int createPage(NewOptions options, Path workingDir) throws IOException {
        // ファイル名のサニタイズ
        String sanitizedTitle = sanitizeFilename(options.title);
        
        // サブディレクトリの処理
        Path pageFile;
        if (options.title.contains("/")) {
            String[] parts = options.title.split("/");
            Path subDir = workingDir.resolve("content");
            
            for (int i = 0; i < parts.length - 1; i++) {
                subDir = subDir.resolve(parts[i]);
            }
            
            Files.createDirectories(subDir);
            System.out.println("サブディレクトリを作成しました: " + subDir);
            
            pageFile = subDir.resolve(parts[parts.length - 1] + ".md");
        } else {
            Files.createDirectories(workingDir.resolve("content"));
            pageFile = workingDir.resolve("content/" + sanitizedTitle + ".md");
        }
        
        // 既存ファイルのチェック
        if (Files.exists(pageFile) && !options.force) {
            System.err.println("ファイルが既に存在します: " + pageFile.getFileName());
            System.err.println("上書きするには --force オプションを使用してください");
            return 1;
        }
        
        if (Files.exists(pageFile) && options.force) {
            System.out.println("既存ファイルを上書きしました: " + pageFile.getFileName());
        }
        
        // フロントマターの生成
        String frontMatter = createPageFrontMatter(options);
        String content = frontMatter + "\n\n# " + options.title + "\n\nページの内容をここに書いてください。\n";
        
        Files.writeString(pageFile, content, StandardCharsets.UTF_8);
        
        System.out.println("新しいページを作成しました: " + pageFile);
        
        return 0;
    }
    
    private int createSite(NewOptions options, Path workingDir) throws IOException {
        String siteName = options.title;
        Path siteDir = workingDir.resolve(siteName);
        
        if (Files.exists(siteDir) && !options.force) {
            System.err.println("ディレクトリが既に存在します: " + siteName);
            System.err.println("上書きするには --force オプションを使用してください");
            return 1;
        }
        
        // サイト構造の作成
        createSiteStructure(siteDir, options);
        
        System.out.println("新しいサイト '" + siteName + "' を作成しました!");
        System.out.println();
        System.out.println("次のステップ:");
        System.out.println("  cd " + siteName);
        System.out.println("  javassg serve");
        
        return 0;
    }
    
    private void createSiteStructure(Path siteDir, NewOptions options) throws IOException {
        // 基本ディレクトリ構造
        Files.createDirectories(siteDir.resolve("content"));
        Files.createDirectories(siteDir.resolve("templates"));
        Files.createDirectories(siteDir.resolve("static/css"));
        Files.createDirectories(siteDir.resolve("static/js"));
        Files.createDirectories(siteDir.resolve("static/images"));
        
        // テンプレート固有の構造
        if ("blog".equals(options.template)) {
            Files.createDirectories(siteDir.resolve("content/posts"));
            Files.writeString(siteDir.resolve("templates/post.html"), createPostTemplate(), StandardCharsets.UTF_8);
            Files.writeString(siteDir.resolve("templates/archive.html"), createArchiveTemplate(), StandardCharsets.UTF_8);
        }
        
        // 設定ファイル
        String configContent = createSiteConfig(options);
        Files.writeString(siteDir.resolve("config.yaml"), configContent, StandardCharsets.UTF_8);
        
        // デフォルトファイル
        Files.writeString(siteDir.resolve("content/index.md"), createIndexPage(), StandardCharsets.UTF_8);
        Files.writeString(siteDir.resolve("templates/base.html"), createBaseTemplate(), StandardCharsets.UTF_8);
        Files.writeString(siteDir.resolve("static/css/style.css"), createDefaultCSS(), StandardCharsets.UTF_8);
    }
    
    private String createPostFrontMatter(NewOptions options, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(options.title).append("\"\n");
        sb.append("date: ").append(date).append("\n");
        sb.append("published: ").append(options.published && !options.draft).append("\n");
        
        if (options.category != null) {
            sb.append("categories: [\"").append(options.category).append("\"]\n");
        } else {
            sb.append("categories: []\n");
        }
        
        if (options.tags != null) {
            String[] tagArray = options.tags.split(",");
            sb.append("tags: [");
            for (int i = 0; i < tagArray.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(tagArray[i].trim()).append("\"");
            }
            sb.append("]\n");
        } else {
            sb.append("tags: []\n");
        }
        
        sb.append("---");
        return sb.toString();
    }
    
    private String createPageFrontMatter(NewOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(options.title).append("\"\n");
        sb.append("layout: \"").append(options.layout != null ? options.layout : "page").append("\"\n");
        sb.append("---");
        return sb.toString();
    }
    
    private String createSiteConfig(NewOptions options) {
        String template = options.template != null ? options.template : "default";
        
        StringBuilder config = new StringBuilder();
        config.append("site:\n");
        config.append("  title: \"").append(options.title).append("\"\n");
        config.append("  description: \"Description of the site\"\n");
        config.append("  url: \"https://example.com\"\n");
        config.append("  language: \"ja-JP\"\n");
        config.append("  author:\n");
        config.append("    name: \"Author Name\"\n");
        config.append("    email: \"author@example.com\"\n");
        config.append("\n");
        config.append("build:\n");
        config.append("  contentDirectory: \"content\"\n");
        config.append("  outputDirectory: \"_site\"\n");
        config.append("  staticDirectory: \"static\"\n");
        config.append("  templatesDirectory: \"templates\"\n");
        
        if ("blog".equals(template)) {
            config.append("\n");
            config.append("blog:\n");
            config.append("  postsPerPage: 10\n");
            config.append("  generateArchive: true\n");
            config.append("  generateCategories: true\n");
            config.append("  generateTags: true\n");
        }
        
        return config.toString();
    }
    
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[/\\\\*?<>|\"]", "-")
                      .replaceAll("\\s+", "-")
                      .replaceAll("-+", "-");
    }
    
    private String createIndexPage() {
        return """
            ---
            title: "ホーム"
            description: "サイトのホームページ"
            ---
            
            # ようこそ
            
            新しいサイトへようこそ！
            """;
    }
    
    private String createBaseTemplate() {
        return """
            <!DOCTYPE html>
            <html lang="ja">
            <head>
                <meta charset="utf-8">
                <title>{{ page.title }} - {{ site.title }}</title>
                <meta name="description" content="{{ page.description | default: site.description }}">
                <link rel="stylesheet" href="/css/style.css">
            </head>
            <body>
                <header>
                    <h1><a href="/">{{ site.title }}</a></h1>
                </header>
                <main>
                    {{ content }}
                </main>
                <footer>
                    <p>&copy; {{ site.author.name }}</p>
                </footer>
            </body>
            </html>
            """;
    }
    
    private String createPostTemplate() {
        return """
            {% extends "base.html" %}
            
            {% block content %}
            <article>
                <header>
                    <h1>{{ page.title }}</h1>
                    <time datetime="{{ page.date }}">{{ page.date | date: "%Y年%m月%d日" }}</time>
                </header>
                {{ content }}
            </article>
            {% endblock %}
            """;
    }
    
    private String createArchiveTemplate() {
        return """
            {% extends "base.html" %}
            
            {% block content %}
            <h1>記事一覧</h1>
            <ul>
            {% for post in posts %}
                <li>
                    <a href="{{ post.url }}">{{ post.title }}</a>
                    <time datetime="{{ post.date }}">{{ post.date | date: "%Y-%m-%d" }}</time>
                </li>
            {% endfor %}
            </ul>
            {% endblock %}
            """;
    }
    
    private String createDefaultCSS() {
        return """
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                line-height: 1.6;
                max-width: 800px;
                margin: 0 auto;
                padding: 20px;
            }
            
            header {
                border-bottom: 1px solid #eee;
                margin-bottom: 2rem;
                padding-bottom: 1rem;
            }
            
            header h1 a {
                text-decoration: none;
                color: #333;
            }
            
            main {
                margin-bottom: 2rem;
            }
            
            footer {
                border-top: 1px solid #eee;
                padding-top: 1rem;
                text-align: center;
                color: #666;
            }
            """;
    }
    
    private void showAvailableTemplates() {
        System.out.println("利用可能なテンプレート:");
        for (String template : AVAILABLE_TEMPLATES) {
            System.out.println("  " + template);
        }
    }
    
    private void showHelp() {
        System.out.println("Usage: javassg new <type> <title> [options]");
        System.out.println();
        System.out.println("Types:");
        System.out.println("  post <title>          Create a new blog post");
        System.out.println("  page <title>          Create a new page");
        System.out.println("  site <name>           Create a new site");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --category <cat>      Set post category");
        System.out.println("  --tag <tags>          Set post tags (comma-separated)");
        System.out.println("  --date <date>         Set post date (YYYY-MM-DD)");
        System.out.println("  --layout <layout>     Set page layout");
        System.out.println("  --template <template> Set site template");
        System.out.println("  --published           Mark post as published");
        System.out.println("  --draft               Create as draft");
        System.out.println("  --force               Overwrite existing files");
        System.out.println("  --list-templates      Show available site templates");
        System.out.println("  --help, -h            Show this help message");
    }
    
    private static class NewOptions {
        boolean help = false;
        boolean listTemplates = false;
        boolean force = false;
        boolean draft = false;
        boolean published = false;
        String contentType = null;
        String title = null;
        String category = null;
        String tags = null;
        String date = null;
        String layout = null;
        String template = null;
    }
}
