package com.javassg.model;

public record ServerConfig(int port, boolean liveReload) {
    public ServerConfig {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("ポート番号は1以上65535以下である必要があります");
        }
    }
}