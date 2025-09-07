# Frequently Asked Questions

## General Questions

### What is JavaSSG?

JavaSSG is a modern, fast, and secure static site generator built with Java 21. It's designed to be a Java alternative to popular static site generators like Jekyll, Hugo, or Eleventy.

### Why Java?

Java provides:
- Excellent performance and memory management
- Strong type safety
- Comprehensive security features
- Rich ecosystem of libraries
- Cross-platform compatibility

### How does it compare to other static site generators?

| Feature | JavaSSG | Jekyll | Hugo | Eleventy |
|---------|---------|--------|------|----------|
| Language | Java 21 | Ruby | Go | JavaScript |
| Speed | Very Fast | Medium | Very Fast | Fast |
| Security | Excellent | Good | Good | Good |
| Plugins | Yes | Yes | Yes | Yes |
| Live Reload | Yes | Yes | Yes | Yes |

## Installation Questions

### What are the system requirements?

- Java 21 or higher
- Maven 3.9 or higher (for building from source)
- macOS 13+, Windows 10+, or Linux

### Can I install JavaSSG without Java?

Yes! You can build a native application using jpackage that includes Java runtime:

```bash
mvn clean package jpackage:jpackage
```

### How do I update JavaSSG?

If you built from source:
```bash
git pull origin main
mvn clean package
```

For native applications, rebuild with the latest version.

## Usage Questions

### How do I create a new site?

```bash
java -jar javassg.jar new site my-site
```

### How do I add a blog post?

```bash
java -jar javassg.jar new post "My Post Title" --working-directory ./my-site
```

### How do I customize the site structure?

Edit `config.yaml` to change directories:

```yaml
build:
  contentDirectory: "content"
  outputDirectory: "_site"
  staticDirectory: "static"
  templatesDirectory: "templates"
```

### Can I use custom themes?

Yes! JavaSSG uses a flexible template system. You can:
- Modify existing templates
- Create custom templates
- Use template inheritance
- Add custom filters

## Development Questions

### How do I start the development server?

```bash
java -jar javassg.jar serve --working-directory ./my-site
```

### How do I enable live reload?

Live reload is enabled by default. To disable:

```bash
java -jar javassg.jar serve --no-live-reload --working-directory ./my-site
```

### How do I build for production?

```bash
java -jar javassg.jar build --production --working-directory ./my-site
```

### Can I use incremental builds?

Yes! Use the `--incremental` flag:

```bash
java -jar javassg.jar build --incremental --working-directory ./my-site
```

## Plugin Questions

### What plugins are available?

Built-in plugins:
- **sitemap**: Generate sitemap.xml
- **rss**: Generate RSS feeds
- **minify**: Minify HTML/CSS
- **imageOptimization**: Optimize images
- **syntaxHighlight**: Code syntax highlighting

### How do I create a custom plugin?

1. Implement the `Plugin` interface
2. Add your plugin to the configuration
3. Place the plugin class in the classpath

Example:
```java
public class MyPlugin implements Plugin {
    @Override
    public String getName() {
        return "my-plugin";
    }
    
    @Override
    public void execute(PluginContext context) {
        // Your plugin logic
    }
}
```

### How do I configure plugins?

In `config.yaml`:

```yaml
plugins:
  - name: "sitemap"
    enabled: true
  - name: "rss"
    enabled: true
    settings:
      maxItems: 20
```

## Security Questions

### What security features does JavaSSG provide?

- XSS attack prevention
- Path traversal protection
- Input validation and sanitization
- Safe template processing
- File size limitations
- Secure asset handling

### How do I configure security limits?

In `config.yaml`:

```yaml
limits:
  maxMarkdownFileSize: 10485760     # 10MB
  maxConfigFileSize: 1048576        # 1MB
  maxFrontMatterSize: 100000        # 100KB
  maxFilenameLength: 255
  maxTitleLength: 200
  maxDescriptionLength: 500
```

### Is JavaSSG safe for production use?

Yes! JavaSSG includes comprehensive security features and is designed with security in mind. However, always:
- Keep dependencies updated
- Review your content before publishing
- Use HTTPS in production
- Follow security best practices

## Performance Questions

### How fast is JavaSSG?

JavaSSG is designed for speed:
- Multi-level caching
- Parallel processing
- Incremental builds
- Memory-efficient processing

Benchmarks show it's competitive with other fast generators.

### How do I optimize build performance?

1. Use incremental builds: `--incremental`
2. Enable caching in configuration
3. Optimize images and assets
4. Use appropriate file sizes
5. Consider build parallelization

### How much memory does JavaSSG use?

Memory usage depends on:
- Site size
- Number of files
- Cache settings
- Concurrent operations

Typical usage: 100-500MB for medium sites.

## Troubleshooting

### Build fails with "OutOfMemoryError"

Increase memory allocation:
```bash
java -Xmx1g -jar javassg.jar build --working-directory ./my-site
```

### Development server won't start

Check:
- Port availability (default: 8080)
- File permissions
- Working directory path
- Configuration validity

### Templates not rendering correctly

Check:
- Template syntax
- Variable names
- Filter usage
- Template inheritance

### Live reload not working

Check:
- WebSocket support in browser
- Firewall settings
- Port availability
- Browser console for errors

## Contributing Questions

### How can I contribute?

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for detailed guidelines.

### Where do I report bugs?

Use the [Issues](https://github.com/yourusername/JavaSSG/issues) page.

### How do I request features?

Use [Discussions](https://github.com/yourusername/JavaSSG/discussions) for feature requests.

## License Questions

### What license does JavaSSG use?

JavaSSG is licensed under the MIT License. See [LICENSE](../../LICENSE) for details.

### Can I use JavaSSG commercially?

Yes! The MIT License allows commercial use.

### Can I modify and redistribute JavaSSG?

Yes! The MIT License allows modification and redistribution.

## Support

### Where can I get help?

- Check this FAQ
- Read the documentation
- Join discussions
- Report issues
- Contact maintainers

### Is there a community?

Yes! Join our discussions and connect with other users.

---

Still have questions? Feel free to ask in our [Discussions](https://github.com/yourusername/JavaSSG/discussions)!