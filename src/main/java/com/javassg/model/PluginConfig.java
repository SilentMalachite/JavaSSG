package com.javassg.model;

import java.util.Map;

public record PluginConfig(
    String name,
    boolean enabled,
    Map<String, Object> settings
) {
    public PluginConfig {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("プラグイン名は空にできません");
        }
    }
}