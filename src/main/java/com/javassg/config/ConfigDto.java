package com.javassg.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.javassg.model.SecurityLimits;

import java.util.List;
import java.util.Map;

/**
 * 設定ファイルのデータ転送オブジェクト
 */
public class ConfigDto {
    
    @JsonProperty("site")
    public SiteDto site;
    
    @JsonProperty("build")
    public BuildDto build;
    
    @JsonProperty("server")
    public ServerDto server;
    
    @JsonProperty("blog")
    public BlogDto blog;
    
    @JsonProperty("limits")
    public LimitsDto limits;
    
    @JsonProperty("plugins")
    public List<PluginDto> plugins;
    
    public static class SiteDto {
        @JsonProperty("title")
        public String title;
        
        @JsonProperty("description")
        public String description;
        
        @JsonProperty("url")
        public String url;
        
        @JsonProperty("language")
        public String language;
        
        @JsonProperty("author")
        public AuthorDto author;
    }
    
    public static class AuthorDto {
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("email")
        public String email;
    }
    
    public static class BuildDto {
        @JsonProperty("contentDirectory")
        public String contentDirectory;
        
        @JsonProperty("outputDirectory")
        public String outputDirectory;
        
        @JsonProperty("staticDirectory")
        public String staticDirectory;
        
        @JsonProperty("templatesDirectory")
        public String templatesDirectory;
    }
    
    public static class ServerDto {
        @JsonProperty("port")
        public Integer port;
        
        @JsonProperty("liveReload")
        public Boolean liveReload;
    }
    
    public static class BlogDto {
        @JsonProperty("postsPerPage")
        public Integer postsPerPage;
        
        @JsonProperty("generateArchive")
        public Boolean generateArchive;
        
        @JsonProperty("generateCategories")
        public Boolean generateCategories;
        
        @JsonProperty("generateTags")
        public Boolean generateTags;
    }
    
    public static class LimitsDto {
        @JsonProperty("maxMarkdownFileSize")
        public Long maxMarkdownFileSize;
        
        @JsonProperty("maxConfigFileSize")
        public Long maxConfigFileSize;
        
        @JsonProperty("maxFrontMatterSize")
        public Long maxFrontMatterSize;
        
        @JsonProperty("maxFilenameLength")
        public Integer maxFilenameLength;
        
        @JsonProperty("maxTitleLength")
        public Integer maxTitleLength;
        
        @JsonProperty("maxDescriptionLength")
        public Integer maxDescriptionLength;
        
        public SecurityLimits toSecurityLimits() {
            return new SecurityLimits(
                maxMarkdownFileSize != null ? maxMarkdownFileSize : SecurityLimits.defaultLimits().maxMarkdownFileSize(),
                maxConfigFileSize != null ? maxConfigFileSize : SecurityLimits.defaultLimits().maxConfigFileSize(),
                maxFrontMatterSize != null ? maxFrontMatterSize : SecurityLimits.defaultLimits().maxFrontMatterSize(),
                maxFilenameLength != null ? maxFilenameLength : SecurityLimits.defaultLimits().maxFilenameLength(),
                maxTitleLength != null ? maxTitleLength : SecurityLimits.defaultLimits().maxTitleLength(),
                maxDescriptionLength != null ? maxDescriptionLength : SecurityLimits.defaultLimits().maxDescriptionLength()
            );
        }
    }
    
    public static class PluginDto {
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("enabled")
        public Boolean enabled;
        
        @JsonProperty("settings")
        public Map<String, Object> settings;
    }
}