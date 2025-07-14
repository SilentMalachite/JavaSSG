package com.javassg.server;

/**
 * WebSocketセッションのインターフェース
 */
public interface WebSocketSession {
    
    String getId();
    
    void sendMessage(String message);
    
    boolean isClosed();
    
    boolean isConnected();
    
    void close();
}