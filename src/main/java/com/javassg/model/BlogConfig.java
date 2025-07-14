package com.javassg.model;

public record BlogConfig(
    int postsPerPage,
    boolean generateArchive,
    boolean generateCategories,
    boolean generateTags
) {
    public BlogConfig {
        if (postsPerPage < 1) {
            throw new IllegalArgumentException("ページあたりの記事数は1以上である必要があります");
        }
    }
}