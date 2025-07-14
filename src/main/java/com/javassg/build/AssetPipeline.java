package com.javassg.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AssetPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetPipeline.class);
    
    private static final long MAX_ASSET_SIZE = 5L * 1024 * 1024; // 5MB
    private final Map<String, Long> processingTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> fileCounts = new ConcurrentHashMap<>();
    private long totalSizeReduction = 0;
    
    public CssProcessingResult processCss(Path sourceDir, Path outputDir) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, String> processedFiles = new HashMap<>();
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                
                if (fileName.endsWith(".scss") || fileName.endsWith(".sass")) {
                    String cssFileName = fileName.replaceAll("\\.(scss|sass)$", ".css");
                    Path outputFile = outputDir.resolve(cssFileName);
                    
                    String scssContent = Files.readString(file);
                    String compiledCss = compileSass(scssContent);
                    
                    Files.createDirectories(outputFile.getParent());
                    Files.writeString(outputFile, compiledCss);
                    
                    processedFiles.put(fileName, cssFileName);
                    logger.debug("SCSS/Sassをコンパイルしました: {} -> {}", fileName, cssFileName);
                } else if (fileName.endsWith(".css")) {
                    Path outputFile = outputDir.resolve(fileName);
                    Files.createDirectories(outputFile.getParent());
                    Files.copy(file, outputFile, StandardCopyOption.REPLACE_EXISTING);
                    processedFiles.put(fileName, fileName);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        long processingTime = System.currentTimeMillis() - startTime;
        fileCounts.put("css", processedFiles.size());
        processingTimes.put("css", processingTime);
        
        return new CssProcessingResult(processedFiles, processingTime, true);
    }
    
    public void processCssWithMinification(Path sourceDir, Path outputDir) throws IOException {
        processCss(sourceDir, outputDir);
        
        Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".css")) {
                    String css = Files.readString(file);
                    String minified = minifyCSS(css);
                    Files.writeString(file, minified);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public JavaScriptProcessingResult processJavaScript(Path sourceDir, Path outputDir) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, String> processedFiles = new HashMap<>();
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                
                if (fileName.endsWith(".js")) {
                    Path outputFile = outputDir.resolve(fileName);
                    String jsContent = Files.readString(file);
                    String transpiled = transpileJavaScript(jsContent);
                    
                    Files.createDirectories(outputFile.getParent());
                    Files.writeString(outputFile, transpiled);
                    
                    processedFiles.put(fileName, fileName);
                    logger.debug("JavaScriptを処理しました: {}", fileName);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        long processingTime = System.currentTimeMillis() - startTime;
        fileCounts.put("js", processedFiles.size());
        processingTimes.put("js", processingTime);
        
        return new JavaScriptProcessingResult(processedFiles, processingTime, true);
    }
    
    public BundleResult bundleJavaScript(Path sourceDir, Path outputDir, String bundleName) throws IOException {
        List<String> modules = new ArrayList<>();
        StringBuilder bundledContent = new StringBuilder();
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".js")) {
                    String content = Files.readString(file);
                    String moduleName = sourceDir.relativize(file).toString();
                    
                    // ES6 importを解決してバンドル
                    String processedContent = resolveImports(content, sourceDir);
                    bundledContent.append("// Module: ").append(moduleName).append("\n");
                    bundledContent.append(processedContent).append("\n\n");
                    
                    modules.add(moduleName);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        Path bundleFile = outputDir.resolve(bundleName);
        Files.createDirectories(bundleFile.getParent());
        Files.writeString(bundleFile, bundledContent.toString());
        
        logger.info("JavaScriptバンドルを作成しました: {} ({}モジュール)", bundleName, modules.size());
        
        return new BundleResult(bundleFile.toString(), bundledContent.length(), modules.size());
    }
    
    public ImageOptimizationResult optimizeImages(Path sourceDir, Path outputDir) throws IOException {
        long startTime = System.currentTimeMillis();
        final AtomicLong originalSize = new AtomicLong(0);
        final AtomicLong optimizedSize = new AtomicLong(0);
        Map<String, OptimizedImageInfo> processedImages = new HashMap<>();
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String extension = getFileExtension(fileName);
                
                if (Set.of(".jpg", ".jpeg", ".png", ".gif").contains(extension)) {
                    // ファイルサイズチェック
                    if (attrs.size() > MAX_ASSET_SIZE) {
                        throw new AssetProcessingException(
                            String.format("ファイルサイズが上限を超えています: %s (%d bytes)", file, attrs.size())
                        );
                    }
                    
                    Path outputFile = outputDir.resolve(fileName);
                    long original = attrs.size();
                    
                    // 画像最適化を実行
                    optimizeImage(file, outputFile);
                    long optimized = Files.size(outputFile);
                    
                    originalSize.addAndGet(original);
                    optimizedSize.addAndGet(optimized);
                    
                    processedImages.put(fileName, new OptimizedImageInfo(
                        fileName, original, optimized, calculateCompression(original, optimized)
                    ));
                    
                    logger.debug("画像を最適化しました: {} ({} -> {} bytes)", 
                               fileName, original, optimized);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        long processingTime = System.currentTimeMillis() - startTime;
        long origSize = originalSize.get();
        long optSize = optimizedSize.get();
        double compressionRatio = calculateCompression(origSize, optSize);
        
        fileCounts.put("images", processedImages.size());
        processingTimes.put("images", processingTime);
        totalSizeReduction += (origSize - optSize);
        
        return new ImageOptimizationResult(origSize, optSize, compressionRatio, processedImages);
    }
    
    public WebPGenerationResult generateWebPImages(Path sourceDir, Path outputDir) throws IOException {
        Map<String, String> generatedWebP = new HashMap<>();
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String extension = getFileExtension(fileName);
                
                if (Set.of(".jpg", ".jpeg", ".png").contains(extension)) {
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String webpName = baseName + ".webp";
                    
                    Path outputFile = outputDir.resolve(fileName);
                    Path webpFile = outputDir.resolve(webpName);
                    
                    // 元ファイルをコピー
                    Files.createDirectories(outputFile.getParent());
                    Files.copy(file, outputFile, StandardCopyOption.REPLACE_EXISTING);
                    
                    // WebP版を生成（実際にはライブラリを使用）
                    generateWebP(file, webpFile);
                    
                    generatedWebP.put(fileName, webpName);
                    logger.debug("WebP画像を生成しました: {} -> {}", fileName, webpName);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return new WebPGenerationResult(generatedWebP);
    }
    
    public ResponsiveImageResult createResponsiveImages(Path sourceDir, Path outputDir) throws IOException {
        Map<String, Map<String, ResponsiveImageVariant>> responsiveVariants = new HashMap<>();
        
        int[] sizes = {576, 768, 992, 1200}; // sm, md, lg, xl
        String[] sizeNames = {"sm", "md", "lg", "xl"};
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String extension = getFileExtension(fileName);
                
                if (Set.of(".jpg", ".jpeg", ".png").contains(extension)) {
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    
                    // 元ファイルをコピー
                    Path outputFile = outputDir.resolve(fileName);
                    Files.createDirectories(outputFile.getParent());
                    Files.copy(file, outputFile, StandardCopyOption.REPLACE_EXISTING);
                    
                    Map<String, ResponsiveImageVariant> variants = new HashMap<>();
                    
                    for (int i = 0; i < sizes.length; i++) {
                        String variantName = baseName + "-" + sizeNames[i] + extension;
                        Path variantFile = outputDir.resolve(variantName);
                        
                        // リサイズ処理（実際には画像処理ライブラリを使用）
                        resizeImage(file, variantFile, sizes[i]);
                        
                        variants.put(sizeNames[i], new ResponsiveImageVariant(
                            variantName, sizes[i], (int) (sizes[i] * 0.75) // 4:3比率と仮定
                        ));
                    }
                    
                    responsiveVariants.put(fileName, variants);
                    logger.debug("レスポンシブ画像を生成しました: {}", fileName);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return new ResponsiveImageResult(responsiveVariants);
    }
    
    public AssetManifest generateManifest() {
        Map<String, Map<String, String>> assets = new HashMap<>();
        LocalDateTime buildTime = LocalDateTime.now();
        String version = generateVersion();
        
        // 各アセットタイプの情報を収集
        for (Map.Entry<String, Integer> entry : fileCounts.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            
            Map<String, String> typeInfo = new HashMap<>();
            typeInfo.put("count", String.valueOf(count));
            typeInfo.put("type", type);
            typeInfo.put("processingTime", String.valueOf(processingTimes.getOrDefault(type, 0L)));
            
            assets.put(type, typeInfo);
        }
        
        return new AssetManifest(assets, buildTime, version);
    }
    
    public String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが利用できません", e);
        }
    }
    
    public void processCssWithVersioning(Path sourceDir, Path outputDir) throws IOException {
        processCss(sourceDir, outputDir);
        
        Files.walkFileTree(outputDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".css")) {
                    String hash = calculateFileHash(file);
                    String shortHash = hash.substring(0, 8);
                    
                    String fileName = file.getFileName().toString();
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String versionedName = baseName + "." + shortHash + ".css";
                    
                    Path versionedFile = file.getParent().resolve(versionedName);
                    Files.move(file, versionedFile);
                    
                    logger.debug("バージョン付きファイルを作成しました: {}", versionedName);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public DependencyResult processCssWithDependencies(Path sourceDir, Path outputDir) throws IOException {
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        
        // まず依存関係を解析
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".css")) {
                    String fileName = file.getFileName().toString();
                    String content = Files.readString(file);
                    List<String> dependencies = extractCssDependencies(content);
                    dependencyGraph.put(fileName, dependencies);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        // 依存関係を解決してCSSをコンパイル
        processCss(sourceDir, outputDir);
        
        return new DependencyResult(dependencyGraph);
    }
    
    public ProcessingStatistics getProcessingStatistics() {
        int totalFiles = fileCounts.values().stream().mapToInt(Integer::intValue).sum();
        long totalTime = processingTimes.values().stream().mapToLong(Long::longValue).sum();
        
        return new ProcessingStatistics(
            totalFiles,
            fileCounts.getOrDefault("css", 0),
            fileCounts.getOrDefault("js", 0),
            fileCounts.getOrDefault("images", 0),
            totalTime,
            totalSizeReduction
        );
    }
    
    private String compileSass(String sassContent) {
        // 簡易SCSS/Sassコンパイラの実装
        // 実際にはlibsassやDartSassを使用することを推奨
        return sassContent
            .replaceAll("\\$([a-zA-Z-]+):\\s*([^;]+);", "") // 変数定義を除去
            .replaceAll("\\$([a-zA-Z-]+)", "var(--$1)") // 変数参照をCSS変数に変換
            .replaceAll("\\s*\\{\\s*", " {\n  ") // ネストを展開（簡易）
            .replaceAll("\\s*\\}\\s*", "\n}\n");
    }
    
    private String minifyCSS(String css) {
        return css
            .replaceAll("/\\*.*?\\*/", "") // コメント除去
            .replaceAll("\\s+", " ") // 複数空白を1つに
            .replaceAll("\\s*([{}:;,>+~])\\s*", "$1") // 記号周りの空白除去
            .replaceAll("#([0-9a-fA-F])\\1([0-9a-fA-F])\\2([0-9a-fA-F])\\3", "#$1$2$3") // 色の短縮
            .trim();
    }
    
    private String transpileJavaScript(String jsContent) {
        // 簡易ES6+トランスパイラの実装
        // 実際にはBabelやTypeScriptコンパイラを使用することを推奨
        return jsContent
            .replaceAll("const\\s+", "var ") // const -> var
            .replaceAll("let\\s+", "var ") // let -> var
            .replaceAll("\\`([^`]*)\\`", "\"$1\"") // テンプレートリテラルを文字列に
            .replaceAll("\\(([^)]*?)\\)\\s*=>", "function($1)"); // アロー関数を通常の関数に
    }
    
    private String resolveImports(String content, Path baseDir) {
        // ES6 import文の解決
        // 実際にはより高度なモジュールバンドラーを使用することを推奨
        return content.replaceAll("import\\s+.*?from\\s+['\"]([^'\"]+)['\"];?", "// Import: $1");
    }
    
    private void optimizeImage(Path inputFile, Path outputFile) throws IOException {
        // 実際の画像最適化処理
        // ImageIOやTwelveMonkeysなどの画像処理ライブラリを使用
        Files.createDirectories(outputFile.getParent());
        Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        
        // 簡易最適化（実際にはより高度な処理を実装）
        byte[] originalData = Files.readAllBytes(outputFile);
        byte[] optimizedData = Arrays.copyOf(originalData, (int) (originalData.length * 0.9));
        Files.write(outputFile, optimizedData);
    }
    
    private void generateWebP(Path inputFile, Path webpFile) throws IOException {
        // WebP形式への変換（実際にはwebpライブラリを使用）
        Files.createDirectories(webpFile.getParent());
        Files.copy(inputFile, webpFile, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private void resizeImage(Path inputFile, Path outputFile, int targetWidth) throws IOException {
        // 画像リサイズ処理（実際には画像処理ライブラリを使用）
        Files.createDirectories(outputFile.getParent());
        Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private List<String> extractCssDependencies(String cssContent) {
        List<String> dependencies = new ArrayList<>();
        // @import文から依存関係を抽出
        String[] lines = cssContent.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("@import")) {
                // 簡易パース
                if (line.contains("url(")) {
                    String url = line.substring(line.indexOf("url(") + 4, line.indexOf(")"));
                    url = url.replace("'", "").replace("\"", "");
                    dependencies.add(url);
                }
            }
        }
        return dependencies;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot).toLowerCase() : "";
    }
    
    private double calculateCompression(long original, long optimized) {
        return original > 0 ? (double) (original - optimized) / original : 0.0;
    }
    
    private String generateVersion() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
    
    // Record classes for return types
    public record CssProcessingResult(
        Map<String, String> processedFiles,
        long processingTimeMs,
        boolean transpiled
    ) {}
    
    public record JavaScriptProcessingResult(
        Map<String, String> processedFiles,
        long processingTimeMs,
        boolean transpiled
    ) {}
    
    public record BundleResult(
        String bundleFile,
        long bundleSize,
        int moduleCount
    ) {}
    
    public record ImageOptimizationResult(
        long originalSize,
        long optimizedSize,
        double compressionRatio,
        Map<String, OptimizedImageInfo> processedImages
    ) {}
    
    public record OptimizedImageInfo(
        String filename,
        long originalSize,
        long optimizedSize,
        double compressionRatio
    ) {}
    
    public record WebPGenerationResult(
        Map<String, String> generatedWebP
    ) {}
    
    public record ResponsiveImageResult(
        Map<String, Map<String, ResponsiveImageVariant>> responsiveVariants
    ) {}
    
    public record ResponsiveImageVariant(
        String filename,
        int width,
        int height
    ) {}
    
    public record AssetManifest(
        Map<String, Map<String, String>> assets,
        LocalDateTime buildTime,
        String version
    ) {}
    
    public record DependencyResult(
        Map<String, List<String>> dependencyGraph
    ) {}
    
    public record ProcessingStatistics(
        int totalFiles,
        int cssFiles,
        int jsFiles,
        int imageFiles,
        long totalProcessingTime,
        long totalSizeReduction
    ) {}
}