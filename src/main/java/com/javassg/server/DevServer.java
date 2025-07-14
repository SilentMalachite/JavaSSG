package com.javassg.server;

import com.javassg.build.BuildEngine;
import com.javassg.build.BuildEngineInterface;
import com.javassg.model.SiteConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * 開発用HTTPサーバーの実装
 */
public class DevServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DevServer.class);
    
    private final SiteConfig siteConfig;
    private final BuildEngineInterface buildEngine;
    private final LiveReloadService liveReloadService;
    private final List<RequestLogEntry> requestLog = new CopyOnWriteArrayList<>();
    
    private HttpServer httpServer;
    private volatile boolean running = false;
    private final long startTime = System.currentTimeMillis();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final AtomicLong totalBytesServed = new AtomicLong(0);
    
    // CLI用の追加フィールド
    private int port = 8080;
    private Path outputDir;
    
    // MIMEタイプマッピング
    private static final Map<String, String> MIME_TYPES = Map.of(
        ".html", "text/html; charset=utf-8",
        ".css", "text/css; charset=utf-8",
        ".js", "text/javascript; charset=utf-8",
        ".json", "application/json; charset=utf-8",
        ".png", "image/png",
        ".jpg", "image/jpeg",
        ".jpeg", "image/jpeg",
        ".gif", "image/gif",
        ".svg", "image/svg+xml",
        ".ico", "image/x-icon"
    );
    
    private static final Map<String, String> ADDITIONAL_MIME_TYPES = Map.of(
        ".txt", "text/plain; charset=utf-8"
    );
    
    public DevServer(SiteConfig siteConfig, BuildEngineInterface buildEngine) {
        this.siteConfig = siteConfig;
        this.buildEngine = buildEngine;
        this.liveReloadService = new LiveReloadService();
    }
    
    // CLI用の簡易コンストラクタ
    public DevServer(SiteConfig siteConfig, Path outputDir, int port) {
        this.siteConfig = siteConfig;
        this.buildEngine = new BuildEngine(siteConfig, outputDir.getParent());
        this.liveReloadService = new LiveReloadService();
        this.port = port;
        this.outputDir = outputDir;
    }
    
    public void start() throws DevServerException {
        if (running) {
            throw new DevServerException("サーバーは既に実行中です");
        }
        
        int port = getServerPort();
        
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            
            // ハンドラーの設定
            httpServer.createContext("/", new StaticFileHandler());
            httpServer.createContext("/livereload", new LiveReloadHandler());
            
            httpServer.start();
            running = true;
            
            logger.info("開発サーバーを起動しました: http://localhost:{}", port);
            
        } catch (BindException e) {
            throw DevServerException.portInUse(port);
        } catch (IOException e) {
            throw DevServerException.serverStartFailed(e);
        }
    }
    
    public void stop() {
        if (!running) {
            return;
        }
        
        if (httpServer != null) {
            httpServer.stop(1);
        }
        
        liveReloadService.stopWatching();
        running = false;
        
        logger.info("開発サーバーを停止しました");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return httpServer != null ? httpServer.getAddress().getPort() : -1;
    }
    
    public void startWatching() throws IOException {
        Path outputDir = getOutputDirectory();
        liveReloadService.startWatching(outputDir);
        
        // ファイル変更時のコールバックを設定
        liveReloadService.setFileChangeListener(path -> {
            try {
                buildEngine.buildIncremental(LocalDateTime.now().minusMinutes(1));
                liveReloadService.broadcastFileChange(path.toString());
            } catch (Exception e) {
                logger.error("増分ビルドエラー", e);
            }
        });
    }
    
    public void stopWatching() {
        liveReloadService.stopWatching();
    }
    
    public int getActiveConnections() {
        return liveReloadService.getConnectionCount();
    }
    
    public DevServerStatistics getStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        long avgResponseTime = totalRequests.get() > 0 ? 
            requestLog.stream().mapToLong(RequestLogEntry::responseTimeMs).sum() / totalRequests.get() : 0;
        
        return new DevServerStatistics(
            totalRequests.get(),
            successfulRequests.get(),
            errorRequests.get(),
            getActiveConnections(),
            uptime,
            avgResponseTime,
            totalBytesServed.get()
        );
    }
    
    public List<RequestLogEntry> getRequestLog() {
        return List.copyOf(requestLog);
    }
    
    public WebSocketSession createWebSocketClient(String url) {
        // テスト用のWebSocketクライアント作成（実際の実装では別途WebSocketクライアントライブラリを使用）
        // このメソッドはテスト専用のため、実装では別のWebSocketクライアントを使用する
        // テスト環境では簡易実装を提供
        return new WebSocketSession() {
            private boolean closed = false;
            private final String id = "test-client-" + System.currentTimeMillis();
            
            @Override
            public String getId() { return id; }
            
            @Override
            public void sendMessage(String message) {
                // テスト用の簡易実装
            }
            
            @Override
            public boolean isClosed() { return closed; }
            
            @Override
            public boolean isConnected() { return !closed; }
            
            @Override
            public void close() { closed = true; }
        };
    }
    
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long startTime = System.currentTimeMillis();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                // セキュリティヘッダーの設定
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
                
                // パストラバーサル攻撃の防止
                if (path.contains("..") || path.contains("~")) {
                    sendError(exchange, 400, "Bad Request");
                    return;
                }
                
                // ルートパスの処理
                if ("/".equals(path)) {
                    path = "/index.html";
                }
                
                Path filePath = getOutputDirectory().resolve(path.substring(1));
                
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    // カスタム404ページがあるかチェック
                    Path custom404 = getOutputDirectory().resolve("404.html");
                    if (Files.exists(custom404)) {
                        serveFile(exchange, custom404, 404);
                    } else {
                        sendError(exchange, 404, "404 Not Found");
                    }
                    return;
                }
                
                // Rangeリクエストの処理
                String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    handleRangeRequest(exchange, filePath, rangeHeader);
                } else {
                    serveFile(exchange, filePath, 200);
                }
                
            } catch (Exception e) {
                logger.error("リクエスト処理エラー", e);
                sendError(exchange, 500, "Internal Server Error");
            } finally {
                logRequest(exchange, startTime);
            }
        }
        
        private void serveFile(HttpExchange exchange, Path filePath, int statusCode) throws IOException {
            byte[] content = Files.readAllBytes(filePath);
            String contentType = getMimeType(filePath);
            
            // ライブリロードスクリプトの注入
            if (contentType.contains("text/html")) {
                String html = new String(content);
                html = injectLiveReloadScript(html);
                content = html.getBytes();
            }
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            // Gzip圧縮の処理
            String acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            if (acceptEncoding != null && acceptEncoding.contains("gzip") && content.length > 1024) {
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
                content = gzipCompress(content);
            }
            
            exchange.sendResponseHeaders(statusCode, content.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
            
            totalBytesServed.addAndGet(content.length);
        }
        
        private void handleRangeRequest(HttpExchange exchange, Path filePath, String rangeHeader) throws IOException {
            // 簡易Range処理（実際にはより詳細な実装が必要）
            String[] rangeParts = rangeHeader.substring(6).split("-");
            long start = Long.parseLong(rangeParts[0]);
            long fileSize = Files.size(filePath);
            long end = rangeParts.length > 1 && !rangeParts[1].isEmpty() ? 
                Long.parseLong(rangeParts[1]) : fileSize - 1;
            
            byte[] allContent = Files.readAllBytes(filePath);
            byte[] rangeContent = new byte[(int) (end - start + 1)];
            System.arraycopy(allContent, (int) start, rangeContent, 0, rangeContent.length);
            
            exchange.getResponseHeaders().set("Content-Type", getMimeType(filePath));
            exchange.getResponseHeaders().set("Content-Range", 
                String.format("bytes %d-%d/%d", start, end, fileSize));
            exchange.sendResponseHeaders(206, rangeContent.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(rangeContent);
            }
        }
        
        private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
            byte[] response = message.getBytes();
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
            
            if (statusCode >= 400) {
                errorRequests.incrementAndGet();
            }
        }
        
        private String injectLiveReloadScript(String html) {
            String script = "<!-- LiveReload script -->\n" +
                          "<script>\n" +
                          "  var ws = new WebSocket('ws://localhost:" + getPort() + "/livereload');\n" +
                          "  ws.onmessage = function(event) {\n" +
                          "    var data = JSON.parse(event.data);\n" +
                          "    if (data.command === 'reload') {\n" +
                          "      location.reload();\n" +
                          "    }\n" +
                          "  };\n" +
                          "</script>";
            
            return html.replace("</body>", script + "\n</body>");
        }
    }
    
    private class LiveReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // WebSocket接続のハンドリング（簡易実装）
            // 実際にはWebSocketライブラリを使用することを推奨
            // このハンドラーは基本的なWebSocketアップグレードレスポンスのみ送信
            
            exchange.getResponseHeaders().set("Upgrade", "websocket");
            exchange.getResponseHeaders().set("Connection", "Upgrade");
            exchange.sendResponseHeaders(101, -1); // Switching Protocols
        }
    }
    
    private void logRequest(HttpExchange exchange, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        int statusCode = exchange.getResponseCode();
        boolean successful = statusCode >= 200 && statusCode < 400;
        
        RequestLogEntry logEntry = RequestLogEntry.create(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            statusCode,
            responseTime,
            0 // content length is tracked separately
        );
        
        requestLog.add(logEntry);
        totalRequests.incrementAndGet();
        
        if (successful) {
            successfulRequests.incrementAndGet();
        } else {
            errorRequests.incrementAndGet();
        }
        
        logger.debug("{} {} {} {}ms", 
                   exchange.getRequestMethod(), 
                   exchange.getRequestURI().getPath(), 
                   statusCode, 
                   responseTime);
    }
    
    private String getMimeType(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = fileName.substring(lastDot);
            String mimeType = MIME_TYPES.get(extension);
            if (mimeType == null) {
                mimeType = ADDITIONAL_MIME_TYPES.get(extension);
            }
            return mimeType != null ? mimeType : "application/octet-stream";
        }
        return "application/octet-stream";
    }
    
    private byte[] gzipCompress(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }
    
    private int getServerPort() {
        if (this.port > 0) {
            return this.port;
        }
        return siteConfig.getServerPort();
    }
    
    private Path getOutputDirectory() {
        if (this.outputDir != null) {
            return this.outputDir;
        }
        String outputDir = siteConfig.getOutputDirectory();
        return Paths.get(outputDir != null ? outputDir : "_site");
    }
    
}