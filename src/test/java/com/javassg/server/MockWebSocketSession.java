package com.javassg.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocketセッションのテスト用モッククラス
 */
public class MockWebSocketSession implements WebSocketSession {
    
    private String id;
    private final List<String> receivedMessages = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private boolean connected = true;
    
    public MockWebSocketSession() {
        this.id = "mock-session-" + System.currentTimeMillis();
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public void sendMessage(String message) {
        if (!closed.get() && connected) {
            receivedMessages.add(message);
        }
    }
    
    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
    
    public void close() {
        closed.set(true);
        connected = false;
    }
    
    public boolean isClosed() {
        return closed.get();
    }
    
    public boolean isConnected() {
        return connected && !closed.get();
    }
    
    public void clearMessages() {
        receivedMessages.clear();
    }
    
    public int getMessageCount() {
        return receivedMessages.size();
    }
    
    public void simulateDisconnect() {
        close();
    }
    
    @Override
    public String toString() {
        return "MockWebSocketSession{" +
               "id='" + id + '\'' +
               ", connected=" + connected +
               ", closed=" + closed.get() +
               ", messageCount=" + receivedMessages.size() +
               '}';
    }
}