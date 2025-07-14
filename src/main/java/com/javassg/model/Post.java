package com.javassg.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record Post(
    String filename,
    String slug,
    Map<String, Object> frontMatter,
    String rawContent,
    String renderedContent,
    LocalDateTime lastModified,
    LocalDateTime publishedAt,
    List<String> categories,
    List<String> tags
) {
    public Post {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("ファイル名は空にできません");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("公開日時は null にできません");
        }
        if (categories == null) {
            throw new IllegalArgumentException("カテゴリーは null にできません");
        }
        if (tags == null) {
            throw new IllegalArgumentException("タグは null にできません");
        }
    }
    
    public String title() {
        var title = frontMatter.get("title");
        if (title instanceof String str && !str.trim().isEmpty()) {
            return str;
        }
        return generateTitleFromFilename();
    }
    
    public String description() {
        var desc = frontMatter.get("description");
        return desc instanceof String str ? str : "";
    }
    
    public boolean isPublished() {
        var draft = frontMatter.get("draft");
        if (draft instanceof Boolean bool && bool) {
            return false;
        }
        
        // 未来の日付の場合は下書き扱い
        return !publishedAt.isAfter(LocalDateTime.now());
    }
    
    public String getExcerpt(int maxLength) {
        if (rawContent == null || rawContent.length() <= maxLength) {
            return rawContent;
        }
        
        var excerpt = rawContent.substring(0, maxLength);
        var lastSpace = excerpt.lastIndexOf(' ');
        if (lastSpace > 0) {
            excerpt = excerpt.substring(0, lastSpace);
        }
        var result = excerpt + "...";
        
        // 最大長を超える場合は、さらに短縮
        if (result.length() > maxLength) {
            return rawContent.substring(0, Math.max(0, maxLength - 3)) + "...";
        }
        return result;
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
    
    public static Post fromPage(Page page) {
        var publishedAt = parsePublishedAt(page.frontMatter());
        var categories = parseCategories(page.frontMatter());
        var tags = parseTags(page.frontMatter());
        
        return new Post(
            page.filename(),
            page.slug(),
            page.frontMatter(),
            page.rawContent(),
            page.renderedContent(),
            page.lastModified(),
            publishedAt,
            categories,
            tags
        );
    }
    
    private static LocalDateTime parsePublishedAt(Map<String, Object> frontMatter) {
        var date = frontMatter.get("date");
        if (date instanceof String dateStr) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // フォーマットが異なる場合の処理
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }
    
    @SuppressWarnings("unchecked")
    private static List<String> parseCategories(Map<String, Object> frontMatter) {
        var categories = frontMatter.get("categories");
        if (categories instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        } else if (categories instanceof String str) {
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }
    
    @SuppressWarnings("unchecked")
    private static List<String> parseTags(Map<String, Object> frontMatter) {
        var tags = frontMatter.get("tags");
        if (tags instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        } else if (tags instanceof String str) {
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }
}