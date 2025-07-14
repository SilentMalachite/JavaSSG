package com.javassg.plugin;

import com.javassg.model.PluginConfig;
import com.javassg.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * プラグインマネージャー
 */
public class PluginManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final List<Plugin> enabledPlugins = new ArrayList<>();
    private final SiteConfig siteConfig;
    
    public PluginManager(SiteConfig siteConfig) {
        this.siteConfig = siteConfig;
        registerBuiltinPlugins();
        loadAndInitializePlugins();
    }
    
    /**
     * 組み込みプラグインを登録
     */
    private void registerBuiltinPlugins() {
        registerPlugin(new SitemapPlugin());
        registerPlugin(new RssPlugin());
        registerPlugin(new MinifyPlugin());
        registerPlugin(new ImageOptimizationPlugin());
        registerPlugin(new SyntaxHighlightPlugin());
    }
    
    /**
     * プラグインを登録
     */
    public void registerPlugin(Plugin plugin) {
        if (plugin == null) {
            logger.warn("nullプラグインを登録しようとしました");
            return;
        }
        
        plugins.put(plugin.getName(), plugin);
        logger.debug("プラグインを登録しました: {}", plugin.getName());
    }
    
    /**
     * プラグインを読み込み、初期化
     */
    private void loadAndInitializePlugins() {
        List<PluginConfig> pluginConfigs = siteConfig.plugins();
        if (pluginConfigs == null || pluginConfigs.isEmpty()) {
            logger.info("設定されたプラグインはありません");
            return;
        }
        
        for (PluginConfig config : pluginConfigs) {
            if (!config.enabled()) {
                logger.debug("プラグインが無効です: {}", config.name());
                continue;
            }
            
            Plugin plugin = plugins.get(config.name());
            if (plugin == null) {
                logger.warn("プラグインが見つかりません: {}", config.name());
                continue;
            }
            
            try {
                plugin.initialize(siteConfig, config.settings());
                enabledPlugins.add(plugin);
                logger.info("プラグインを初期化しました: {}", plugin.getName());
            } catch (Exception e) {
                logger.error("プラグインの初期化に失敗しました: " + plugin.getName(), e);
            }
        }
        
        // 実行順序でソート
        enabledPlugins.sort(Comparator.comparingInt(Plugin::getExecutionOrder));
    }
    
    /**
     * 指定されたフェーズのプラグインを実行
     */
    public void executePlugins(PluginContext context) {
        if (enabledPlugins.isEmpty()) {
            logger.debug("実行するプラグインはありません");
            return;
        }
        
        logger.info("プラグインを実行します (フェーズ: {}, 数: {})", 
                   context.getPhase(), enabledPlugins.size());
        
        for (Plugin plugin : enabledPlugins) {
            try {
                if (plugin.isEnabled()) {
                    long startTime = System.currentTimeMillis();
                    plugin.execute(context);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    logger.debug("プラグイン実行完了: {} ({}ms)", plugin.getName(), duration);
                } else {
                    logger.debug("プラグインがスキップされました: {}", plugin.getName());
                }
            } catch (Exception e) {
                logger.error("プラグイン実行エラー: " + plugin.getName(), e);
            }
        }
    }
    
    /**
     * 全プラグインのクリーンアップ
     */
    public void cleanup() {
        logger.info("プラグインのクリーンアップを開始します");
        
        for (Plugin plugin : enabledPlugins) {
            try {
                plugin.cleanup();
                logger.debug("プラグインクリーンアップ完了: {}", plugin.getName());
            } catch (Exception e) {
                logger.error("プラグインクリーンアップエラー: " + plugin.getName(), e);
            }
        }
        
        enabledPlugins.clear();
        logger.info("プラグインのクリーンアップが完了しました");
    }
    
    /**
     * 利用可能なプラグインのリストを取得
     */
    public List<Plugin> getAvailablePlugins() {
        return new ArrayList<>(plugins.values());
    }
    
    /**
     * 有効なプラグインのリストを取得
     */
    public List<Plugin> getEnabledPlugins() {
        return new ArrayList<>(enabledPlugins);
    }
    
    /**
     * プラグインの情報を取得
     */
    public Map<String, Object> getPluginInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("available", plugins.size());
        info.put("enabled", enabledPlugins.size());
        info.put("plugins", plugins.values().stream()
            .map(p -> Map.of(
                "name", p.getName(),
                "version", p.getVersion(),
                "description", p.getDescription(),
                "enabled", p.isEnabled()
            ))
            .collect(Collectors.toList()));
        return info;
    }
}