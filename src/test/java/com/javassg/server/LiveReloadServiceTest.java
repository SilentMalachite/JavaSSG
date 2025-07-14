package com.javassg.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LiveReloadServiceTest {

    private LiveReloadService liveReloadService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        liveReloadService = new LiveReloadService();
    }

    @Test
    void shouldAddAndRemoveWebSocketConnections() {
        MockWebSocketSession session1 = new MockWebSocketSession();
        MockWebSocketSession session2 = new MockWebSocketSession();
        
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(0);
        
        liveReloadService.addConnection(session1);
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(1);
        
        liveReloadService.addConnection(session2);
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(2);
        
        liveReloadService.removeConnection(session1);
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(1);
        
        liveReloadService.removeConnection(session2);
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(0);
    }

    @Test
    void shouldBroadcastReloadMessage() {
        MockWebSocketSession session1 = new MockWebSocketSession();
        MockWebSocketSession session2 = new MockWebSocketSession();
        MockWebSocketSession session3 = new MockWebSocketSession();
        
        liveReloadService.addConnection(session1);
        liveReloadService.addConnection(session2);
        liveReloadService.addConnection(session3);
        
        liveReloadService.broadcastReload();
        
        assertThat(session1.getReceivedMessages()).hasSize(1);
        assertThat(session2.getReceivedMessages()).hasSize(1);
        assertThat(session3.getReceivedMessages()).hasSize(1);
        
        String message = session1.getReceivedMessages().get(0);
        assertThat(message).contains("\"command\":\"reload\"");
        assertThat(message).contains("\"path\":\"*\"");
    }

    @Test
    void shouldBroadcastSpecificFileChange() {
        MockWebSocketSession session = new MockWebSocketSession();
        liveReloadService.addConnection(session);
        
        liveReloadService.broadcastFileChange("/css/style.css");
        
        assertThat(session.getReceivedMessages()).hasSize(1);
        String message = session.getReceivedMessages().get(0);
        assertThat(message).contains("\"command\":\"reload\"");
        assertThat(message).contains("\"path\":\"/css/style.css\"");
    }

    @Test
    void shouldHandleClosedConnections() {
        MockWebSocketSession session1 = new MockWebSocketSession();
        MockWebSocketSession session2 = new MockWebSocketSession();
        
        liveReloadService.addConnection(session1);
        liveReloadService.addConnection(session2);
        
        // 一つの接続を閉じる
        session1.close();
        
        liveReloadService.broadcastReload();
        
        // 閉じた接続にはメッセージが送信されず、アクティブな接続のみに送信される
        assertThat(session1.getReceivedMessages()).isEmpty();
        assertThat(session2.getReceivedMessages()).hasSize(1);
        
        // 閉じた接続は自動的に削除される
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldStartAndStopFileWatching() throws IOException, InterruptedException {
        Path watchDir = tempDir.resolve("watch");
        Files.createDirectories(watchDir);
        
        AtomicInteger changeCount = new AtomicInteger(0);
        
        liveReloadService.setFileChangeListener(path -> changeCount.incrementAndGet());
        liveReloadService.startWatching(watchDir);
        
        assertThat(liveReloadService.isWatching()).isTrue();
        
        // ファイルを作成
        Path testFile = watchDir.resolve("test.txt");
        Files.writeString(testFile, "initial content");
        
        // 変更を待機
        Thread.sleep(500);
        
        // ファイルを変更
        Files.writeString(testFile, "updated content", StandardOpenOption.TRUNCATE_EXISTING);
        
        // 変更を待機
        Thread.sleep(500);
        
        liveReloadService.stopWatching();
        
        assertThat(liveReloadService.isWatching()).isFalse();
        assertThat(changeCount.get()).isGreaterThan(0);
    }

    @Test
    void shouldIgnoreHiddenFiles() throws IOException, InterruptedException {
        Path watchDir = tempDir.resolve("watch");
        Files.createDirectories(watchDir);
        
        // 初期ファイルを作成（監視開始前）
        Path hiddenFile = watchDir.resolve(".hidden");
        Path visibleFile = watchDir.resolve("visible.txt");
        Files.writeString(hiddenFile, "initial content");
        Files.writeString(visibleFile, "initial content");
        
        AtomicInteger changeCount = new AtomicInteger(0);
        
        liveReloadService.setFileChangeListener(path -> {
            // 隠しファイルでないことを確認
            if (!path.getFileName().toString().startsWith(".")) {
                changeCount.incrementAndGet();
            }
        });
        liveReloadService.startWatching(watchDir);
        
        Thread.sleep(200); // 監視開始を待つ
        
        // 隠しファイルを変更（これは無視される）
        Files.writeString(hiddenFile, "hidden content modified");
        
        Thread.sleep(200);
        
        // 通常ファイルを変更（これはカウントされる）
        Files.writeString(visibleFile, "visible content modified");
        
        Thread.sleep(300);
        
        liveReloadService.stopWatching();
        
        // 通常ファイルの変更のみがカウントされる
        assertThat(changeCount.get()).isEqualTo(1);
    }

    @Test
    void shouldThrottleFileChanges() throws IOException, InterruptedException {
        Path watchDir = tempDir.resolve("watch");
        Files.createDirectories(watchDir);
        Path testFile = watchDir.resolve("test.txt");
        
        AtomicInteger changeCount = new AtomicInteger(0);
        
        liveReloadService.setFileChangeListener(path -> changeCount.incrementAndGet());
        liveReloadService.setThrottleDelayMs(200); // 200msのスロットリング
        liveReloadService.startWatching(watchDir);
        
        // 短時間で複数回変更
        for (int i = 0; i < 5; i++) {
            Files.writeString(testFile, "content " + i, 
                i == 0 ? StandardOpenOption.CREATE : StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(50); // 50ms間隔
        }
        
        // スロットリング期間を待機
        Thread.sleep(300);
        
        liveReloadService.stopWatching();
        
        // スロットリングにより、変更通知が制限される
        assertThat(changeCount.get()).isLessThan(5);
    }

    @Test
    void shouldHandleWebSocketProtocol() {
        MockWebSocketSession session = new MockWebSocketSession();
        liveReloadService.addConnection(session);
        
        // クライアントからのhelloメッセージ
        String helloMessage = "{\"command\":\"hello\",\"protocols\":[\"http://livereload.com/protocols/official-7\"]}";
        liveReloadService.handleMessage(session, helloMessage);
        
        // サーバーからのhelloレスポンス
        assertThat(session.getReceivedMessages()).hasSize(1);
        String response = session.getReceivedMessages().get(0);
        assertThat(response).contains("\"command\":\"hello\"");
        assertThat(response).contains("\"protocols\":[\"http://livereload.com/protocols/official-7\"]");
        assertThat(response).contains("\"serverName\":\"JavaSSG\"");
    }

    @Test
    void shouldProvideConnectionStatistics() {
        MockWebSocketSession session1 = new MockWebSocketSession();
        MockWebSocketSession session2 = new MockWebSocketSession();
        
        liveReloadService.addConnection(session1);
        liveReloadService.addConnection(session2);
        
        liveReloadService.broadcastReload();
        liveReloadService.broadcastFileChange("/test.css");
        
        LiveReloadStatistics stats = liveReloadService.getStatistics();
        
        assertThat(stats.activeConnections()).isEqualTo(2);
        assertThat(stats.totalConnections()).isEqualTo(2);
        assertThat(stats.messagesSent()).isEqualTo(4); // 2回のブロードキャスト × 2接続
        assertThat(stats.reloadEvents()).isEqualTo(2);
    }

    @Test
    void shouldCleanupDisconnectedSessions() throws InterruptedException {
        MockWebSocketSession session1 = new MockWebSocketSession();
        MockWebSocketSession session2 = new MockWebSocketSession();
        MockWebSocketSession session3 = new MockWebSocketSession();
        
        liveReloadService.addConnection(session1);
        liveReloadService.addConnection(session2);
        liveReloadService.addConnection(session3);
        
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(3);
        
        // いくつかの接続を閉じる
        session1.close();
        session3.close();
        
        // クリーンアップを実行
        liveReloadService.cleanupConnections();
        
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleConcurrentConnections() throws InterruptedException {
        int connectionCount = 100;
        CompletableFuture<Void>[] futures = new CompletableFuture[connectionCount];
        
        for (int i = 0; i < connectionCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                MockWebSocketSession session = new MockWebSocketSession();
                session.setId("session-" + index);
                liveReloadService.addConnection(session);
            });
        }
        
        try {
            CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertThat(liveReloadService.getConnectionCount()).isEqualTo(connectionCount);
        
        // 全接続にブロードキャスト
        liveReloadService.broadcastReload();
        
        // すべての接続にメッセージが送信されることを確認
        LiveReloadStatistics stats = liveReloadService.getStatistics();
        assertThat(stats.messagesSent()).isEqualTo(connectionCount);
    }

    @Test
    void shouldSupportCustomReloadCommands() {
        MockWebSocketSession session = new MockWebSocketSession();
        liveReloadService.addConnection(session);
        
        // CSS固有のリロード
        liveReloadService.broadcastCssReload("/style.css");
        
        assertThat(session.getReceivedMessages()).hasSize(1);
        String message = session.getReceivedMessages().get(0);
        assertThat(message).contains("\"command\":\"reload\"");
        assertThat(message).contains("\"path\":\"/style.css\"");
        assertThat(message).contains("\"liveCSS\":true");
    }

    @Test
    void shouldHandleInvalidMessages() {
        MockWebSocketSession session = new MockWebSocketSession();
        liveReloadService.addConnection(session);
        
        // 無効なJSONメッセージ
        liveReloadService.handleMessage(session, "invalid json");
        
        // エラーレスポンス
        assertThat(session.getReceivedMessages()).hasSize(1);
        String response = session.getReceivedMessages().get(0);
        assertThat(response).contains("\"command\":\"error\"");
        assertThat(response).contains("\"message\":\"Invalid message format\"");
    }
}