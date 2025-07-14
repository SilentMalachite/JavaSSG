package com.javassg.server;

/**
 * ライブリロードサービスの統計情報を保持するレコードクラス
 */
public record LiveReloadStatistics(
    int activeConnections,
    int totalConnections,
    long messagesSent,
    long reloadEvents,
    long uptimeMs,
    long lastReloadTime
) {
    
    public static LiveReloadStatistics empty() {
        return new LiveReloadStatistics(0, 0, 0, 0, 0, 0);
    }
    
    public LiveReloadStatistics withActiveConnections(int activeConnections) {
        return new LiveReloadStatistics(
            activeConnections,
            this.totalConnections,
            this.messagesSent,
            this.reloadEvents,
            this.uptimeMs,
            this.lastReloadTime
        );
    }
    
    public LiveReloadStatistics withMessageSent() {
        return new LiveReloadStatistics(
            this.activeConnections,
            this.totalConnections,
            this.messagesSent + 1,
            this.reloadEvents,
            this.uptimeMs,
            this.lastReloadTime
        );
    }
    
    public LiveReloadStatistics withReloadEvent() {
        return new LiveReloadStatistics(
            this.activeConnections,
            this.totalConnections,
            this.messagesSent,
            this.reloadEvents + 1,
            this.uptimeMs,
            System.currentTimeMillis()
        );
    }
}