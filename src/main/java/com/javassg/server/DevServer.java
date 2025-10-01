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
import java.util.concurrent.atomic.AtomicInteger;
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
    
    // 統合されたMIMEタイプマッピング
    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
        Map.entry(".html", "text/html; charset=utf-8"),
        Map.entry(".css", "text/css; charset=utf-8"),
        Map.entry(".js", "text/javascript; charset=utf-8"),
        Map.entry(".json", "application/json; charset=utf-8"),
        Map.entry(".txt", "text/plain; charset=utf-8"),
        Map.entry(".xml", "application/xml; charset=utf-8"),
        Map.entry(".pdf", "application/pdf"),
        Map.entry(".zip", "application/zip"),
        Map.entry(".png", "image/png"),
        Map.entry(".jpg", "image/jpeg"),
        Map.entry(".jpeg", "image/jpeg"),
        Map.entry(".gif", "image/gif"),
        Map.entry(".svg", "image/svg+xml"),
        Map.entry(".ico", "image/x-icon"),
        Map.entry(".webp", "image/webp"),
        Map.entry(".woff", "font/woff"),
        Map.entry(".woff2", "font/woff2"),
        Map.entry(".ttf", "font/ttf"),
        Map.entry(".eot", "application/vnd.ms-fontobject")
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

            // 制限されたスレッドプールを使用してリソースを管理
            int maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
            java.util.concurrent.ThreadFactory threadFactory = new java.util.concurrent.ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "DevServer-Worker-" + threadNumber.getAndIncrement());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY - 1); // 少し低い優先度
                    return thread;
                }
            };

            java.util.concurrent.ThreadPoolExecutor executor = new java.util.concurrent.ThreadPoolExecutor(
                Math.max(2, maxThreads / 4), // コアスレッド数
                maxThreads,                   // 最大スレッド数
                60L,                         // アイドル時間
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(100), // キューサイズ制限
                threadFactory,
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy() // キューが満杯時の拒否ポリシー
            );

            httpServer.setExecutor(executor);

            // ハンドラーの設定
            httpServer.createContext("/", new StaticFileHandler());
            httpServer.createContext("/livereload", new LiveReloadHandler());

            httpServer.start();
            running = true;

            logger.info("開発サーバーを起動しました: http://localhost:{}", port);
            logger.debug("スレッドプール設定: コア={}, 最大={}",
                executor.getCorePoolSize(), executor.getMaximumPoolSize());

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

        running = false; // 早期にフラグを設定

        try {
            // ライブリロードサービスを先に停止
            liveReloadService.stopWatching();

            if (httpServer != null) {
                // スレッドプールのシャットダウンを確実に実行
                java.util.concurrent.Executor executor = httpServer.getExecutor();
                if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
                    java.util.concurrent.ThreadPoolExecutor threadPool =
                        (java.util.concurrent.ThreadPoolExecutor) executor;

                    logger.debug("スレッドプールをシャットダウンします (実行中のタスク: {})",
                        threadPool.getActiveCount());

                    threadPool.shutdown();
                    if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.warn("スレッドプールのシャットダウンがタイムアウトしました。強制終了します。");
                        threadPool.shutdownNow();
                    }
                }

                // HTTPサーバーを停止
                httpServer.stop(0); // 即時停止
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("サーバー停止中に割り込みが発生しました");
        } catch (Exception e) {
            logger.error("サーバー停止中にエラーが発生しました", e);
        } finally {
            logger.info("開発サーバーを停止しました");
        }
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
                
                // パストラバーサル攻撃の防止（強化版）
                if (isPathTraversalAttack(path)) {
                    logger.warn("パストラバーサル攻撃を検出しました: {}", path);
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
            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            // WebSocketアップグレード要求のチェック
            if ("GET".equals(requestMethod) && "/livereload".equals(path)) {
                String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
                String connection = exchange.getRequestHeaders().getFirst("Connection");
                String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
                String version = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Version");
                String origin = exchange.getRequestHeaders().getFirst("Origin");

                // Originヘッダーの検証（CSRF対策）
                if (!isValidOrigin(origin)) {
                    logger.warn("不正なOriginからのWebSocket接続要求: {}", origin);
                    exchange.sendResponseHeaders(403, -1); // Forbidden
                    return;
                }

                // WebSocketハンドシェイクの検証
                if ("websocket".equalsIgnoreCase(upgrade) &&
                    connection != null && connection.toLowerCase().contains("upgrade") &&
                    key != null && !key.isEmpty() &&
                    "13".equals(version)) {
                    
                    // WebSocketレスポンスヘッダーの生成
                    String acceptKey = generateWebSocketAccept(key);
                    
                    exchange.getResponseHeaders().set("Upgrade", "websocket");
                    exchange.getResponseHeaders().set("Connection", "Upgrade");
                    exchange.getResponseHeaders().set("Sec-WebSocket-Accept", acceptKey);
                    exchange.sendResponseHeaders(101, -1); // Switching Protocols
                    
                    // クライアントをWebSocketセッションとして登録
                    WebSocketSession session = new SimpleWebSocketSession(exchange, acceptKey);
                    liveReloadService.addClient(session);
                    
                    logger.debug("WebSocketクライアントが接続しました: {}", acceptKey);
                } else {
                    // 不正なWebSocket要求
                    exchange.sendResponseHeaders(400, -1);
                }
            } else {
                // WebSocketエンドポイント以外の要求
                exchange.sendResponseHeaders(404, -1);
            }
        }
        
        private String generateWebSocketAccept(String key) {
            try {
                String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                String combined = key + magic;
                java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
                byte[] digest = sha1.digest(combined.getBytes());
                return java.util.Base64.getEncoder().encodeToString(digest);
            } catch (Exception e) {
                logger.error("WebSocket Acceptキーの生成に失敗しました", e);
                return "";
            }
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
            String extension = fileName.substring(lastDot).toLowerCase();
            String mimeType = MIME_TYPES.get(extension);
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
    
    /**
     * パストラバーサル攻撃を検出する強化版メソッド
     */
    private boolean isPathTraversalAttack(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // 1. 基本的なパターン検証（最も一般的な攻撃）
        String lowerPath = path.toLowerCase();

        // 直接のパストラバーサル
        if (path.contains("..")) {
            return true;
        }

        // 2. 複数レベルのエンコーディング攻撃
        String currentPath = path;
        for (int i = 0; i < 3; i++) { // 最大3回デコード
            try {
                String decoded = java.net.URLDecoder.decode(currentPath, "UTF-8");
                if (!decoded.equals(currentPath)) {
                    currentPath = decoded;
                    if (decoded.contains("..") ||
                        decoded.contains("../") ||
                        decoded.contains("..\\") ||
                        decoded.contains("%2e%2e") ||
                        decoded.startsWith("..")) {
                        return true;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                return true; // デコード失敗は攻撃とみなす
            }
        }

        // 3. 16進エンコーディング攻撃
        if (lowerPath.contains("%2e") || // . の16進
            lowerPath.contains("%2f") || // / の16進
            lowerPath.contains("%5c") || // \ の16進
            lowerPath.contains("%c0%af") || // / のオーバーロンエンコーディング
            lowerPath.contains("%c1%9c")) { // \ のオーバーロンエンコーディング
            return true;
        }

        // 4. Unicodeエンコーディング攻撃
        if (lowerPath.contains("\\u002e") || // . のUnicode
            lowerPath.contains("\\u002f") || // / のUnicode
            lowerPath.contains("\\u005c")) { // \ のUnicode
            return true;
        }

        // 5. Windows特有の攻撃
        if (lowerPath.matches(".*[a-z]:.*") || // ドライブ文字
            lowerPath.contains("\\\\") || // UNCパス
            lowerPath.contains("\\..\\") || // Windowsパス
            lowerPath.contains("..\\\\") ||
            lowerPath.contains("~") || // 短いファイル名
            lowerPath.contains("$")) { // 環境変数
            return true;
        }

        // 6. 路径正規化による検証
        try {
            Path normalizedPath = Paths.get(path).normalize();
            String normalizedStr = normalizedPath.toString();

            // 正規化後に絶対パスになる、または親ディレクトリを含む場合
            if (normalizedPath.isAbsolute() ||
                normalizedStr.startsWith("..") ||
                normalizedStr.contains("../") ||
                normalizedStr.contains("..\\") ||
                normalizedPath.getNameCount() > 20) { // 過度に深いパス
                return true;
            }
        } catch (Exception e) {
            return true; // 無効なパスは攻撃とみなす
        }

        // 7. 制御文字やnullバイト
        for (char c : path.toCharArray()) {
            if (c < 32 || c == 127 || c == 0) {
                return true;
            }
        }

        // 8. 非常に長いパス（DoS対策）
        if (path.length() > 255) {
            return true;
        }

        return false;
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

    /**
     * Originヘッダーを検証（CSRF対策）
     */
    private boolean isValidOrigin(String origin) {
        // 開発環境ではローカルホストのみ許可
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }

        String lowerOrigin = origin.toLowerCase();
        int port = getServerPort();

        // 許可されたOriginパターン
        return lowerOrigin.equals("http://localhost:" + port) ||
               lowerOrigin.equals("https://localhost:" + port) ||
               lowerOrigin.equals("http://127.0.0.1:" + port) ||
               lowerOrigin.equals("https://127.0.0.1:" + port) ||
               lowerOrigin.startsWith("http://localhost:") ||
               lowerOrigin.startsWith("https://localhost:") ||
               lowerOrigin.startsWith("http://127.0.0.1:") ||
               lowerOrigin.startsWith("https://127.0.0.1:");
    }

}