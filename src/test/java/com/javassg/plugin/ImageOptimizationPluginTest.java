package com.javassg.plugin;

import com.javassg.model.SiteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageOptimizationPluginTest {

    private ImageOptimizationPlugin plugin;
    private SiteConfig siteConfig;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        plugin = new ImageOptimizationPlugin();
        siteConfig = SiteConfig.defaultConfig();
    }

    @Test
    void shouldInitializeWithDefaultSettings() {
        plugin.initialize(siteConfig, null);
        
        assertThat(plugin.getName()).isEqualTo("imageOptimization");
        assertThat(plugin.getVersion()).isEqualTo("1.0.0");
        assertThat(plugin.getDescription()).contains("画像の最適化");
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldInitializeWithCustomSettings() {
        Map<String, Object> settings = Map.of(
            "generateResponsiveImages", false,
            "optimizeImages", true
        );
        
        plugin.initialize(siteConfig, settings);
        
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldExecuteInPostBuildPhase() throws IOException {
        // 出力ディレクトリを作成
        Path outputDir = tempDir.resolve("_site");
        Files.createDirectories(outputDir);
        
        // テスト用画像ファイルを作成（空ファイル）
        Files.createFile(outputDir.resolve("test.jpg"));
        Files.createFile(outputDir.resolve("image.png"));
        Files.createFile(outputDir.resolve("icon.gif"));
        Files.createFile(outputDir.resolve("photo.webp"));
        Files.createFile(outputDir.resolve("document.pdf")); // 画像以外のファイル
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(),
            outputDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        
        // プラグインを実行（エラーが発生しないことを確認）
        plugin.execute(context);
        
        // ファイルが存在することを確認
        assertThat(Files.exists(outputDir.resolve("test.jpg"))).isTrue();
        assertThat(Files.exists(outputDir.resolve("image.png"))).isTrue();
        assertThat(Files.exists(outputDir.resolve("icon.gif"))).isTrue();
        assertThat(Files.exists(outputDir.resolve("photo.webp"))).isTrue();
        assertThat(Files.exists(outputDir.resolve("document.pdf"))).isTrue();
    }

    @Test
    void shouldSkipExecutionInOtherPhases() {
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(),
            tempDir,
            Map.of(),
            PluginContext.PluginPhase.PRE_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        
        // PRE_BUILDフェーズでは何も実行されない
        plugin.execute(context);
        
        // エラーが発生しないことを確認
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldHaveCorrectExecutionOrder() {
        assertThat(plugin.getExecutionOrder()).isEqualTo(800);
    }

    @Test
    void shouldCleanupWithoutErrors() {
        plugin.initialize(siteConfig, null);
        
        // クリーンアップがエラーなく実行されることを確認
        plugin.cleanup();
        
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    void shouldHandleNonExistentOutputDirectory() {
        Path nonExistentDir = tempDir.resolve("non-existent");
        
        PluginContext context = new PluginContext(
            siteConfig,
            List.of(),
            List.of(),
            nonExistentDir,
            Map.of(),
            PluginContext.PluginPhase.POST_BUILD
        );
        
        plugin.initialize(siteConfig, null);
        
        // 存在しないディレクトリでもエラーが発生しないことを確認
        plugin.execute(context);
        
        assertThat(plugin.isEnabled()).isTrue();
    }
}