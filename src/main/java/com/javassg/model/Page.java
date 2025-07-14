package com.javassg.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

public record Page(
    String filename,
    String slug,
    Map<String, Object> frontMatter,
    String rawContent,
    String renderedContent,
    LocalDateTime lastModified
) {
    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");
    
    public Page {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("ファイル名は空にできません");
        }
        if (rawContent == null) {
            throw new IllegalArgumentException("コンテンツは null にできません");
        }
        if (renderedContent == null) {
            throw new IllegalArgumentException("レンダリング済みコンテンツは null にできません");
        }
        if (lastModified == null) {
            throw new IllegalArgumentException("最終更新日時は null にできません");
        }
    }
    
    public String title() {
        var title = getFrontMatterValue("title");
        if (title instanceof String str && !str.trim().isEmpty()) {
            return str;
        }
        return generateTitleFromFilename();
    }
    
    public String description() {
        var desc = getFrontMatterValue("description");
        return desc instanceof String str ? str : "";
    }
    
    public Object getFrontMatterValue(String key) {
        return frontMatter.get(key);
    }
    
    public boolean isPublished() {
        var draft = getFrontMatterValue("draft");
        if (draft instanceof Boolean bool) {
            return !bool;
        }
        return true; // デフォルトは公開
    }
    
    private String generateTitleFromFilename() {
        var nameWithoutExt = filename.replaceFirst("\\.[^.]+$", "");
        var withSpaces = nameWithoutExt.replaceAll("[-_]", " ");
        // 各単語の最初の文字を大文字にする
        return java.util.Arrays.stream(withSpaces.split(" "))
                .map(word -> word.isEmpty() ? word : 
                     Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String filename;
        private String slug;
        private Map<String, Object> frontMatter = Map.of();
        private String rawContent;
        private String renderedContent;
        private LocalDateTime lastModified;
        
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }
        
        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }
        
        public Builder frontMatter(Map<String, Object> frontMatter) {
            this.frontMatter = frontMatter;
            return this;
        }
        
        public Builder rawContent(String rawContent) {
            this.rawContent = rawContent;
            return this;
        }
        
        public Builder renderedContent(String renderedContent) {
            this.renderedContent = renderedContent;
            return this;
        }
        
        public Builder lastModified(LocalDateTime lastModified) {
            this.lastModified = lastModified;
            return this;
        }
        
        public Page build() {
            if (slug == null) {
                slug = generateSlugFromFilename(filename);
            }
            return new Page(filename, slug, frontMatter, rawContent, renderedContent, lastModified);
        }
        
        private String generateSlugFromFilename(String filename) {
            if (filename == null) return "";
            
            var nameWithoutExt = filename.replaceFirst("\\.[^.]+$", "");
            
            // 日本語をローマ字に変換（簡易版）
            var romanized = nameWithoutExt
                .replace("テスト", "tesuto")
                .replace("ページ", "peeji")
                .replace(" ", "-")
                .toLowerCase();
            
            // 英数字とハイフン以外を削除
            return SLUG_PATTERN.matcher(romanized).replaceAll("");
        }
    }
}