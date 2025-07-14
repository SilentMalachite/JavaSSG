package com.javassg.plugin;

import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 画像最適化プラグイン（基本実装）
 */
public class ImageOptimizationPlugin implements Plugin {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageOptimizationPlugin.class);
    
    private boolean enabled = true;
    private boolean generateResponsiveImages = true;
    private boolean optimizeImages = true;
    
    @Override
    public String getName() {
        return "imageOptimization";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "画像の最適化とレスポンシブ画像の生成を行います";
    }
    
    @Override
    public void initialize(SiteConfig config, Map<String, Object> settings) {
        if (settings != null) {
            this.generateResponsiveImages = (Boolean) settings.getOrDefault("generateResponsiveImages", true);
            this.optimizeImages = (Boolean) settings.getOrDefault("optimizeImages", true);
        }
        logger.debug("画像最適化プラグインを初期化しました (レスポンシブ: {}, 最適化: {})", 
                    generateResponsiveImages, optimizeImages);
    }
    
    @Override
    public void execute(PluginContext context) {
        if (context.getPhase() != PluginContext.PluginPhase.POST_BUILD) {
            return;
        }
        
        try {
            processImages(context.getOutputDirectory());
            logger.info("画像最適化が完了しました");
        } catch (Exception e) {
            logger.error("画像最適化エラー", e);
        }
    }
    
    private void processImages(Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            return;
        }
        
        Files.walk(outputDir)
            .filter(Files::isRegularFile)
            .filter(this::isImageFile)
            .forEach(this::processImage);
    }
    
    private boolean isImageFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") || 
               fileName.endsWith(".webp");
    }
    
    private void processImage(Path imageFile) {
        try {
            // 基本的な画像情報を取得
            long fileSize = Files.size(imageFile);
            String fileName = imageFile.getFileName().toString();
            
            // 実際の画像最適化処理はここで実装
            // 今回は基本的なログ出力のみ
            if (optimizeImages) {
                logger.debug("画像最適化をスキップしました（未実装）: {} ({} bytes)", fileName, fileSize);
            }
            
            // レスポンシブ画像の生成
            if (generateResponsiveImages) {
                generateResponsiveImages(imageFile);
            }
            
        } catch (Exception e) {
            logger.warn("画像処理エラー: " + imageFile, e);
        }
    }
    
    private void generateResponsiveImages(Path imageFile) {
        // レスポンシブ画像の生成処理
        // 実際の実装では、異なるサイズの画像を生成する
        String fileName = imageFile.getFileName().toString();
        logger.debug("レスポンシブ画像生成をスキップしました（未実装）: {}", fileName);
        
        // 例: 生成すべきサイズ
        // - 320w (モバイル)
        // - 768w (タブレット)
        // - 1200w (デスクトップ)
        // - 1920w (大画面)
    }
    
    @Override
    public void cleanup() {
        // 特にクリーンアップは不要
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public int getExecutionOrder() {
        return 800; // 早めに実行
    }
}