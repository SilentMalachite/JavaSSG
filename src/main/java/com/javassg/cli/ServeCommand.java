package com.javassg.cli;

import com.javassg.build.BuildEngine;
import com.javassg.build.BuildEngineInterface;
import com.javassg.config.ConfigLoader;
import com.javassg.model.SiteConfig;
import com.javassg.server.DevServer;
import com.javassg.server.DevServerStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * serveコマンドの実装
 */
public class ServeCommand {
    
    private static final Logger logger = LoggerFactory.getLogger(ServeCommand.class);
    
    public int execute(String[] args, Path workingDir) throws Exception {
        ServeOptions options = parseOptions(args);
        
        if (options.help) {
            showHelp();
            return 0;
        }
        
        try {
            // 設定ファイルの読み込み
            Path configPath = workingDir.resolve(options.configFile);
            SiteConfig config = ConfigLoader.loadFromPath(configPath);
            
            // 出力ディレクトリの存在確認
            Path outputDir = workingDir.resolve(options.outputDirectory != null ? 
                options.outputDirectory : config.build().outputDirectory());
            
            if (!Files.exists(outputDir)) {
                System.err.println("出力ディレクトリが存在しません: " + outputDir);
                System.err.println("先にビルドを実行してください: javassg build");
                return 1;
            }
            
            if (options.outputDirectory != null) {
                System.out.println("配信ディレクトリ: " + options.outputDirectory);
            }
            
            // ポート範囲の検証
            if (options.port < 1 || options.port > 65535) {
                System.err.println("無効なポート番号: " + options.port);
                System.err.println("有効な範囲: 1-65535");
                return 1;
            }
            
            // ビルドエンジンの初期化
            BuildEngineInterface buildEngine = new BuildEngine(config, workingDir);
            
            // サーバー開始前のビルド
            if (options.build) {
                System.out.println("サーバー開始前にビルドを実行しています...");
                buildEngine.build();
            }
            
            // 開発サーバーの初期化
            DevServer devServer = new DevServer(config, outputDir, options.port);
            
            // ライブリロードの設定
            if (options.liveReload) {
                System.out.println("ライブリロードが有効です");
                devServer.startWatching();
            } else if (options.noLiveReload) {
                System.out.println("ライブリロードを無効にしました");
            }
            
            // サーバー開始
            try {
                devServer.start();
                
                String host = options.host != null ? options.host : "localhost";
                String url = String.format("http://%s:%d", host, devServer.getPort());
                
                if ("0.0.0.0".equals(host)) {
                    System.out.println("すべてのネットワークインターフェースで待機しています");
                }
                
                System.out.println("開発サーバーを開始しました: " + url);
                System.out.println("Ctrl+C で停止できます");
                
                // ブラウザ自動起動
                if (options.open) {
                    openBrowser(url);
                }
                
                // 統計情報の表示
                if (options.stats) {
                    startStatisticsDisplay(devServer, options.verbose);
                }
                
                // 詳細情報の表示
                if (options.verbose) {
                    displayVerboseInfo(devServer);
                }
                
                // サーバー実行
                waitForShutdown(devServer);
                
                return 0;
                
            } catch (RuntimeException e) {
                if (e.getMessage().contains("ポートが既に使用されています")) {
                    System.err.println("ポートが既に使用されています: " + options.port);
                    System.err.println("別のポートを試してください: javassg serve --port <port>");
                    return 1;
                } else {
                    throw e;
                }
            }
            
        } catch (Exception e) {
            logger.error("サーバー開始中にエラーが発生しました", e);
            System.err.println("サーバーエラー: " + e.getMessage());
            return 1;
        }
    }
    
    private ServeOptions parseOptions(String[] args) {
        ServeOptions options = new ServeOptions();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h":
                    options.help = true;
                    break;
                case "--port", "-p":
                    if (i + 1 < args.length) {
                        options.port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--host":
                    if (i + 1 < args.length) {
                        options.host = args[++i];
                    }
                    break;
                case "--live-reload":
                    options.liveReload = true;
                    break;
                case "--no-live-reload":
                    options.noLiveReload = true;
                    break;
                case "--open", "-o":
                    options.open = true;
                    break;
                case "--build":
                    options.build = true;
                    break;
                case "--stats":
                    options.stats = true;
                    break;
                case "--verbose":
                    options.verbose = true;
                    break;
                case "--config":
                    if (i + 1 < args.length) {
                        options.configFile = args[++i];
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        options.outputDirectory = args[++i];
                    }
                    break;
            }
        }
        
        return options;
    }
    
    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                System.out.println("ブラウザで開いています: " + url);
            }
        } catch (Exception e) {
            logger.warn("ブラウザの起動に失敗しました", e);
        }
    }
    
    private void startStatisticsDisplay(DevServer devServer, boolean verbose) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                DevServerStatistics stats = devServer.getStatistics();
                
                System.out.println("\n=== サーバー統計 ===");
                System.out.println("リクエスト数: " + stats.totalRequests());
                System.out.println("成功: " + stats.successfulRequests());
                System.out.println("エラー: " + stats.errorRequests());
                System.out.println("アクティブな接続: " + stats.activeConnections());
                
                long uptimeMinutes = stats.uptimeMs() / (1000 * 60);
                System.out.println("稼働時間: " + uptimeMinutes + "分");
                
                if (verbose) {
                    displayVerboseInfo(devServer);
                }
                
            } catch (Exception e) {
                logger.warn("統計情報の取得に失敗しました", e);
            }
        }, 10, 30, TimeUnit.SECONDS);
        
        // シャットダウン時にクリーンアップ
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }
    
    private void displayVerboseInfo(DevServer devServer) {
        if (devServer.getActiveConnections() > 0) {
            System.out.println("アクティブな接続: " + devServer.getActiveConnections());
        }
    }
    
    private void waitForShutdown(DevServer devServer) {
        // シャットダウンフックの設定
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nサーバーを停止しています...");
            devServer.stop();
        }));
        
        try {
            // メインスレッドを維持
            while (devServer.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\nCtrl+C で停止します");
        }
    }
    
    private void showHelp() {
        System.out.println("Usage: javassg serve [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --port <port>, -p     Server port (default: 8080)");
        System.out.println("  --host <host>         Server host (default: localhost)");
        System.out.println("  --live-reload         Enable live reload");
        System.out.println("  --no-live-reload      Disable live reload");
        System.out.println("  --open, -o            Open browser automatically");
        System.out.println("  --build               Build before serving");
        System.out.println("  --stats               Show server statistics");
        System.out.println("  --verbose             Verbose output");
        System.out.println("  --config <file>       Custom configuration file");
        System.out.println("  --output <dir>        Custom output directory");
        System.out.println("  --help, -h            Show this help message");
    }
    
    private static class ServeOptions {
        boolean help = false;
        int port = 8080;
        String host = null;
        boolean liveReload = false;
        boolean noLiveReload = false;
        boolean open = false;
        boolean build = false;
        boolean stats = false;
        boolean verbose = false;
        String configFile = "config.yaml";
        String outputDirectory = null;
    }
}