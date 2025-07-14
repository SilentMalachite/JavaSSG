package com.javassg.model;

public record SecurityLimits(
    long maxMarkdownFileSize,
    long maxConfigFileSize,
    long maxFrontMatterSize,
    int maxFilenameLength,
    int maxTitleLength,
    int maxDescriptionLength
) {
    public static SecurityLimits defaultLimits() {
        return new SecurityLimits(
            10 * 1024 * 1024,  // 10MB
            1024 * 1024,       // 1MB
            100 * 1024,        // 100KB
            255,
            200,
            500
        );
    }
}