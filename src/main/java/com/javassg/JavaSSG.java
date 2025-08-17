package com.javassg;

import com.javassg.cli.CommandLineInterface;
import com.javassg.config.ConfigLoader;
import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JavaSSGのメインエントリポイント
 */
public class JavaSSG {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaSSG.class);
    
    public static void main(String[] args) {
        try {
            logger.info("JavaSSG 1.0.0 を開始します");
            
            // コマンドライン引数の解析
            CommandLineInterface cli = new CommandLineInterface();
            int exitCode = cli.run(args);
            
            if (exitCode != 0) {
                logger.error("JavaSSGが異常終了しました (exit code: {})", exitCode);
                System.exit(exitCode);
            }
            
            logger.info("JavaSSGが正常終了しました");
            
        } catch (Exception e) {
            logger.error("予期しないエラーが発生しました", e);
            System.err.println("エラー: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * デフォルト設定でSiteConfigを読み込む
     */
    public static SiteConfig loadConfig() {
        return loadConfig(Paths.get("config.yaml"));
    }
    
    /**
     * 指定されたパスからSiteConfigを読み込む
     */
    public static SiteConfig loadConfig(Path configPath) {
        try {
            return ConfigLoader.loadFromPath(configPath);
        } catch (Exception e) {
            logger.warn("設定ファイルの読み込みに失敗しました。デフォルト設定を使用します: {}", e.getMessage());
            return SiteConfig.defaultConfig();
        }
    }
    
    /**
     * アプリケーションの情報を出力
     */
    public static void printInfo() {
        System.out.println("JavaSSG - Java Static Site Generator");
        String implVersion = JavaSSG.class.getPackage() != null
            ? JavaSSG.class.getPackage().getImplementationVersion()
            : null;
        System.out.println("Version: " + (implVersion != null ? implVersion : "1.0.0"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
    }
}
