package com.javassg.server;

import com.javassg.build.BuildEngineInterface;
import com.javassg.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevServerTest {

    private DevServer devServer;
    private HttpClient httpClient;
    
    @Mock
    private BuildEngineInterface buildEngine;
    
    @TempDir
    Path tempDir;
    
    private SiteConfig siteConfig;
    private int testPort;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // テスト用の空きポートを見つける
        testPort = findAvailablePort();
        
        siteConfig = new SiteConfig(
            new SiteInfo(
                "Test Site",
                "Test Description",
                "http://localhost:" + testPort,
                "ja-JP",
                new Author("Test Author", "test@example.com")
            ),
            new BuildConfig("content", "_site", "static", "templates"),
            new ServerConfig(testPort, true),
            new BlogConfig(10, true, true, true),
            null,
            List.of()
        );
        
        // テスト用コンテンツを作成
        setupTestContent();
        
        devServer = new DevServer(siteConfig, buildEngine);
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @AfterEach
    void tearDown() {
        if (devServer != null && devServer.isRunning()) {
            devServer.stop();
        }
    }

    @Test
    void shouldStartAndStopServer() throws Exception {
        assertThat(devServer.isRunning()).isFalse();
        
        devServer.start();
        
        assertThat(devServer.isRunning()).isTrue();
        assertThat(devServer.getPort()).isEqualTo(testPort);
        
        devServer.stop();
        
        assertThat(devServer.isRunning()).isFalse();
    }

    @Test
    void shouldServeStaticFiles() throws Exception {
        setupStaticFiles();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/style.css"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type"))
            .isPresent()
            .get().asString().contains("text/css");
        assertThat(response.body()).contains("body { margin: 0; }");
    }

    @Test
    void shouldServeHtmlPages() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/index.html"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type"))
            .isPresent()
            .get().asString().contains("text/html");
        assertThat(response.body()).contains("<h1>Welcome</h1>");
    }

    @Test
    void shouldReturn404ForMissingFiles() throws Exception {
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/nonexistent.html"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("404 Not Found");
    }

    @Test
    void shouldServeDirectoryIndex() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<h1>Welcome</h1>");
    }

    @Test
    void shouldInjectLiveReloadScript() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/index.html"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<!-- LiveReload script -->");
        assertThat(response.body()).contains("new WebSocket('ws://localhost:" + testPort + "/livereload')");
    }

    @Test
    void shouldHandleWebSocketConnections() throws Exception {
        devServer.start();
        
        // WebSocket接続のテスト（簡易版）
        // テスト用のWebSocketクライアントを作成
        WebSocketSession webSocketClient = devServer.createWebSocketClient("ws://localhost:" + testPort + "/livereload");
        
        // 接続が正常に作成されることを確認
        assertThat(webSocketClient).isNotNull();
        assertThat(webSocketClient.isConnected()).isTrue();
        assertThat(webSocketClient.isClosed()).isFalse();
        
        // 接続を閉じる
        webSocketClient.close();
        assertThat(webSocketClient.isClosed()).isTrue();
    }

    @Test
    void shouldTriggerLiveReloadOnFileChange() throws Exception {
        devServer.start();
        
        // ファイル変更の監視を開始
        devServer.startWatching();
        
        // テストファイルを変更
        Path testFile = tempDir.resolve("_site").resolve("test.html");
        Files.writeString(testFile, "<html><body><h1>Updated</h1></body></html>");
        
        // ライブリロードがトリガーされるまで待機
        Thread.sleep(500);
        
        // BuildEngineが呼び出されることを確認
        verify(buildEngine).buildIncremental(any());
        
        devServer.stopWatching();
    }

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        setupHtmlFiles();
        devServer.start();
        
        // 複数の同時リクエスト
        List<CompletableFuture<HttpResponse<String>>> futures = List.of(
            sendAsyncRequest("/index.html"),
            sendAsyncRequest("/index.html"),
            sendAsyncRequest("/index.html"),
            sendAsyncRequest("/index.html"),
            sendAsyncRequest("/index.html")
        );
        
        // すべてのリクエストの完了を待機
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        
        // すべてのレスポンスが200 OKであることを確認
        for (var future : futures) {
            HttpResponse<String> response = future.get();
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Test
    void shouldHandleCustomErrorPages() throws Exception {
        setupCustomErrorPages();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/nonexistent.html"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Custom 404 Page");
    }

    @Test
    void shouldServeCorrectMimeTypes() throws Exception {
        setupVariousFileTypes();
        
        devServer.start();
        
        // CSS
        HttpResponse<String> cssResponse = sendRequest("/style.css");
        assertThat(cssResponse.headers().firstValue("content-type"))
            .isPresent()
            .get().asString().contains("text/css");
        
        // JavaScript
        HttpResponse<String> jsResponse = sendRequest("/script.js");
        assertThat(jsResponse.headers().firstValue("content-type"))
            .isPresent()
            .get().asString().contains("text/javascript");
        
        // JSON
        HttpResponse<String> jsonResponse = sendRequest("/data.json");
        assertThat(jsonResponse.headers().firstValue("content-type"))
            .isPresent()
            .get().asString().contains("application/json");
    }

    @Test
    void shouldHandleRangeRequests() throws Exception {
        setupLargeFile();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/large.txt"))
            .header("Range", "bytes=0-99")
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(206); // Partial Content
        assertThat(response.headers().firstValue("content-range")).isPresent();
        assertThat(response.body()).hasSize(100);
    }

    @Test
    void shouldImplementSecurityHeaders() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        HttpResponse<String> response = sendRequest("/index.html");
        
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Content-Type-Options"))
            .isPresent()
            .get().asString().isEqualTo("nosniff");
        assertThat(response.headers().firstValue("X-Frame-Options"))
            .isPresent()
            .get().asString().isEqualTo("DENY");
    }

    @Test
    void shouldHandleGzipCompression() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/index.html"))
            .header("Accept-Encoding", "gzip")
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        // レスポンスが圧縮されているかチェック
        assertThat(response.headers().firstValue("content-encoding"))
            .isPresent()
            .get().asString().contains("gzip");
    }

    @Test
    void shouldPreventDirectoryTraversal() throws Exception {
        setupHtmlFiles();
        
        devServer.start();
        
        // ディレクトリトラバーサル攻撃の試行
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/../../../etc/passwd"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(400); // Bad Request または 404
    }

    @Test
    void shouldThrowExceptionOnPortInUse() throws Exception {
        devServer.start();
        
        // 同じポートで別のサーバーを起動しようとする
        DevServer anotherServer = new DevServer(siteConfig, buildEngine);
        
        assertThatThrownBy(() -> anotherServer.start())
            .isInstanceOf(DevServerException.class)
            .hasMessageContaining("ポートが既に使用されています");
    }

    @Test
    void shouldProvideServerStatistics() throws Exception {
        setupHtmlFiles();
        devServer.start();
        
        // いくつかのリクエストを送信
        sendRequest("/index.html");
        sendRequest("/index.html");
        sendRequest("/nonexistent.html");
        
        DevServerStatistics stats = devServer.getStatistics();
        
        assertThat(stats.totalRequests()).isEqualTo(3);
        assertThat(stats.successfulRequests()).isEqualTo(2);
        assertThat(stats.errorRequests()).isEqualTo(1);
        assertThat(stats.activeConnections()).isGreaterThanOrEqualTo(0);
        assertThat(stats.uptimeMs()).isGreaterThan(0);
    }

    @Test
    void shouldLogRequestsAndResponses() throws Exception {
        setupHtmlFiles();
        devServer.start();
        
        sendRequest("/index.html");
        
        // ログが出力されることを確認（実際のテストではLogbackのテストアペンダーを使用）
        var logEntries = devServer.getRequestLog();
        assertThat(logEntries).hasSize(1);
        
        var logEntry = logEntries.get(0);
        assertThat(logEntry.method()).isEqualTo("GET");
        assertThat(logEntry.path()).isEqualTo("/index.html");
        assertThat(logEntry.statusCode()).isEqualTo(200);
        assertThat(logEntry.responseTimeMs()).isGreaterThan(0);
    }

    private void setupTestContent() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
    }

    private void setupStaticFiles() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
        Files.writeString(siteDir.resolve("style.css"), "body { margin: 0; padding: 0; }");
    }

    private void setupHtmlFiles() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
        Files.writeString(siteDir.resolve("index.html"), 
            "<!DOCTYPE html><html><body><h1>Welcome</h1></body></html>");
    }

    private void setupCustomErrorPages() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
        Files.writeString(siteDir.resolve("404.html"), 
            "<!DOCTYPE html><html><body><h1>Custom 404 Page</h1></body></html>");
    }

    private void setupVariousFileTypes() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
        Files.writeString(siteDir.resolve("style.css"), "body { color: red; }");
        Files.writeString(siteDir.resolve("script.js"), "console.log('test');");
        Files.writeString(siteDir.resolve("data.json"), "{\"key\": \"value\"}");
    }

    private void setupLargeFile() throws IOException {
        Path siteDir = tempDir.resolve("_site");
        Files.createDirectories(siteDir);
        
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("This is line ").append(i).append(" of the large file.\n");
        }
        Files.writeString(siteDir.resolve("large.txt"), content.toString());
    }

    private HttpResponse<String> sendRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + path))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> sendAsyncRequest(String path) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + path))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private int findAvailablePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8080; // フォールバック
        }
    }
}