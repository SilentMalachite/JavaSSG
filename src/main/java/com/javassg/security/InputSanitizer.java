package com.javassg.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 入力データのサニタイゼーション
 */
public class InputSanitizer {
    
    private static final Logger logger = LoggerFactory.getLogger(InputSanitizer.class);
    
    // HTMLエンティティマッピング
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();
    static {
        HTML_ENTITIES.put("&", "&amp;");
        HTML_ENTITIES.put("<", "&lt;");
        HTML_ENTITIES.put(">", "&gt;");
        HTML_ENTITIES.put("\"", "&quot;");
        HTML_ENTITIES.put("'", "&#39;");
        HTML_ENTITIES.put("/", "&#x2F;");
    }
    
    // 許可されたHTMLタグ
    private static final List<String> ALLOWED_HTML_TAGS = List.of(
        "p", "br", "strong", "em", "b", "i", "u", "h1", "h2", "h3", "h4", "h5", "h6",
        "ul", "ol", "li", "blockquote", "pre", "code", "a", "img", "table", "thead", 
        "tbody", "tr", "th", "td", "div", "span", "hr"
    );
    
    // 許可されたHTML属性
    private static final Map<String, List<String>> ALLOWED_ATTRIBUTES = new HashMap<>();
    static {
        ALLOWED_ATTRIBUTES.put("a", List.of("href", "title", "target"));
        ALLOWED_ATTRIBUTES.put("img", List.of("src", "alt", "title", "width", "height"));
        ALLOWED_ATTRIBUTES.put("blockquote", List.of("cite"));
        ALLOWED_ATTRIBUTES.put("table", List.of("class"));
        ALLOWED_ATTRIBUTES.put("td", List.of("colspan", "rowspan"));
        ALLOWED_ATTRIBUTES.put("th", List.of("colspan", "rowspan"));
        ALLOWED_ATTRIBUTES.put("div", List.of("class"));
        ALLOWED_ATTRIBUTES.put("span", List.of("class"));
        ALLOWED_ATTRIBUTES.put("code", List.of("class"));
        ALLOWED_ATTRIBUTES.put("pre", List.of("class"));
    }
    
    // 危険なプロトコルパターン
    private static final Pattern DANGEROUS_PROTOCOL_PATTERN = Pattern.compile(
        "^\\s*(javascript|data|vbscript|file|about):", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * HTMLエスケープ
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            String entity = HTML_ENTITIES.get(String.valueOf(c));
            if (entity != null) {
                result.append(entity);
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * HTMLアンエスケープ
     */
    public String unescapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            result = result.replace(entry.getValue(), entry.getKey());
        }
        
        return result;
    }
    
    /**
     * 安全なHTMLサニタイゼーション
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        // 最初にHTMLエスケープ
        String sanitized = escapeHtml(input);
        
        // 許可されたタグのみアンエスケープ
        for (String tag : ALLOWED_HTML_TAGS) {
            // 開始タグ
            sanitized = sanitized.replaceAll(
                "&lt;" + tag + "&gt;", 
                "<" + tag + ">"
            );
            
            // 終了タグ
            sanitized = sanitized.replaceAll(
                "&lt;/" + tag + "&gt;", 
                "</" + tag + ">"
            );
            
            // 属性付きタグの処理
            sanitized = sanitizeTagAttributes(sanitized, tag);
        }
        
        return sanitized;
    }
    
    /**
     * タグの属性をサニタイズ
     */
    private String sanitizeTagAttributes(String input, String tagName) {
        List<String> allowedAttrs = ALLOWED_ATTRIBUTES.get(tagName);
        if (allowedAttrs == null) {
            return input;
        }
        
        // 簡単な属性サニタイゼーション
        // 実際の実装では、より高度なHTMLパーサーを使用することを推奨
        Pattern tagPattern = Pattern.compile(
            "&lt;" + tagName + "\\s+([^&]+)&gt;", 
            Pattern.CASE_INSENSITIVE
        );
        
        return tagPattern.matcher(input).replaceAll(match -> {
            String attributes = match.group(1);
            String sanitizedAttrs = sanitizeAttributes(attributes, allowedAttrs);
            return "<" + tagName + (sanitizedAttrs.isEmpty() ? "" : " " + sanitizedAttrs) + ">";
        });
    }
    
    /**
     * 属性のサニタイゼーション
     */
    private String sanitizeAttributes(String attributes, List<String> allowedAttrs) {
        StringBuilder result = new StringBuilder();
        
        // 属性を解析（簡易版）
        String[] parts = attributes.split("\\s+");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim().replaceAll("^[\"']|[\"']$", "");
                
                if (allowedAttrs.contains(key)) {
                    // 危険なプロトコルをチェック
                    if ("href".equals(key) || "src".equals(key)) {
                        if (DANGEROUS_PROTOCOL_PATTERN.matcher(value).find()) {
                            continue; // 危険なプロトコルはスキップ
                        }
                    }
                    
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(key).append("=\"").append(escapeHtml(value)).append("\"");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * ファイルパスのサニタイゼーション
     */
    public String sanitizeFilePath(String path) {
        if (path == null) {
            return "";
        }
        
        // パストラバーサル攻撃を防ぐ
        String sanitized = path.replaceAll("\\.{2,}", ".");
        
        // 危険な文字を削除
        sanitized = sanitized.replaceAll("[<>:\"|?*]", "");
        
        // 連続するスラッシュを単一に
        sanitized = sanitized.replaceAll("[/\\\\]+", "/");
        
        // 先頭と末尾のスラッシュを削除
        sanitized = sanitized.replaceAll("^[/\\\\]+|[/\\\\]+$", "");
        
        return sanitized;
    }
    
    /**
     * URLのサニタイゼーション
     */
    public String sanitizeUrl(String url) {
        if (url == null) {
            return "";
        }
        
        // 危険なプロトコルを削除
        if (DANGEROUS_PROTOCOL_PATTERN.matcher(url).find()) {
            logger.warn("危険なURLプロトコルが検出されました: {}", url);
            return "";
        }
        
        // 基本的なURLエンコーディング
        return url.replaceAll("\\s", "%20");
    }
    
    /**
     * 文字列の長さを制限
     */
    public String truncateString(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        
        if (input.length() <= maxLength) {
            return input;
        }
        
        return input.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 改行文字の正規化
     */
    public String normalizeLineBreaks(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replaceAll("\\r\\n|\\r", "\n");
    }
    
    /**
     * 制御文字の削除
     */
    public String removeControlCharacters(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 32 && c != 127) { // 制御文字以外
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * スラッグの生成（URL安全）
     */
    public String generateSlug(String input) {
        if (input == null) {
            return "";
        }
        
        return input.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }
}