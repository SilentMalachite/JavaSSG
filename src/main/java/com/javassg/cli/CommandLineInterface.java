package com.javassg.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

/**
 * JavaSSGのメインCLIインターフェース
 */
public class CommandLineInterface {
    
    private static String getVersion() {
        try (java.io.InputStream is = CommandLineInterface.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                return props.getProperty("version", "1.0.0");
            }
        } catch (IOException e) {
            // プロパティファイルが存在しない場合はデフォルトを返す
        }
        return "1.0.0";
    }
    
    public int run(String[] args) {
        if (args.length == 0) {
            showHelp();
            return 0;
        }

        // グローバルオプション（最低限）の解釈：作業ディレクトリのみ適用
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < args.length; i++) {
            if ("--working-directory".equals(args[i]) && i + 1 < args.length) {
                workingDir = Paths.get(args[i + 1]);
                i++;
            }
        }

        // サブコマンド or グローバルオプション
        // 先頭の非オプション引数をコマンドとして扱う
        String firstArg = Arrays.stream(args)
                .filter(a -> !a.startsWith("-"))
                .findFirst()
                .orElse(args[0]);

        // グローバルオプション
        if ("--help".equals(firstArg) || "-h".equals(firstArg)) {
            showHelp();
            return 0;
        }
        if ("--version".equals(firstArg) || "-v".equals(firstArg)) {
            showVersion();
            return 0;
        }

        // サブコマンドのディスパッチ（引数はそのまま委譲：各コマンドが解析）
        try {
            switch (firstArg) {
                case "serve":
                    return new ServeCommand().execute(args, workingDir);
                case "build":
                    return new BuildCommand().execute(args, workingDir);
                case "new":
                    return new NewCommand().execute(args, workingDir);
                default:
                    System.out.println("Unknown command: " + firstArg);
                    System.out.println("Run 'javassg --help' for usage information.");
                    return 1;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    private void showHelp() {
        System.out.println("JavaSSG - Java Static Site Generator");
        System.out.println();
        System.out.println("Usage: javassg [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  serve     Start development server");
        System.out.println("  build     Build the site");
        System.out.println("  new       Create new content");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --config <file>         Configuration file path");
        System.out.println("  --verbose              Enable verbose output");
        System.out.println("  --quiet                Suppress output");
        System.out.println("  --working-directory <dir>  Set working directory");
        System.out.println("  --help, -h             Show this help message");
        System.out.println("  --version, -v          Show version information");
    }
    
    private void showVersion() {
        System.out.println("JavaSSG " + getVersion());
        System.out.println("Java Static Site Generator");
    }
}
