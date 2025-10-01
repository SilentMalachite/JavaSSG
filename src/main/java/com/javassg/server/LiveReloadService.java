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
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong reloadEvents = new AtomicLong(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long startTime = System.currentTimeMillis();
    
    private WatchService watchService;
    private volatile boolean watching = false;
    private Consumer<Path> fileChangeListener;
    private int throttleDelayMs = 100;
    
    public void addClient(WebSocketSession session) {
        connections.add(session);
        sessionLastActivity.put(session.getId(), System.currentTimeMillis());
        totalConnections.incrementAndGet();
        logger.debug("WebSocketクライアントを追加しました: {}", session.getId());
    }
    
    public void removeConnection(WebSocketSession session) {
        connections.remove(session);
        sessionLastActivity.remove(session.getId());
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
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String command = (String) messageData.get("command");
            
            if ("hello".equals(command)) {
                String response = createHelloResponse();
                session.sendMessage(response);
                messagesSent.incrementAndGet();
                // メッセージ受信時もアクティビティを更新
                sessionLastActivity.put(session.getId(), System.currentTimeMillis());
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

        // 高性能なWatchServiceベースの監視を使用
        Thread watchThread = new Thread(() -> {
            try {
                // ファイル変更のキャッシュとスロットリング
                Map<Path, Long> lastEventTime = new ConcurrentHashMap<>();
                Set<Path> pendingEvents = ConcurrentHashMap.newKeySet();

                while (watching) {
                    try {
                        WatchKey key = watchService.poll(1000, java.util.concurrent.TimeUnit.MILLISECONDS);

                        if (key != null) {
                            List<WatchEvent<?>> events = key.pollEvents();
                            Path watchedDir = (Path) key.watchable();

                            for (WatchEvent<?> event : events) {
                                WatchEvent.Kind<?> kind = event.kind();

                                if (kind == StandardWatchEventKinds.OVERFLOW) {
                                    // イベントオーバーフロー：フルスキャンを実行
                                    handleFullScan(directory, lastEventTime, pendingEvents);
                                    continue;
                                }

                                Path changedFile = watchedDir.resolve((Path) event.context());

                                // 隠しファイルを無視
                                if (changedFile.getFileName().toString().startsWith(".")) {
                                    continue;
                                }

                                // ディレクトリの場合は登録
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE &&
                                    Files.isDirectory(changedFile)) {
                                    registerDirectoryRecursively(changedFile);
                                }

                                // スロットリングと重複排除
                                long currentTime = System.currentTimeMillis();
                                Long lastTime = lastEventTime.get(changedFile);

                                if (lastTime == null || (currentTime - lastTime) > throttleDelayMs) {
                                    lastEventTime.put(changedFile, currentTime);

                                    // 非同期処理でファイル変更を通知
                                    if (fileChangeListener != null && lastTime != null) {
                                        pendingEvents.add(changedFile);

                                        // 少し遅延してバッチ処理
                                        java.util.concurrent.ScheduledExecutorService scheduler =
                                            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                                        scheduler.schedule(() -> {
                                            Set<Path> eventsToProcess = new HashSet<>(pendingEvents);
                                            pendingEvents.clear();

                                            for (Path file : eventsToProcess) {
                                                try {
                                                    fileChangeListener.accept(file);
                                                    logger.debug("ファイル変更を検知: {}", file);
                                                } catch (Exception e) {
                                                    logger.error("ファイル変更処理エラー: {}", file, e);
                                                }
                                            }
                                        }, throttleDelayMs / 2, java.util.concurrent.TimeUnit.MILLISECONDS);
                                        scheduler.shutdown();
                                    }
                                }
                            }

                            // WatchKeyをリセットして監視を継続
                            boolean valid = key.reset();
                            if (!valid) {
                                logger.warn("WatchKeyが無効になりました: {}", watchedDir);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("ファイル監視で致命的なエラーが発生しました", e);
            } finally {
                logger.info("ファイル監視が終了しました");
            }
        });

        watchThread.setDaemon(true);
        watchThread.setName("LiveReloadFileWatcher");
        watchThread.start();

        logger.info("WatchServiceベースのファイル監視を開始しました: {}", directory);
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
        int closedCount = 0;

        for (WebSocketSession session : connections) {
            try {
                if (session.isClosed()) {
                    toRemove.add(session);
                    closedCount++;

                    // セッションのリソースを確実に解放
                    try {
                        session.close();
                    } catch (Exception e) {
                        logger.debug("セッションクローズ時にエラー: {}", e.getMessage());
                    }
                } else {
                    // タイムアウトチェック（長時間無活動な接続を削除）
                    long lastActivityTime = getLastActivityTime(session);
                    long currentTime = System.currentTimeMillis();
                    long inactiveTime = currentTime - lastActivityTime;

                    // 30分以上無活動な接続をクローズ
                    if (inactiveTime > 30 * 60 * 1000) {
                        logger.debug("タイムアウトにより接続をクローズ: {}", session.getId());
                        try {
                            session.close();
                            toRemove.add(session);
                            closedCount++;
                        } catch (Exception e) {
                            logger.debug("タイムアウトセッションのクローズエラー: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("接続状態確認中にエラーが発生しました: {}", session.getId(), e);
                toRemove.add(session); // 異常な接続は削除対象
            }
        }

        // 切断された接続を削除
        for (WebSocketSession session : toRemove) {
            connections.remove(session);
        }

        if (closedCount > 0) {
            logger.debug("{}個のWebSocket接続をクリーンアップしました", closedCount);
        }
    }
    
    private void broadcast(String message) {
        List<WebSocketSession> toRemove = new ArrayList<>();
        for (WebSocketSession session : connections) {
            try {
                if (session.isClosed()) {
                    toRemove.add(session);
                } else {
                    session.sendMessage(message);
                    messagesSent.incrementAndGet();
                    // アクティビティタイムスタンプを更新
                    sessionLastActivity.put(session.getId(), System.currentTimeMillis());
                }
            } catch (Exception e) {
                logger.error("メッセージ送信エラー: {}", session.getId(), e);
                toRemove.add(session); // 送信失敗した接続は削除
            }
        }
        // 切断された接続を削除
        for (WebSocketSession session : toRemove) {
            connections.remove(session);
            sessionLastActivity.remove(session.getId());
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

    /**
     * イベントオーバーフロー時のフルスキャン処理
     */
    private void handleFullScan(Path directory, Map<Path, Long> lastEventTime, Set<Path> pendingEvents) {
        try {
            logger.debug("イベントオーバーフローによりフルスキャンを実行します: {}", directory);

            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        String fileName = file.getFileName().toString();

                        // 隠しファイルをスキップ
                        if (fileName.startsWith(".")) {
                            return FileVisitResult.CONTINUE;
                        }

                        long currentTime = System.currentTimeMillis();
                        Long lastTime = lastEventTime.get(file);

                        // ファイルが変更されているかチェック
                        if (lastTime == null || attrs.lastModifiedTime().toMillis() > lastTime) {
                            lastEventTime.put(file, currentTime);

                            if (fileChangeListener != null && lastTime != null) {
                                pendingEvents.add(file);
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    } catch (Exception e) {
                        logger.error("フルスキャン中のファイル処理エラー: {}", file, e);
                        return FileVisitResult.CONTINUE;
                    }
                }
            });

            // 保留中のイベントを処理
            if (!pendingEvents.isEmpty() && fileChangeListener != null) {
                Set<Path> eventsToProcess = new HashSet<>(pendingEvents);
                pendingEvents.clear();

                for (Path file : eventsToProcess) {
                    try {
                        fileChangeListener.accept(file);
                        logger.debug("フルスキャンでファイル変更を検知: {}", file);
                    } catch (Exception e) {
                        logger.error("フルスキャンでのファイル変更処理エラー: {}", file, e);
                    }
                }
            }

        } catch (IOException e) {
            logger.error("フルスキャン実行中にエラーが発生しました", e);
        }
    }

    /**
     * セッションの最終アクティビティ時刻を取得
     */
    private long getLastActivityTime(WebSocketSession session) {
        return sessionLastActivity.getOrDefault(session.getId(), System.currentTimeMillis());
    }
}