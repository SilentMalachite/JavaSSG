package com.javassg.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * WebSocketセッションの簡易実装
 */
public class SimpleWebSocketSession implements WebSocketSession {
    
    private final String id;
    private final HttpExchange exchange;
    private volatile boolean closed = false;
    private volatile boolean connected = true;
    
    public SimpleWebSocketSession(HttpExchange exchange, String id) {
        this.exchange = exchange;
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void sendMessage(String message) {
        if (closed || !connected) {
            return;
        }
        
        try {
            // 簡易的なWebSocketメッセージ送信
            // 実際にはWebSocketプロトコルに従った実装が必要
            String response = "data: " + message + "\n\n";
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().flush();
        } catch (IOException e) {
            close();
        }
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public boolean isConnected() {
        return connected && !closed;
    }
    
    @Override
    public void close() {
        closed = true;
        connected = false;
        try {
            if (exchange.getResponseBody() != null) {
                exchange.getResponseBody().close();
            }
        } catch (IOException e) {
            // 無視
        }
    }
}