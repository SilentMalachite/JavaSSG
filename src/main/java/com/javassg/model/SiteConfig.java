package com.javassg.model;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;

public record SiteConfig(
    SiteInfo site,
    BuildConfig build,
    ServerConfig server,
    BlogConfig blog,
    SecurityLimits limits,
    List<PluginConfig> plugins
) {
    // バリデーション用正規表現
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+$");
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[a-z]{2}-[A-Z]{2}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    
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
    
    /**
     * 設定値の妥当性を検証
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // サイト情報の検証
        if (site.title() == null || site.title().trim().isEmpty()) {
            errors.add("サイトタイトルは必須です");
        }
        
        if (site.url() == null || site.url().trim().isEmpty()) {
            errors.add("サイトURLは必須です");
        } else if (!URL_PATTERN.matcher(site.url()).matches()) {
            errors.add("サイトURLの形式が正しくありません: " + site.url());
        }
        
        if (site.language() == null || !LANGUAGE_PATTERN.matcher(site.language()).matches()) {
            errors.add("言語コードの形式が正しくありません。xx-XXの形式で指定してください");
        }
        
        if (site.author() != null && site.author().email() != null && 
            !EMAIL_PATTERN.matcher(site.author().email()).matches()) {
            errors.add("著者メールアドレスの形式が正しくありません: " + site.author().email());
        }
        
        // ビルド設定の検証
        if (build.contentDirectory() == null || build.contentDirectory().trim().isEmpty()) {
            errors.add("コンテンツディレクトリは必須です");
        }
        
        if (build.outputDirectory() == null || build.outputDirectory().trim().isEmpty()) {
            errors.add("出力ディレクトリは必須です");
        }
        
        if (build.staticDirectory() == null || build.staticDirectory().trim().isEmpty()) {
            errors.add("静的ファイルディレクトリは必須です");
        }
        
        if (build.templatesDirectory() == null || build.templatesDirectory().trim().isEmpty()) {
            errors.add("テンプレートディレクトリは必須です");
        }
        
        // サーバー設定の検証
        if (server.port() < 1 || server.port() > 65535) {
            errors.add("サーバーポートは1〜65535の範囲で指定してください");
        }
        
        // ブログ設定の検証
        if (blog.postsPerPage() < 1) {
            errors.add("1ページあたりの投稿数は1以上で指定してください");
        }
        
        return errors;
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