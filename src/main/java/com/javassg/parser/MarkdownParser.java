package com.javassg.parser;

import com.javassg.model.Page;
import com.javassg.model.Post;
import com.javassg.model.SecurityLimits;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MarkdownParser {
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile("(\\.\\./|\\.\\.\\\\|[<>:\"|?*])");
    private static final SecurityLimits DEFAULT_LIMITS = SecurityLimits.defaultLimits();
    
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final Yaml yamlParser;
    private final SecurityLimits securityLimits;
    
    public MarkdownParser() {
        this(DEFAULT_LIMITS);
    }
    
    public MarkdownParser(SecurityLimits securityLimits) {
        this.securityLimits = securityLimits;
        
        var extensions = List.of(YamlFrontMatterExtension.create());
        this.markdownParser = Parser.builder()
            .extensions(extensions)
            .build();
        this.htmlRenderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build();
        this.yamlParser = new Yaml();
    }
    
    public Page parseFile(Path filePath) {
        validateFilename(filePath.getFileName().toString());
        
        try {
            var content = Files.readString(filePath);
            var lastModified = Files.getLastModifiedTime(filePath).toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
            
            return parseContentWithModificationTime(filePath.getFileName().toString(), content, lastModified);
        } catch (Exception e) {
            throw new RuntimeException("ファイルの読み込みに失敗しました: " + filePath, e);
        }
    }
    
    public Page parseContent(String filename, String content) {
        return parseContentWithModificationTime(filename, content, LocalDateTime.now());
    }
    
    public Page parsePage(Path filePath) {
        return parseFile(filePath);
    }
    
    public Post parsePost(Path filePath) {
        Page page = parseFile(filePath);
        return Post.fromPage(page);
    }
    
    private Page parseContentWithModificationTime(String filename, String content, LocalDateTime lastModified) {
        validateFilename(filename);
        validateContentSize(content);
        
        if (content == null || content.trim().isEmpty()) {
            return createEmptyPage(filename, lastModified);
        }
        
        var document = markdownParser.parse(content);
        var frontMatter = extractFrontMatter(document, content);
        var rawContent = extractRawContent(content);
        var renderedContent = htmlRenderer.render(document);
        var slug = generateSlug(filename);
        
        return new Page(filename, slug, frontMatter, rawContent, renderedContent, lastModified);
    }
    
    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("ファイル名は空にできません");
        }
        
        if (filename.length() > securityLimits.maxFilenameLength()) {
            throw new IllegalArgumentException("ファイル名が長すぎます");
        }
        
        if (INVALID_FILENAME_PATTERN.matcher(filename).find()) {
            throw new IllegalArgumentException("無効なファイル名です: " + filename);
        }
    }
    
    private void validateContentSize(String content) {
        if (content != null && content.length() > securityLimits.maxMarkdownFileSize()) {
            throw new IllegalArgumentException("ファイルサイズが制限を超えています");
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFrontMatter(Node document, String content) {
        // まず手動でフロントマターを抽出してみる
        return extractFrontMatterManually(content);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFrontMatterManually(String content) {
        try {
            if (content == null || !content.startsWith("---\n")) {
                return Map.of();
            }
            
            var lines = content.split("\n");
            if (lines.length < 3) {
                return Map.of();
            }
            
            // 最初の行が"---"でない場合は、フロントマターなし
            if (!lines[0].equals("---")) {
                return Map.of();
            }
            
            // 2行目以降で次の"---"を探す
            int endIndex = -1;
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].equals("---")) {
                    endIndex = i;
                    break;
                }
            }
            
            if (endIndex == -1) {
                return Map.of();
            }
            
            // フロントマター部分を抽出（1行目から endIndex-1 行目まで）
            var yamlLines = new String[endIndex - 1];
            System.arraycopy(lines, 1, yamlLines, 0, endIndex - 1);
            var yamlContent = String.join("\n", yamlLines);
            
            // フロントマターのサイズを検証
            if (yamlContent.length() > securityLimits.maxFrontMatterSize()) {
                throw new IllegalArgumentException("フロントマターのサイズが制限を超えています: " + yamlContent.length() + " bytes");
            }
            
            // YAMLをパース
            Object parsed = yamlParser.load(yamlContent);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return Map.of();
        } catch (IllegalArgumentException e) {
            // サイズ制限エラーは再スロー
            throw e;
        } catch (Exception e) {
            // その他のフロントマター解析エラーは空のMapを返す
            return Map.of();
        }
    }
    
    private String extractRawContent(String content) {
        // フロントマターを除去した生のMarkdownコンテンツを抽出
        var lines = content.split("\n");
        boolean inFrontMatter = false;
        boolean frontMatterEnded = false;
        StringBuilder rawContent = new StringBuilder();
        
        for (String line : lines) {
            if (line.trim().equals("---")) {
                if (!inFrontMatter) {
                    inFrontMatter = true;
                    continue;
                } else {
                    frontMatterEnded = true;
                    continue;
                }
            }
            
            if (!inFrontMatter || frontMatterEnded) {
                rawContent.append(line).append("\n");
            }
        }
        
        return rawContent.toString().trim();
    }
    
    private String generateSlug(String filename) {
        var nameWithoutExt = filename.replaceFirst("\\.[^.]+$", "");
        return nameWithoutExt.toLowerCase()
            .replaceAll("_", "-")  // アンダースコアをハイフンに変換
            .replaceAll("[^a-z0-9\\-]", "-")  // 許可されていない文字をハイフンに
            .replaceAll("-+", "-")  // 連続するハイフンを1つに
            .replaceAll("^-|-$", "");  // 先頭と末尾のハイフンを削除
    }
    
    private Page createEmptyPage(String filename, LocalDateTime lastModified) {
        var slug = generateSlug(filename);
        return new Page(filename, slug, Map.of(), "", "", lastModified);
    }
}