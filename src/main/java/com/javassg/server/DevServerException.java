package com.javassg.server;

/**
 * 開発サーバーの例外クラス
 */
public class DevServerException extends RuntimeException {
    
    public DevServerException(String message) {
        super(message);
    }
    
    public DevServerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DevServerException(Throwable cause) {
        super(cause);
    }
    
    public static DevServerException portInUse(int port) {
        return new DevServerException("ポートが既に使用されています: " + port);
    }
    
    public static DevServerException serverStartFailed(String reason) {
        return new DevServerException("サーバーの起動に失敗しました: " + reason);
    }
    
    public static DevServerException serverStartFailed(Throwable cause) {
        return new DevServerException("サーバーの起動に失敗しました", cause);
    }
    
    public static DevServerException configurationError(String message) {
        return new DevServerException("設定エラー: " + message);
    }
    
    public static DevServerException fileSystemError(String message) {
        return new DevServerException("ファイルシステムエラー: " + message);
    }
    
    public static DevServerException fileSystemError(String message, Throwable cause) {
        return new DevServerException("ファイルシステムエラー: " + message, cause);
    }
}