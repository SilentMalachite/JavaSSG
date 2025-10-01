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
            logger.error("設定ファイルのセキュリティ検証に失敗: {}", e.getMessage());
            throw new IOException("設定ファイルのセキュリティ検証に失敗しました: " + e.getMessage(), e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("設定ファイルのYAML解析に失敗: {}", e.getMessage());
            throw new IOException("設定ファイルのYAML形式が正しくありません: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("設定ファイルの読み込みに失敗しました", e);
            throw new IOException("設定ファイルの読み込みに失敗しました: " + e.getMessage(), e);
        }
    }
    
    private static SiteConfig parseConfigWithJackson(String yamlContent) throws IOException {
        try {
            // JacksonでYAMLを安全に解析
            ConfigDto configDto = yamlMapper.readValue(yamlContent, ConfigDto.class);

            // DTOからドメインモデルに変換
            SiteConfig siteConfig = convertToSiteConfig(configDto);

            // 設定の妥当性を検証
            List<String> validationErrors = siteConfig.validate();
            if (!validationErrors.isEmpty()) {
                logger.warn("設定の検証でエラーが見つかりました: {}", validationErrors);
                // 致命的でないエラーは続行、致命的なエラーは例外をスロー
                List<String> criticalErrors = validationErrors.stream()
                    .filter(error -> error.contains("必須") || error.contains("正しくありません"))
                    .toList();

                if (!criticalErrors.isEmpty()) {
                    throw new IOException("設定に致命的なエラーがあります: " + String.join(", ", criticalErrors));
                }
            }

            return siteConfig;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("YAML形式の解析に失敗しました: {}", e.getOriginalMessage());
            throw new IOException("設定ファイルのYAML形式が正しくありません: " + e.getOriginalMessage(), e);
        } catch (Exception e) {
            logger.warn("Jacksonでの解析に失敗しました。フォールバック解析を試行します: {}", e.getMessage());
            // フォールバック: 簡易解析
            try {
                return parseConfigFallback(yamlContent);
            } catch (Exception fallbackException) {
                logger.error("フォールバック解析にも失敗しました", fallbackException);
                throw new IOException("設定ファイルの解析に完全に失敗しました: " + e.getMessage(), e);
            }
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
    
    private static SiteConfig parseConfigFallback(String yamlContent) throws IOException {
        try {
            logger.warn("フォールバック設定を使用して設定ファイルを解析します");

            // 簡易的なフォールバック解析
            SiteInfo siteInfo = new SiteInfo(
                extractValue(yamlContent, "title", "My Site"),
                extractValue(yamlContent, "description", "A static site generated with JavaSSG"),
                extractValue(yamlContent, "url", "https://example.com"),
                extractValue(yamlContent, "language", "en-US"),
                new Author(
                    extractValue(yamlContent, "name", "Site Author"),
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

            BlogConfig blogConfig = new BlogConfig(
                extractIntValue(yamlContent, "postsPerPage", 10),
                extractBooleanValue(yamlContent, "generateArchive", true),
                extractBooleanValue(yamlContent, "generateCategories", true),
                extractBooleanValue(yamlContent, "generateTags", true)
            );

            // セキュリティ制限
            SecurityLimits limits = SecurityLimits.defaultLimits();

            SiteConfig siteConfig = new SiteConfig(siteInfo, buildConfig, serverConfig, blogConfig, limits, List.of());

            // フォールバック設定の検証
            List<String> validationErrors = siteConfig.validate();
            if (!validationErrors.isEmpty()) {
                logger.warn("フォールバック設定に検証エラーがあります: {}", validationErrors);
            }

            return siteConfig;

        } catch (Exception e) {
            logger.error("フォールバック設定の解析に失敗しました", e);
            throw new IOException("フォールバック設定の解析に失敗しました: " + e.getMessage(), e);
        }
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