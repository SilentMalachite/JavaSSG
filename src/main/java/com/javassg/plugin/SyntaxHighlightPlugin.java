package com.javassg.plugin;

import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * シンタックスハイライトプラグイン
 */
public class SyntaxHighlightPlugin implements Plugin {
    
    private static final Logger logger = LoggerFactory.getLogger(SyntaxHighlightPlugin.class);
    
    private boolean enabled = true;
    private String theme = "default";
    
    private static final Pattern CODE_BLOCK_PATTERN = 
        Pattern.compile("<pre><code class=\"language-([^\"]+)\">(.*?)</code></pre>", Pattern.DOTALL);
    
    @Override
    public String getName() {
        return "syntaxHighlight";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "コードブロックのシンタックスハイライトを拡張します";
    }
    
    @Override
    public void initialize(SiteConfig config, Map<String, Object> settings) {
        if (settings != null) {
            this.theme = (String) settings.getOrDefault("theme", "default");
        }
        logger.debug("シンタックスハイライトプラグインを初期化しました (テーマ: {})", theme);
    }
    
    @Override
    public void execute(PluginContext context) {
        if (context.getPhase() != PluginContext.PluginPhase.POST_TEMPLATE) {
            return;
        }
        
        try {
            processHtmlFiles(context.getOutputDirectory());
            logger.info("シンタックスハイライト処理が完了しました");
        } catch (Exception e) {
            logger.error("シンタックスハイライト処理エラー", e);
        }
    }
    
    private void processHtmlFiles(Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            return;
        }
        
        Files.walk(outputDir)
            .filter(Files::isRegularFile)
            .filter(file -> file.getFileName().toString().endsWith(".html"))
            .forEach(this::processHtmlFile);
    }
    
    private void processHtmlFile(Path file) {
        try {
            String content = Files.readString(file);
            String originalContent = content;
            
            // コードブロックを検索して処理
            Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String language = matcher.group(1);
                String code = matcher.group(2);
                
                String highlightedCode = highlightCode(code, language);
                String replacement = String.format(
                    "<pre><code class=\"language-%s hljs\">%s</code></pre>", 
                    language, highlightedCode);
                
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            
            // Prism.jsまたはhighlight.jsのCSSとJSを追加
            if (result.toString().contains("hljs")) {
                result = addHighlightResources(result);
            }
            
            // 内容が変更された場合のみ書き込み
            if (!result.toString().equals(originalContent)) {
                Files.writeString(file, result.toString());
                logger.debug("シンタックスハイライト処理: {}", file.getFileName());
            }
            
        } catch (Exception e) {
            logger.warn("HTMLファイル処理エラー: " + file, e);
        }
    }
    
    private String highlightCode(String code, String language) {
        // 基本的なシンタックスハイライト
        // 実際の実装では、より高度なハイライトライブラリを使用
        
        return switch (language.toLowerCase()) {
            case "java" -> highlightJava(code);
            case "javascript", "js" -> highlightJavaScript(code);
            case "html" -> highlightHtml(code);
            case "css" -> highlightCss(code);
            case "json" -> highlightJson(code);
            default -> escapeHtml(code);
        };
    }
    
    private String highlightJava(String code) {
        code = escapeHtml(code);
        
        // キーワードのハイライト
        code = code.replaceAll("\\b(public|private|protected|static|final|class|interface|extends|implements|import|package)\\b", 
                              "<span class=\"hljs-keyword\">$1</span>");
        
        // 文字列のハイライト
        code = code.replaceAll("\"([^\"]*?)\"", "<span class=\"hljs-string\">\"$1\"</span>");
        
        // コメントのハイライト
        code = code.replaceAll("//([^\\n]*)", "<span class=\"hljs-comment\">//$1</span>");
        code = code.replaceAll("/\\*([^*]*?)\\*/", "<span class=\"hljs-comment\">/*$1*/</span>");
        
        return code;
    }
    
    private String highlightJavaScript(String code) {
        code = escapeHtml(code);
        
        // キーワードのハイライト
        code = code.replaceAll("\\b(var|let|const|function|return|if|else|for|while|class|extends)\\b", 
                              "<span class=\"hljs-keyword\">$1</span>");
        
        // 文字列のハイライト
        code = code.replaceAll("\"([^\"]*?)\"", "<span class=\"hljs-string\">\"$1\"</span>");
        code = code.replaceAll("'([^']*?)'", "<span class=\"hljs-string\">'$1'</span>");
        
        return code;
    }
    
    private String highlightHtml(String code) {
        code = escapeHtml(code);
        
        // HTMLタグのハイライト
        code = code.replaceAll("&lt;([^&gt;]+)&gt;", "<span class=\"hljs-tag\">&lt;$1&gt;</span>");
        
        return code;
    }
    
    private String highlightCss(String code) {
        code = escapeHtml(code);
        
        // CSSプロパティのハイライト
        code = code.replaceAll("([a-zA-Z-]+)\\s*:", "<span class=\"hljs-attribute\">$1</span>:");
        
        // CSS値のハイライト
        code = code.replaceAll(":\\s*([^;]+);", ": <span class=\"hljs-value\">$1</span>;");
        
        return code;
    }
    
    private String highlightJson(String code) {
        code = escapeHtml(code);
        
        // JSONキーのハイライト
        code = code.replaceAll("\"([^\"]+)\"\\s*:", "<span class=\"hljs-attr\">\"$1\"</span>:");
        
        // JSON文字列値のハイライト
        code = code.replaceAll(":\\s*\"([^\"]+)\"", ": <span class=\"hljs-string\">\"$1\"</span>");
        
        return code;
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private StringBuffer addHighlightResources(StringBuffer content) {
        // highlight.jsのCSSとJSを追加
        String cssLink = "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/" + theme + ".min.css\">";
        String jsScript = "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js\"></script>";
        String initScript = "<script>hljs.highlightAll();</script>";
        
        // </head>の直前にCSSを追加
        int headIndex = content.indexOf("</head>");
        if (headIndex != -1) {
            content.insert(headIndex, cssLink + "\n");
        }
        
        // </body>の直前にJSを追加
        int bodyIndex = content.indexOf("</body>");
        if (bodyIndex != -1) {
            content.insert(bodyIndex, jsScript + "\n" + initScript + "\n");
        }
        
        return content;
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
        return 700; // テンプレート処理後に実行
    }
}