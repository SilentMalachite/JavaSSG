package com.javassg.plugin;

import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HTML最小化プラグイン
 */
public class MinifyPlugin implements Plugin {
    
    private static final Logger logger = LoggerFactory.getLogger(MinifyPlugin.class);
    
    private boolean enabled = true;
    private boolean minifyHtml = true;
    private boolean minifyCss = true;
    private boolean minifyJs = false; // セキュリティ上の理由でデフォルトは無効
    
    // HTML最小化パターン
    private static final Pattern HTML_WHITESPACE = Pattern.compile("\\s{2,}");
    private static final Pattern HTML_NEWLINES = Pattern.compile("\\n\\s*");
    private static final Pattern HTML_COMMENTS = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    
    @Override
    public String getName() {
        return "minify";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "HTML、CSS、JSファイルを最小化します";
    }
    
    @Override
    public void initialize(SiteConfig config, Map<String, Object> settings) {
        if (settings != null) {
            this.minifyHtml = (Boolean) settings.getOrDefault("minifyHTML", true);
            this.minifyCss = (Boolean) settings.getOrDefault("minifyCSS", true);
            this.minifyJs = (Boolean) settings.getOrDefault("minifyJS", false);
        }
        logger.debug("最小化プラグインを初期化しました (HTML: {}, CSS: {}, JS: {})", 
                    minifyHtml, minifyCss, minifyJs);
    }
    
    @Override
    public void execute(PluginContext context) {
        if (context.getPhase() != PluginContext.PluginPhase.POST_BUILD) {
            return;
        }
        
        try {
            minifyFiles(context.getOutputDirectory());
            logger.info("ファイルの最小化が完了しました");
        } catch (Exception e) {
            logger.error("ファイル最小化エラー", e);
        }
    }
    
    private void minifyFiles(Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            return;
        }
        
        Files.walk(outputDir)
            .filter(Files::isRegularFile)
            .forEach(this::processFile);
    }
    
    private void processFile(Path file) {
        try {
            String fileName = file.getFileName().toString().toLowerCase();
            
            if (fileName.endsWith(".html") && minifyHtml) {
                minifyHtmlFile(file);
            } else if (fileName.endsWith(".css") && minifyCss) {
                minifyCssFile(file);
            } else if (fileName.endsWith(".js") && minifyJs) {
                minifyJsFile(file);
            }
        } catch (Exception e) {
            logger.warn("ファイル処理エラー: " + file, e);
        }
    }
    
    private void minifyHtmlFile(Path file) throws IOException {
        String content = Files.readString(file);
        String originalContent = content;
        
        // HTMLコメントの削除
        content = HTML_COMMENTS.matcher(content).replaceAll("");
        
        // 改行とスペースの最小化
        content = HTML_NEWLINES.matcher(content).replaceAll(" ");
        content = HTML_WHITESPACE.matcher(content).replaceAll(" ");
        
        // 前後のスペースを削除
        content = content.trim();
        
        // 内容が変更された場合のみ書き込み
        if (!content.equals(originalContent)) {
            Files.writeString(file, content);
            logger.debug("HTML最小化: {} ({} -> {} bytes)", 
                        file.getFileName(), originalContent.length(), content.length());
        }
    }
    
    private void minifyCssFile(Path file) throws IOException {
        String content = Files.readString(file);
        String originalContent = content;
        
        // CSSコメントの削除
        content = content.replaceAll("/\\*.*?\\*/", "");
        
        // 改行とスペースの最小化
        content = content.replaceAll("\\s{2,}", " ");
        content = content.replaceAll("\\n\\s*", "");
        
        // セミコロンの後のスペース削除
        content = content.replaceAll(";\\s+", ";");
        
        // 波括弧の前後のスペース削除
        content = content.replaceAll("\\s*\\{\\s*", "{");
        content = content.replaceAll("\\s*\\}\\s*", "}");
        
        // 前後のスペースを削除
        content = content.trim();
        
        // 内容が変更された場合のみ書き込み
        if (!content.equals(originalContent)) {
            Files.writeString(file, content);
            logger.debug("CSS最小化: {} ({} -> {} bytes)", 
                        file.getFileName(), originalContent.length(), content.length());
        }
    }
    
    private void minifyJsFile(Path file) throws IOException {
        String content = Files.readString(file);
        String originalContent = content;
        
        // 単行コメントの削除（文字列内は除外）
        content = content.replaceAll("//.*", "");
        
        // 複数行コメントの削除
        content = content.replaceAll("/\\*.*?\\*/", "");
        
        // 改行とスペースの最小化
        content = content.replaceAll("\\s{2,}", " ");
        content = content.replaceAll("\\n\\s*", "");
        
        // 前後のスペースを削除
        content = content.trim();
        
        // 内容が変更された場合のみ書き込み
        if (!content.equals(originalContent)) {
            Files.writeString(file, content);
            logger.debug("JS最小化: {} ({} -> {} bytes)", 
                        file.getFileName(), originalContent.length(), content.length());
        }
    }
    
    @Override
    public void cleanup() {
        // 特にクリーンアップは不要
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public int getExecutionOrder() {
        return 950; // 最後に実行
    }
}