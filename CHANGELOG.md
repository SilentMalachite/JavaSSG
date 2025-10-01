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

## [1.0.1] - 2025-01-10

### Fixed
- ドキュメントの修正（CLAUDE.md: Swift→Maven、URL記述ミス削除）
- 不要な依存関係の削除（Pebble、Java-WebSocket）
- InputSanitizerをSecurityValidatorに統合してセキュリティ実装を一本化
- ConfigLoaderのエラー処理を一貫性のあるものに改善
- DevServerにAtomicIntegerのインポート追加
- MarkdownParserのスラグ生成でアンダースコアをハイフンに変換
- MarkdownParserのフロントマターサイズ検証を実際に実行
- SecurityValidatorでimg自己終了タグをサポート
- ServeCommandテストでエラーストリームを正しくキャプチャ
- テストの安定性向上（待機時間の調整）

### Changed
- JaCoCo設定でJava 21クラスファイルを除外
- ビルドコマンドにファイル監視機能を追加
- 統合テストのタイミング問題を改善

### Removed
- 未使用のInputSanitizerクラスを削除
- 未使用のPebbleテンプレートエンジン依存関係
- 未使用のJava-WebSocket依存関係

## [Unreleased]

### Planned
- Native application packaging with jpackage
- Additional template themes
- Enhanced plugin system
- Performance monitoring and metrics
- Internationalization support
- Advanced caching strategies