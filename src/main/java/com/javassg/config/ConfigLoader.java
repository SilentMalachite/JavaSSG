package com.javassg.config;

import com.javassg.model.*;
import com.javassg.security.SecurityValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 設定ファイルを読み込むローダークラス
 */
public class ConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    public static SiteConfig loadFromPath(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("設定ファイルが見つかりません: " + configPath);
        }
        
        try {
            // セキュリティ検証
            SecurityValidator validator = new SecurityValidator(SecurityLimits.defaultLimits());
            validator.validateFilePath(configPath);
            validator.validateFileSize(configPath);
            
            String configContent = Files.readString(configPath);
            
            // 基本的な検証
            if (configContent.trim().isEmpty()) {
                throw new IOException("設定ファイルが空です");
            }
            
            // JacksonでYAMLを解析
            return parseConfigWithJackson(configContent);
            
        } catch (com.javassg.security.SecurityException e) {
            throw new IOException("設定ファイルのセキュリティ検証に失敗しました: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.warn("設定ファイルの解析に失敗しました。デフォルト設定を使用します: {}", e.getMessage());
            return SiteConfig.defaultConfig();
        }
    }
    
    private static SiteConfig parseConfigWithJackson(String yamlContent) throws IOException {
        try {
            // JacksonでYAMLを安全に解析
            ConfigDto configDto = yamlMapper.readValue(yamlContent, ConfigDto.class);
            
            // DTOからドメインモデルに変換
            return convertToSiteConfig(configDto);
            
        } catch (Exception e) {
            logger.warn("Jacksonでの解析に失敗しました。フォールバック解析を試行します: {}", e.getMessage());
            // フォールバック: 簡易解析
            return parseConfigFallback(yamlContent);
        }
    }
    
    private static SiteConfig convertToSiteConfig(ConfigDto dto) {
        // Site情報
        SiteInfo siteInfo = new SiteInfo(
            dto.site != null && dto.site.title != null ? dto.site.title : "My Site",
            dto.site != null && dto.site.description != null ? dto.site.description : "A static site",
            dto.site != null && dto.site.url != null ? dto.site.url : "https://example.com",
            dto.site != null && dto.site.language != null ? dto.site.language : "ja-JP",
            new Author(
                dto.site != null && dto.site.author != null && dto.site.author.name != null ? 
                    dto.site.author.name : "Site Author",
                dto.site != null && dto.site.author != null && dto.site.author.email != null ? 
                    dto.site.author.email : "author@example.com"
            )
        );
        
        // Build設定
        BuildConfig buildConfig = new BuildConfig(
            dto.build != null && dto.build.contentDirectory != null ? dto.build.contentDirectory : "content",
            dto.build != null && dto.build.outputDirectory != null ? dto.build.outputDirectory : "_site",
            dto.build != null && dto.build.staticDirectory != null ? dto.build.staticDirectory : "static",
            dto.build != null && dto.build.templatesDirectory != null ? dto.build.templatesDirectory : "templates"
        );
        
        // Server設定
        ServerConfig serverConfig = new ServerConfig(
            dto.server != null && dto.server.port != null ? dto.server.port : 8080,
            dto.server != null && dto.server.liveReload != null ? dto.server.liveReload : true
        );
        
        // Blog設定
        BlogConfig blogConfig = new BlogConfig(
            dto.blog != null && dto.blog.postsPerPage != null ? dto.blog.postsPerPage : 10,
            dto.blog != null && dto.blog.generateArchive != null ? dto.blog.generateArchive : true,
            dto.blog != null && dto.blog.generateCategories != null ? dto.blog.generateCategories : true,
            dto.blog != null && dto.blog.generateTags != null ? dto.blog.generateTags : true
        );
        
        // セキュリティ制限
        SecurityLimits limits = dto.limits != null ? dto.limits.toSecurityLimits() : SecurityLimits.defaultLimits();
        
        // プラグイン設定
        List<PluginConfig> plugins = new ArrayList<>();
        if (dto.plugins != null) {
            for (ConfigDto.PluginDto pluginDto : dto.plugins) {
                plugins.add(new PluginConfig(
                    pluginDto.name != null ? pluginDto.name : "",
                    pluginDto.enabled != null ? pluginDto.enabled : false,
                    pluginDto.settings != null ? pluginDto.settings : Collections.emptyMap()
                ));
            }
        }
        
        return new SiteConfig(siteInfo, buildConfig, serverConfig, blogConfig, limits, plugins);
    }
    
    private static SiteConfig parseConfigFallback(String yamlContent) {
        // 簡易的なフォールバック解析
        SiteInfo siteInfo = new SiteInfo(
            extractValue(yamlContent, "title", "Test Site"),
            extractValue(yamlContent, "description", "Test Description"),
            extractValue(yamlContent, "url", "https://example.com"),
            extractValue(yamlContent, "language", "ja-JP"),
            new Author(
                extractValue(yamlContent, "name", "Author"),
                extractValue(yamlContent, "email", "author@example.com")
            )
        );
        
        BuildConfig buildConfig = new BuildConfig(
            extractValue(yamlContent, "contentDirectory", "content"),
            extractValue(yamlContent, "outputDirectory", "_site"),
            extractValue(yamlContent, "staticDirectory", "static"),
            extractValue(yamlContent, "templatesDirectory", "templates")
        );
        
        ServerConfig serverConfig = new ServerConfig(
            extractIntValue(yamlContent, "port", 8080),
            extractBooleanValue(yamlContent, "liveReload", true)
        );
        
        return new SiteConfig(siteInfo, buildConfig, serverConfig, null, null, null);
    }
    
    private static String extractValue(String yaml, String key, String defaultValue) {
        try {
            String[] lines = yaml.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith(key + ":")) {
                    String value = line.substring(line.indexOf(":") + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        } catch (Exception e) {
            // フォールバック
        }
        return defaultValue;
    }
    
    private static int extractIntValue(String yaml, String key, int defaultValue) {
        try {
            String value = extractValue(yaml, key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static boolean extractBooleanValue(String yaml, String key, boolean defaultValue) {
        try {
            String value = extractValue(yaml, key, String.valueOf(defaultValue));
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}