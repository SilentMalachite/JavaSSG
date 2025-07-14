package com.javassg.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * ライブリロード機能を提供するサービスクラス
 */
public class LiveReloadService {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveReloadService.class);
    
    private final Set<WebSocketSession> connections = new CopyOnWriteArraySet<>();
    private final Map<String, LocalDateTime> lastReloadTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong reloadEvents = new AtomicLong(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long startTime = System.currentTimeMillis();
    
    private WatchService watchService;
    private volatile boolean watching = false;
    private Consumer<Path> fileChangeListener;
    private int throttleDelayMs = 100;
    
    public void addConnection(WebSocketSession session) {
        connections.add(session);
        totalConnections.incrementAndGet();
        logger.debug("WebSocket接続を追加しました: {}", session.getId());
    }
    
    public void removeConnection(WebSocketSession session) {
        connections.remove(session);
        logger.debug("WebSocket接続を削除しました: {}", session.getId());
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
    
    public void broadcastReload() {
        String message = createReloadMessage("*");
        broadcast(message);
        reloadEvents.incrementAndGet();
        lastReloadTimes.put("*", LocalDateTime.now());
        logger.debug("リロードメッセージをブロードキャストしました");
    }
    
    public void broadcastFileChange(String filePath) {
        String message = createReloadMessage(filePath);
        broadcast(message);
        reloadEvents.incrementAndGet();
        lastReloadTimes.put(filePath, LocalDateTime.now());
        logger.debug("ファイル変更メッセージをブロードキャストしました: {}", filePath);
    }
    
    public void broadcastCssReload(String cssPath) {
        String message = createCssReloadMessage(cssPath);
        broadcast(message);
        reloadEvents.incrementAndGet();
        logger.debug("CSS専用リロードメッセージをブロードキャストしました: {}", cssPath);
    }
    
    public void handleMessage(WebSocketSession session, String message) {
        try {
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String command = (String) messageData.get("command");
            
            if ("hello".equals(command)) {
                String response = createHelloResponse();
                session.sendMessage(response);
                messagesSent.incrementAndGet();
            } else {
                logger.warn("未知のコマンド: {}", command);
            }
        } catch (JsonProcessingException e) {
            String errorResponse = createErrorResponse("Invalid message format");
            session.sendMessage(errorResponse);
            messagesSent.incrementAndGet();
            logger.warn("無効なメッセージフォーマット: {}", message);
        }
    }
    
    public void startWatching(Path directory) throws IOException {
        if (watching) {
            stopWatching();
        }
        
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watching = true;
        
        registerDirectoryRecursively(directory);
        
        // テスト環境では簡単なポーリング方式を使用
        Thread watchThread = new Thread(() -> {
            Map<Path, Long> lastModified = new HashMap<>();
            
            try {
                while (watching) {
                    // ディレクトリ内のファイルをチェック
                    try {
                        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                String fileName = file.getFileName().toString();
                                
                                // 隠しファイルをスキップ
                                if (fileName.startsWith(".")) {
                                    return FileVisitResult.CONTINUE;
                                }
                                
                                long currentModified = attrs.lastModifiedTime().toMillis();
                                Long lastTime = lastModified.get(file);
                                
                                if (lastTime == null || currentModified > lastTime) {
                                    // スロットリング
                                    if (lastTime != null && (currentModified - lastTime) < throttleDelayMs) {
                                        return FileVisitResult.CONTINUE;
                                    }
                                    
                                    lastModified.put(file, currentModified);
                                    
                                    if (fileChangeListener != null && lastTime != null) {
                                        fileChangeListener.accept(file);
                                    }
                                    
                                    logger.debug("ファイル変更を検知: {}", file);
                                }
                                
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        logger.error("ファイル監視エラー", e);
                    }
                    
                    Thread.sleep(throttleDelayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("ファイル監視が中断されました");
            } catch (Exception e) {
                logger.error("ファイル監視エラー", e);
            }
        });
        
        watchThread.setDaemon(true);
        watchThread.start();
        
        logger.info("ファイル監視を開始しました: {}", directory);
    }
    
    public void stopWatching() {
        this.watching = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            logger.error("ファイル監視停止エラー", e);
        }
        logger.info("ファイル監視を停止しました");
    }
    
    public boolean isWatching() {
        return watching;
    }
    
    public void setFileChangeListener(Consumer<Path> listener) {
        this.fileChangeListener = listener;
    }
    
    public void setThrottleDelayMs(int throttleDelayMs) {
        this.throttleDelayMs = throttleDelayMs;
    }
    
    public LiveReloadStatistics getStatistics() {
        return new LiveReloadStatistics(
            connections.size(),
            (int) totalConnections.get(),
            messagesSent.get(),
            reloadEvents.get(),
            System.currentTimeMillis() - startTime,
            lastReloadTimes.values().stream()
                .map(t -> t.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .max(Long::compareTo)
                .orElse(0L)
        );
    }
    
    public void cleanupConnections() {
        List<WebSocketSession> toRemove = new ArrayList<>();
        for (WebSocketSession session : connections) {
            if (session.isClosed()) {
                toRemove.add(session);
            }
        }
        for (WebSocketSession session : toRemove) {
            connections.remove(session);
        }
    }
    
    private void broadcast(String message) {
        List<WebSocketSession> toRemove = new ArrayList<>();
        for (WebSocketSession session : connections) {
            if (session.isClosed()) {
                toRemove.add(session);
            } else {
                session.sendMessage(message);
                messagesSent.incrementAndGet();
            }
        }
        // 切断されたセッションを削除
        for (WebSocketSession session : toRemove) {
            connections.remove(session);
        }
    }
    
    private String createReloadMessage(String path) {
        Map<String, Object> message = new HashMap<>();
        message.put("command", "reload");
        message.put("path", path);
        message.put("liveCSS", false);
        
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("リロードメッセージの作成に失敗", e);
            return "{\"command\":\"reload\",\"path\":\"*\"}";
        }
    }
    
    private String createCssReloadMessage(String cssPath) {
        Map<String, Object> message = new HashMap<>();
        message.put("command", "reload");
        message.put("path", cssPath);
        message.put("liveCSS", true);
        
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("CSSリロードメッセージの作成に失敗", e);
            return String.format("{\"command\":\"reload\",\"path\":\"%s\",\"liveCSS\":true}", cssPath);
        }
    }
    
    private String createHelloResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("command", "hello");
        response.put("protocols", List.of("http://livereload.com/protocols/official-7"));
        response.put("serverName", "JavaSSG");
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            logger.error("Helloレスポンスの作成に失敗", e);
            return "{\"command\":\"hello\",\"protocols\":[\"http://livereload.com/protocols/official-7\"],\"serverName\":\"JavaSSG\"}";
        }
    }
    
    private String createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("command", "error");
        response.put("message", errorMessage);
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            logger.error("エラーレスポンスの作成に失敗", e);
            return String.format("{\"command\":\"error\",\"message\":\"%s\"}", errorMessage);
        }
    }
    
    private void registerDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}