package com.javassg.plugin;

import com.javassg.model.SiteConfig;

import java.util.Map;

/**
 * プラグインの基本インターフェース
 */
public interface Plugin {
    
    /**
     * プラグイン名を取得
     */
    String getName();
    
    /**
     * プラグインバージョンを取得
     */
    String getVersion();
    
    /**
     * プラグインの説明を取得
     */
    String getDescription();
    
    /**
     * プラグインの初期化
     */
    void initialize(SiteConfig config, Map<String, Object> settings);
    
    /**
     * プラグインの実行
     */
    void execute(PluginContext context);
    
    /**
     * プラグインの終了処理
     */
    void cleanup();
    
    /**
     * プラグインが有効かどうかを確認
     */
    boolean isEnabled();
    
    /**
     * プラグインの実行順序を取得（低い値ほど先に実行）
     */
    default int getExecutionOrder() {
        return 1000;
    }
}