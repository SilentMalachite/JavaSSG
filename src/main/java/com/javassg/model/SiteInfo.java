package com.javassg.model;

public record SiteInfo(
    String title,
    String description,
    String url,
    String language,
    Author author
) {
    public SiteInfo {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("タイトルは空にできません");
        }
        if (url == null || !isValidUrl(url)) {
            throw new IllegalArgumentException("有効なURLを指定してください");
        }
    }
    
    private static boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }
}