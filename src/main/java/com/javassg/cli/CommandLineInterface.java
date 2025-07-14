package com.javassg.cli;

/**
 * JavaSSGのメインCLIインターフェース
 */
public class CommandLineInterface {
    
    private static final String VERSION = "1.0.0";
    
    public int run(String[] args) {
        if (args.length == 0) {
            showHelp();
            return 0;
        }
        
        String firstArg = args[0];
        
        // グローバルオプションの処理
        switch (firstArg) {
            case "--help", "-h":
                showHelp();
                return 0;
            case "--version", "-v":
                showVersion();
                return 0;
            default:
                // 不明なコマンドの場合
                System.out.println("Unknown command: " + firstArg);
                System.out.println("Run 'javassg --help' for usage information.");
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
        System.out.println("JavaSSG " + VERSION);
        System.out.println("Java Static Site Generator");
    }
}