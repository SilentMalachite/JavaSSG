# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-09-08

### Added
- Initial release of JavaSSG
- Java 21 based static site generator
- Multi-level caching system for optimal performance
- Comprehensive security features (XSS protection, path traversal protection)
- Markdown support with front matter using CommonMark
- Template engine with custom filters
- Live reload development server with WebSocket support
- Plugin architecture with built-in plugins:
  - Sitemap generation
  - RSS feed generation
  - HTML/CSS minification
  - Image optimization
  - Syntax highlighting
- Command-line interface with serve, build, and new commands
- YAML configuration support
- Type-safe configuration models
- Memory-safe WebSocket connections and file watching
- Comprehensive test suite with JUnit 5, Mockito, and AssertJ

### Fixed
- Resolved deprecated API usage in SiteInfo.java (java.net.URL constructor)
- Fixed type safety warnings in LiveReloadService.java
- Updated Maven configuration to use --release flag for Java 21 compatibility
- Eliminated all compilation warnings

### Security
- Input validation and sanitization
- Path traversal attack prevention
- XSS attack detection and prevention
- Safe template processing
- File size limitations to prevent DoS attacks
- Secure asset processing

### Performance
- Multi-level caching (content, template, rendering)
- Incremental build support
- Parallel processing capabilities
- Memory-optimized streaming processing
- Intelligent cache invalidation

### Documentation
- Comprehensive README.md with installation and usage instructions
- API documentation
- Security guidelines
- Performance optimization tips
- Plugin development guide

## [Unreleased]

### Planned
- Native application packaging with jpackage
- Additional template themes
- Enhanced plugin system
- Performance monitoring and metrics
- Internationalization support
- Advanced caching strategies