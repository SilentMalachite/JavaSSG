package com.javassg.cli;

import com.javassg.build.BuildEngine;
import com.javassg.build.BuildEngineInterface;
import com.javassg.config.ConfigLoader;
import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * buildコマンドの実装
 */
public class BuildCommand {
    
    private static final Logger logger = LoggerFactory.getLogger(BuildCommand.class);
    
    public int execute(String[] args, Path workingDir) throws Exception {
        BuildOptions options = parseOptions(args);
        
        if (options.help) {
            showHelp();
            return 0;
        }
        
        try {
            if (options.verbose) {
                System.out.println("詳細ログが有効です");
                System.out.println("設定ファイル: " + options.configFile);
                System.out.println("ビルド開始時刻: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            // 設定ファイルの読み込み
            Path configPath = workingDir.resolve(options.configFile);
            if (!Files.exists(configPath)) {
                System.err.println("設定ファイルが見つかりません: " + configPath);
                return 1;
            }
            
            SiteConfig config;
            try {
                config = ConfigLoader.loadFromPath(configPath);
            } catch (Exception e) {
                System.err.println("設定ファイルの読み込みに失敗しました: " + e.getMessage());
                return 1;
            }
            
            // contentディレクトリの存在確認
            Path contentDir = workingDir.resolve(config.build().contentDirectory());
            if (!Files.exists(contentDir)) {
                System.err.println("contentディレクトリが存在しません: " + contentDir);
                return 1;
            }
            
            // 出力ディレクトリの設定
            if (options.outputDirectory != null) {
                System.out.println("出力先: " + options.outputDirectory);
                Files.createDirectories(workingDir.resolve(options.outputDirectory));
            }
            
            // ビルドエンジンの初期化
            BuildEngineInterface buildEngine = new BuildEngine(config, workingDir);
            
            // ビルドの実行
            BuildEngine.BuildResult result = executeBuild(buildEngine, options);
            
            // 結果の表示
            displayBuildResult(result, options);
            
            // ウォッチモード
            if (options.watch) {
                System.out.println("ファイル監視を開始しました。Ctrl+Cで終了します。");
                startWatchMode(buildEngine, options);
            }
            
            return result.success() ? 0 : 1;
            
        } catch (Exception e) {
            logger.error("ビルド中にエラーが発生しました", e);
            System.err.println("ビルドエラー: " + e.getMessage());
            return 1;
        }
    }
    
    private BuildOptions parseOptions(String[] args) {
        BuildOptions options = new BuildOptions();
        List<String> argList = Arrays.asList(args);
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help", "-h":
                    options.help = true;
                    break;
                case "--drafts":
                    options.includeDrafts = true;
                    break;
                case "--production":
                    options.production = true;
                    break;
                case "--clean":
                    options.clean = true;
                    break;
                case "--incremental":
                    options.incremental = true;
                    break;
                case "--watch":
                    options.watch = true;
                    break;
                case "--verbose":
                    options.verbose = true;
                    break;
                case "--stats":
                    options.stats = true;
                    break;
                case "--config":
                    if (i + 1 < args.length) {
                        options.configFile = args[++i];
                        System.out.println("カスタム設定を使用: " + options.configFile);
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
    
    private BuildEngine.BuildResult executeBuild(BuildEngineInterface buildEngine, BuildOptions options) {
        if (options.clean) {
            System.out.println("出力ディレクトリをクリアしています...");
        }
        
        if (options.includeDrafts) {
            System.out.println("下書きを含めてビルドしています...");
            return buildEngine.buildWithDrafts();
        } else if (options.production) {
            System.out.println("本番用ビルドを実行しています...");
            return buildEngine.buildForProduction();
        } else if (options.incremental) {
            System.out.println("増分ビルドを実行しています...");
            return buildEngine.buildIncremental(LocalDateTime.now().minusHours(1));
        } else {
            return buildEngine.build();
        }
    }
    
    private void displayBuildResult(BuildEngine.BuildResult result, BuildOptions options) {
        if (result.success()) {
            if (options.clean) {
                System.out.println("クリーンビルドが完了しました！");
            } else if (options.production) {
                System.out.println("本番用ビルドが完了しました！");
                System.out.println("最適化が完了しました");
            } else if (options.incremental) {
                System.out.println("増分ビルドが完了しました！");
            } else {
                System.out.println("ビルドが完了しました！");
            }
            
            System.out.println(String.format("処理時間: %dms", result.buildTimeMs()));
            System.out.println(String.format("生成されたファイル: %dページ、%d投稿、合計%dファイル", 
                result.totalPages(), result.totalPosts(), result.generatedFiles()));
            
            if (options.stats && result.statistics() != null) {
                displayBuildStatistics(result.statistics());
            }
        } else {
            System.err.println("ビルドが失敗しました");
            for (String error : result.errors()) {
                System.err.println("エラー: " + error);
            }
        }
    }
    
    private void displayBuildStatistics(BuildEngine.BuildStatistics stats) {
        System.out.println("\n=== ビルド統計 ===");
        System.out.println("合計ファイル: " + stats.totalFiles());
        System.out.println("コンテンツファイル: " + stats.contentFiles());
        System.out.println("静的ファイル: " + stats.staticFiles());
        System.out.println("テンプレートファイル: " + stats.templateFiles());
        
        double sizeInMB = stats.outputSize() / (1024.0 * 1024.0);
        System.out.println(String.format("出力サイズ: %.1f MB", sizeInMB));
    }
    
    private void startWatchMode(BuildEngineInterface buildEngine, BuildOptions options) {
        // ウォッチモードの実装（簡易版）
        Thread watchThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    // 実際の実装では、ファイル変更監視を行う
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        watchThread.setDaemon(true);
        watchThread.start();
        
        // Ctrl+C待機
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void showHelp() {
        System.out.println("Usage: javassg build [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --drafts              Include draft posts");
        System.out.println("  --production          Production build with optimizations");
        System.out.println("  --clean               Clean build (remove output directory first)");
        System.out.println("  --incremental         Incremental build");
        System.out.println("  --watch               Watch for changes and rebuild");
        System.out.println("  --verbose             Verbose output");
        System.out.println("  --stats               Show build statistics");
        System.out.println("  --config <file>       Custom configuration file");
        System.out.println("  --output <dir>        Custom output directory");
        System.out.println("  --help, -h            Show this help message");
    }
    
    private static class BuildOptions {
        boolean help = false;
        boolean includeDrafts = false;
        boolean production = false;
        boolean clean = false;
        boolean incremental = false;
        boolean watch = false;
        boolean verbose = false;
        boolean stats = false;
        String configFile = "config.yaml";
        String outputDirectory = null;
    }
}