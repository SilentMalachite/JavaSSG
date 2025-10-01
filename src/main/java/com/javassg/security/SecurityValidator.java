package com.javassg.security;

import com.javassg.model.SecurityLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * セキュリティ検証クラス
 */
public class SecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidator.class);
    
    private final SecurityLimits limits;

    // OWASP HTML Sanitizer ポリシー
    private static final PolicyFactory HTML_SANITIZER_POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "b", "i", "u", "em", "strong", "code", "pre",
                      "h1", "h2", "h3", "h4", "h5", "h6",
                      "ul", "ol", "li", "blockquote", "a", "img")
        .allowAttributes("href").onElements("a")
        .allowAttributes("title").onElements("a")
        .allowAttributes("rel").matching(Pattern.compile("nofollow|noopener|noreferrer")).onElements("a")
        .allowAttributes("src", "alt", "title").onElements("img")
        .allowStandardUrlProtocols()
        .toFactory();

    // 危険なパターンの定義
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern IFRAME_PATTERN = Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern OBJECT_PATTERN = Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern EMBED_PATTERN = Pattern.compile("<embed[^>]*/?\\s*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern LINK_PATTERN = Pattern.compile("<link[^>]*/?\\s*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern META_PATTERN = Pattern.compile("<meta[^>]*/?\\s*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
    // 危険なJavaScriptイベントハンドラ（より包括的）
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
        "on(abort|blur|canplay|canplaythrough|change|click|contextmenu|controls|copy|cut|dblclick|drag|dragend|dragenter|dragstart|drop|durationchange|emptied|ended|error|focus|formchange|forminput|input|invalid|keydown|keypress|keyup|load|loadeddata|loadedmetadata|loadstart|mousedown|mousemove|mouseout|mouseover|mouseup|mousewheel|offline|online|pagehide|pageshow|paste|pause|play|playing|progress|ratechange|readystatechange|reset|resize|scroll|seeked|seeking|select|show|stalled|submit|suspend|timeupdate|toggle|unload|volumechange|waiting)[\\s]*=", 
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    // パストラバーサル攻撃パターン
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.{2}[/\\\\])|([/\\\\]\\.{2})");
    
    public SecurityValidator(SecurityLimits limits) {
        this.limits = limits != null ? limits : SecurityLimits.defaultLimits();
    }
    
    /**
     * ファイルパスの検証
     */
    public void validateFilePath(Path filePath) throws SecurityException {
        if (filePath == null) {
            throw new SecurityException("ファイルパスがnullです");
        }
        
        String pathStr = filePath.toString();
        
        // パストラバーサル攻撃の検証
        if (PATH_TRAVERSAL_PATTERN.matcher(pathStr).find()) {
            throw new SecurityException("不正なパスが検出されました: " + pathStr);
        }
        
        // ファイル名の長さ制限
        String fileName = filePath.getFileName().toString();
        if (fileName.length() > limits.maxFilenameLength()) {
            throw new SecurityException("ファイル名が長すぎます: " + fileName.length() + " > " + limits.maxFilenameLength());
        }
        
        // 危険な文字の検証
        if (containsDangerousCharacters(fileName)) {
            throw new SecurityException("危険な文字が含まれています: " + fileName);
        }
        
        logger.debug("ファイルパス検証成功: {}", pathStr);
    }
    
    /**
     * ファイルサイズの検証
     */
    public void validateFileSize(Path filePath) throws SecurityException, IOException {
        if (!Files.exists(filePath)) {
            throw new SecurityException("ファイルが存在しません: " + filePath);
        }
        
        long fileSize = Files.size(filePath);
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            if (fileSize > limits.maxMarkdownFileSize()) {
                throw new SecurityException("Markdownファイルサイズが制限を超えています: " + fileSize + " > " + limits.maxMarkdownFileSize());
            }
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            if (fileSize > limits.maxConfigFileSize()) {
                throw new SecurityException("設定ファイルサイズが制限を超えています: " + fileSize + " > " + limits.maxConfigFileSize());
            }
        }
        
        logger.debug("ファイルサイズ検証成功: {} ({} bytes)", fileName, fileSize);
    }
    
    /**
     * コンテンツの検証
     */
    public void validateContent(String content) throws SecurityException {
        if (content == null) {
            return;
        }
        
        // スクリプトタグの検証
        if (SCRIPT_PATTERN.matcher(content).find()) {
            throw new SecurityException("危険なスクリプトタグが検出されました");
        }
        
        // iframeタグの検証
        if (IFRAME_PATTERN.matcher(content).find()) {
            throw new SecurityException("iframeタグが検出されました");
        }
        
        // objectタグの検証
        if (OBJECT_PATTERN.matcher(content).find()) {
            throw new SecurityException("objectタグが検出されました");
        }
        
        // embedタグの検証
        if (EMBED_PATTERN.matcher(content).find()) {
            throw new SecurityException("embedタグが検出されました");
        }
        
        // イベントハンドラの検証
        if (EVENT_HANDLER_PATTERN.matcher(content).find()) {
            throw new SecurityException("危険なイベントハンドラが検出されました");
        }
        
        logger.debug("コンテンツ検証成功 ({} 文字)", content.length());
    }
    
    /**
     * フロントマターの検証
     */
    public void validateFrontMatter(String frontMatter) throws SecurityException {
        if (frontMatter == null || frontMatter.isEmpty()) {
            return;
        }
        
        if (frontMatter.length() > limits.maxFrontMatterSize()) {
            throw new SecurityException("フロントマターサイズが制限を超えています: " + frontMatter.length() + " > " + limits.maxFrontMatterSize());
        }
        
        // YAML内の危険なパターンをチェック
        if (frontMatter.contains("!!")) {
            throw new SecurityException("危険なYAMLタグが検出されました");
        }
        
        logger.debug("フロントマター検証成功 ({} 文字)", frontMatter.length());
    }
    
    /**
     * タイトルの検証
     */
    public void validateTitle(String title) throws SecurityException {
        if (title == null || title.isEmpty()) {
            return;
        }
        
        if (title.length() > limits.maxTitleLength()) {
            throw new SecurityException("タイトルが長すぎます: " + title.length() + " > " + limits.maxTitleLength());
        }
        
        // HTMLタグの検証
        if (title.contains("<") || title.contains(">")) {
            throw new SecurityException("タイトルに危険な文字が含まれています");
        }
        
        logger.debug("タイトル検証成功: {}", title);
    }
    
    /**
     * 説明文の検証
     */
    public void validateDescription(String description) throws SecurityException {
        if (description == null || description.isEmpty()) {
            return;
        }
        
        if (description.length() > limits.maxDescriptionLength()) {
            throw new SecurityException("説明文が長すぎます: " + description.length() + " > " + limits.maxDescriptionLength());
        }
        
        // HTMLタグの検証
        if (description.contains("<script") || description.contains("javascript:")) {
            throw new SecurityException("説明文に危険な内容が含まれています");
        }
        
        logger.debug("説明文検証成功 ({} 文字)", description.length());
    }
    
    /**
     * URLの検証
     */
    public void validateUrl(String url) throws SecurityException {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        // 危険なプロトコルの検証
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:") || lowerUrl.startsWith("vbscript:")) {
            throw new SecurityException("危険なURLプロトコルが検出されました: " + url);
        }
        
        // 相対パスの検証
        if (url.contains("..")) {
            throw new SecurityException("危険な相対パスが検出されました: " + url);
        }
        
        logger.debug("URL検証成功: {}", url);
    }
    
    /**
     * HTMLの検証とサニタイゼーション（OWASP HTML Sanitizer使用）
     */
    public String sanitizeHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        try {
            // OWASP HTML Sanitizerを使用して安全なHTMLに変換
            String sanitized = HTML_SANITIZER_POLICY.sanitize(html);

            // 長さ制限をチェック
            if (sanitized.length() > limits.maxDescriptionLength() * 2) {
                logger.warn("サニタイズ後のHTMLが長すぎます: {}", sanitized.length());
                return sanitized.substring(0, limits.maxDescriptionLength() * 2) + "...";
            }

            logger.debug("HTMLサニタイズ完了: {} -> {} 文字", html.length(), sanitized.length());
            return sanitized.trim();

        } catch (Exception e) {
            logger.error("HTMLサニタイズ中にエラーが発生しました", e);
            // フォールバック：すべてのHTMLタグを除去
            return html.replaceAll("<[^>]+>", "").trim();
        }
    }
    
    /**
     * 危険な文字が含まれているかチェック
     */
    private boolean containsDangerousCharacters(String fileName) {
        // 制御文字やnullバイトの検証
        for (char c : fileName.toCharArray()) {
            if (c < 32 || c == 127) {
                return true;
            }
        }
        
        // 危険な文字列の検証
        List<String> dangerousStrings = List.of(
            "CON", "PRN", "AUX", "NUL", // Windows予約名
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        );
        
        String upperFileName = fileName.toUpperCase();
        for (String dangerous : dangerousStrings) {
            if (upperFileName.equals(dangerous) || upperFileName.startsWith(dangerous + ".")) {
                return true;
            }
        }
        
        return false;
    }
}