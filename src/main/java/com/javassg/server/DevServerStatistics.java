package com.javassg.server;

/**
 * 開発サーバーの統計情報を保持するレコードクラス
 */
public record DevServerStatistics(
    long totalRequests,
    long successfulRequests,
    long errorRequests,
    int activeConnections,
    long uptimeMs,
    long averageResponseTimeMs,
    long totalBytesServed
) {
    
    public static DevServerStatistics empty() {
        return new DevServerStatistics(0, 0, 0, 0, 0, 0, 0);
    }
    
    public DevServerStatistics withRequest(boolean successful, long responseTimeMs, long bytesServed) {
        return new DevServerStatistics(
            this.totalRequests + 1,
            successful ? this.successfulRequests + 1 : this.successfulRequests,
            successful ? this.errorRequests : this.errorRequests + 1,
            this.activeConnections,
            this.uptimeMs,
            calculateNewAverageResponseTime(responseTimeMs),
            this.totalBytesServed + bytesServed
        );
    }
    
    public DevServerStatistics withActiveConnections(int activeConnections) {
        return new DevServerStatistics(
            this.totalRequests,
            this.successfulRequests,
            this.errorRequests,
            activeConnections,
            this.uptimeMs,
            this.averageResponseTimeMs,
            this.totalBytesServed
        );
    }
    
    public DevServerStatistics withUptime(long uptimeMs) {
        return new DevServerStatistics(
            this.totalRequests,
            this.successfulRequests,
            this.errorRequests,
            this.activeConnections,
            uptimeMs,
            this.averageResponseTimeMs,
            this.totalBytesServed
        );
    }
    
    private long calculateNewAverageResponseTime(long newResponseTime) {
        if (totalRequests == 0) {
            return newResponseTime;
        }
        return (averageResponseTimeMs * totalRequests + newResponseTime) / (totalRequests + 1);
    }
    
    public double getSuccessRate() {
        return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
    }
    
    public double getErrorRate() {
        return totalRequests > 0 ? (double) errorRequests / totalRequests : 0.0;
    }
}