package com.javassg.model;

public record BuildConfig(
    String contentDirectory,
    String outputDirectory,
    String staticDirectory,
    String templatesDirectory
) {
    public BuildConfig {
        if (contentDirectory == null || contentDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("コンテンツディレクトリは空にできません");
        }
        if (outputDirectory == null || outputDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("出力ディレクトリは空にできません");
        }
    }
}