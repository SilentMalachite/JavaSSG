package com.javassg.build;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * ビルドエンジンのインターフェース
 */
public interface BuildEngineInterface {
    
    BuildEngine.BuildResult build();
    
    BuildEngine.BuildResult buildIncremental(LocalDateTime lastBuild);
    
    BuildEngine.BuildResult buildWithDrafts();
    
    BuildEngine.BuildResult buildForProduction();
    
    BuildEngine.BuildResult buildWithValidation();
    
    void clean() throws IOException;
    
    void startWatching() throws IOException;
    
    void stopWatching();
}