# JavaSSG API Documentation

This directory contains the API documentation for JavaSSG.

## Core Classes

### Main Entry Point
- [JavaSSG](JavaSSG.md) - Main application entry point

### Command Line Interface
- [CommandLineInterface](cli/CommandLineInterface.md) - CLI parser and command dispatcher
- [BuildCommand](cli/BuildCommand.md) - Site building functionality
- [ServeCommand](cli/ServeCommand.md) - Development server
- [NewCommand](cli/NewCommand.md) - Content creation commands

### Build System
- [BuildEngine](build/BuildEngine.md) - Core build pipeline
- [HtmlGenerator](build/HtmlGenerator.md) - HTML generation
- [AssetPipeline](build/AssetPipeline.md) - Asset processing

### Server Components
- [DevServer](server/DevServer.md) - Development server implementation
- [LiveReloadService](server/LiveReloadService.md) - Live reload functionality

### Parsing and Processing
- [MarkdownParser](parser/MarkdownParser.md) - Markdown processing
- [TemplateEngine](template/TemplateEngine.md) - Template rendering

### Plugin System
- [PluginManager](plugin/PluginManager.md) - Plugin management
- [Plugin](plugin/Plugin.md) - Plugin interface
- [PluginContext](plugin/PluginContext.md) - Plugin execution context

### Caching
- [CacheManager](cache/CacheManager.md) - Cache management
- [ContentCache](cache/ContentCache.md) - Content caching
- [TemplateCache](cache/TemplateCache.md) - Template caching
- [RenderCache](cache/RenderCache.md) - Rendering result caching

### Security
- [SecurityValidator](security/SecurityValidator.md) - Security validation
- [InputSanitizer](security/InputSanitizer.md) - Input sanitization

### Configuration
- [ConfigLoader](config/ConfigLoader.md) - Configuration loading
- [SiteConfig](model/SiteConfig.md) - Site configuration model

### Data Models
- [Page](model/Page.md) - Page data model
- [Post](model/Post.md) - Blog post model
- [SiteInfo](model/SiteInfo.md) - Site information model
- [Template](model/Template.md) - Template model

## Usage Examples

### Basic Usage

```java
// Load configuration
SiteConfig config = ConfigLoader.loadFromPath(Paths.get("config.yaml"));

// Create build engine
BuildEngine buildEngine = new BuildEngine(config);

// Build site
buildEngine.build();
```

### Plugin Development

```java
public class CustomPlugin implements Plugin {
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public void execute(PluginContext context) {
        // Plugin implementation
    }
}
```

### Template Development

```java
// Custom filter example
templateEngine.addFilter("custom", (value, args) -> {
    // Custom filter logic
    return processedValue;
});
```

## Error Handling

JavaSSG uses comprehensive exception handling:

- `BuildException` - Build process errors
- `SecurityException` - Security validation errors
- `TemplateException` - Template processing errors
- `DevServerException` - Server-related errors

## Threading and Concurrency

- Build processes use parallel execution where possible
- WebSocket connections are thread-safe
- Cache operations are synchronized
- File watching uses background threads

## Memory Management

- Streaming processing for large files
- Automatic cache cleanup
- Resource management for file handles
- Memory-efficient data structures