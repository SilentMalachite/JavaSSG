package com.javassg.server;

import java.time.LocalDateTime;

/**
 * HTTPリクエストログエントリを表すレコードクラス
 */
public record RequestLogEntry(
    LocalDateTime timestamp,
    String method,
    String path,
    int statusCode,
    long responseTimeMs,
    long contentLength,
    String userAgent,
    String remoteAddress
) {
    
    public static RequestLogEntry create(String method, String path, int statusCode, 
                                       long responseTimeMs, long contentLength) {
        return new RequestLogEntry(
            LocalDateTime.now(),
            method,
            path,
            statusCode,
            responseTimeMs,
            contentLength,
            null,
            null
        );
    }
    
    public static RequestLogEntry create(String method, String path, int statusCode, 
                                       long responseTimeMs, long contentLength, 
                                       String userAgent, String remoteAddress) {
        return new RequestLogEntry(
            LocalDateTime.now(),
            method,
            path,
            statusCode,
            responseTimeMs,
            contentLength,
            userAgent,
            remoteAddress
        );
    }
    
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 400;
    }
    
    public boolean isError() {
        return statusCode >= 400;
    }
    
    public boolean isServerError() {
        return statusCode >= 500;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s %d %dms %db", 
                           timestamp, method, path, statusCode, responseTimeMs, contentLength);
    }
}