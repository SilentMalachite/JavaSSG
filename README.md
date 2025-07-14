# JavaSSG

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)

Java21で構築された、モダンで高速、かつセキュアな静的サイトジェネレーターです。
Swiftで構築した　![Hirundo](https://github.com/SilentMalachite/Hirundo)　のJavaバージョンです

## ✨ 主な機能

- **🚀 高速**: マルチレベルキャッシング付きJavaによる最適なパフォーマンス
- **🔒 セキュア**: 包括的な入力検証、パストラバーサル保護、安全なアセット処理
- **📝 Markdown**: フロントマター付きCommonMarkサポート
- **🎨 テンプレート**: カスタムフィルター付きのテンプレートエンジン
- **🔄 ライブリロード**: リアルタイムエラー報告機能付き自動再構築開発サーバー
- **🧩 拡張可能**: セキュア検証付きカスタム機能プラグインアーキテクチャ
- **💾 スマートキャッシング**: 超高速再構築のためのインテリジェント無効化キャッシング
- **📦 型安全**: 包括的検証付きの強く型付けされた設定とモデル
- **⚡ 設定可能**: カスタマイズ可能なセキュリティ制限とパフォーマンス設定
- **🛡️ メモリ安全**: WebSocket接続とファイル監視の高度なメモリ管理

## 🔧 技術要件

- **Java**: 21以上
- **Maven**: 3.9以上
- **対応OS**: macOS 13+、Windows 10〜11、Linux

## 📦 インストール

### 1. リポジトリのクローン

```bash
git clone https://github.com/yourusername/JavaSSG.git
cd JavaSSG
```

### 2. ビルド

```bash
mvn clean package
```

### 3. インストール（オプション）

```bash
mvn install
```

## 🚀 クイックスタート

### 1. 新しいサイトの作成

```bash
# プロジェクトディレクトリを作成
mkdir my-site
cd my-site

# 基本構造を作成
mkdir -p content/posts templates static
```

### 2. 設定ファイルの作成

`config.yaml`を作成：

```yaml
site:
  title: "私のサイト"
  description: "JavaSSGで作成されたサイトです"
  url: "https://example.com"
  language: "ja-JP"
  author:
    name: "あなたの名前"
    email: "your-email@example.com"

build:
  contentDirectory: "content"
  outputDirectory: "_site"
  staticDirectory: "static"
  templatesDirectory: "templates"

server:
  port: 8080
  liveReload: true

blog:
  postsPerPage: 10
  generateArchive: true
  generateCategories: true
  generateTags: true
```

### 3. コンテンツの作成

`content/index.md`を作成：

```markdown
---
title: "ホーム"
description: "サイトのホームページです"
---

# ようこそ

JavaSSGへようこそ！このサイトは高速で安全な静的サイトジェネレーターで構築されています。
```

`content/posts/first-post.md`を作成：

```markdown
---
title: "最初の記事"
date: 2024-01-01T10:00:00
categories:
  - ブログ
  - お知らせ
tags:
  - JavaSSG
  - Markdown
---

# 最初の記事

これは最初のブログ記事です。JavaSSGの機能を紹介していきます。

## 特徴

- 高速なビルド
- セキュアな処理
- 使いやすいMarkdown記法
```

### 4. テンプレートの作成

`templates/base.html`を作成：

```html
<!DOCTYPE html>
<html lang="{{ site.language }}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ title }} - {{ site.title }}</title>
    <meta name="description" content="{{ description }}">
</head>
<body>
    <header>
        <h1><a href="/">{{ site.title }}</a></h1>
        <p>{{ site.description }}</p>
    </header>
    
    <main>
        {{ content }}
    </main>
    
    <footer>
        <p>&copy; 2024 {{ site.author.name }}</p>
    </footer>
</body>
</html>
```

### 5. ビルドと実行

```bash
# 開発サーバーの起動
java -jar target/javassg-1.0.0.jar serve

# または、本番用ビルド
java -jar target/javassg-1.0.0.jar build --production
```

## 📋 使用方法

### 基本コマンド

#### 開発サーバーの起動
```bash
javassg serve [オプション]
```

- `--port <ポート番号>`: サーバーポートを指定（デフォルト: 8080）
- `--no-livereload`: ライブリロードを無効化

#### 本番用ビルド
```bash
javassg build [オプション]
```

- `--production`: 本番用最適化ビルド
- `--drafts`: 下書き記事を含める
- `--clean`: クリーンビルド
- `--incremental`: 増分ビルド
- `--verbose`: 詳細ログ出力

#### 新規コンテンツの作成
```bash
# ブログ記事の作成
javassg new post "記事タイトル"

# ページの作成
javassg new page "ページタイトル"
```

### プロジェクト構造

```
my-site/
├── config.yaml          # サイト設定
├── content/             # Markdownコンテンツ
│   ├── index.md        # ホームページ
│   ├── about.md        # Aboutページ
│   └── posts/          # ブログ記事
│       ├── first-post.md
│       └── second-post.md
├── templates/           # HTMLテンプレート
│   ├── base.html       # ベースレイアウト
│   ├── page.html       # ページテンプレート
│   └── post.html       # 記事テンプレート
├── static/             # 静的アセット
│   ├── css/           # スタイルシート
│   ├── js/            # JavaScript
│   └── images/        # 画像
└── _site/             # 生成された出力（gitignore対象）
```

## 🔌 プラグインシステム

JavaSSGは拡張可能なプラグインシステムを提供しています：

### 組み込みプラグイン

- **sitemap**: sitemap.xml生成
- **rss**: ブログのRSSフィード生成
- **minify**: HTML出力の最小化
- **imageOptimization**: 画像最適化とレスポンシブ画像作成
- **syntaxHighlight**: 拡張コードシンタックスハイライト

### プラグインの設定

```yaml
plugins:
  - name: "sitemap"
    enabled: true
  - name: "rss"
    enabled: true
    settings:
      maxItems: 20
  - name: "minify"
    enabled: true
    settings:
      minifyHTML: true
      minifyCSS: true
      minifyJS: false
```

## 🛡️ セキュリティ機能

JavaSSGは包括的なセキュリティ機能を提供：

- **XSS攻撃防止**: 危険なスクリプトタグとイベントハンドラーの検出
- **パストラバーサル攻撃防止**: ファイルパスの厳密な検証
- **入力サニタイゼーション**: コンテンツの安全な処理
- **ファイルサイズ制限**: DoS攻撃の防止
- **安全なテンプレート処理**: インジェクション攻撃の防止

### セキュリティ制限の設定

```yaml
limits:
  maxMarkdownFileSize: 10485760     # 10MB
  maxConfigFileSize: 1048576        # 1MB
  maxFrontMatterSize: 100000        # 100KB
  maxFilenameLength: 255
  maxTitleLength: 200
  maxDescriptionLength: 500
```

## 🎨 テンプレート

### 利用可能な変数

- `site`: サイト設定とメタデータ
- `page`: 現在のページデータ
- `pages`: 全ページ
- `posts`: 全ブログ記事
- `categories`: カテゴリーマップ
- `tags`: タグマップ
- `content`: レンダリングされたページコンテンツ

### カスタムフィルター

- `date`: 日付フォーマット
- `slugify`: URLスラグ作成
- `excerpt`: 抜粋抽出
- `absolute_url`: 絶対URL作成
- `markdown`: Markdownレンダリング

## 🔧 開発

### 開発環境のセットアップ

```bash
# リポジトリのクローン
git clone https://github.com/yourusername/JavaSSG.git
cd JavaSSG

# 依存関係のインストール
mvn clean install

# テストの実行
mvn test

# 開発サーバーの起動
mvn exec:java -Dexec.mainClass="com.javassg.JavaSSG" -Dexec.args="serve"
```

### テスト

```bash
# 全テストの実行
mvn test

# 特定のテストクラスの実行
mvn test -Dtest=SecurityValidatorTest

# カバレッジレポートの生成
mvn jacoco:report
```

## 📈 パフォーマンス

JavaSSGは以下のパフォーマンス最適化を提供：

- **マルチレベルキャッシング**: コンテンツ、テンプレート、レンダリング結果の効率的なキャッシュ
- **増分ビルド**: 変更されたファイルのみを再処理
- **並列処理**: マルチコアCPUを活用した高速処理
- **メモリ最適化**: ストリーミング処理による低メモリ使用量

## 🤝 コントリビューション

コントリビューションを歓迎します！詳細は[CONTRIBUTING.md](CONTRIBUTING.md)をご覧ください。

### バグ報告

バグを発見した場合は、[Issues](https://github.com/yourusername/JavaSSG/issues)で報告してください。

### 機能要望

新機能のアイデアがある場合は、[Discussions](https://github.com/yourusername/JavaSSG/discussions)で議論してください。

## 📄 ライセンス

このプロジェクトは[MIT License](LICENSE)の下で公開されています。

## 🙏 謝辞

- [CommonMark](https://commonmark.org/) - Markdown処理
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) - YAML解析
- [Jackson](https://github.com/FasterXML/jackson) - JSON処理
- [SLF4J](http://www.slf4j.org/) & [Logback](http://logback.qos.ch/) - ログ出力

## 📚 関連リンク

- [ドキュメント](docs/)
- [API リファレンス](docs/api/)
- [チュートリアル](docs/tutorials/)
- [FAQ](docs/faq.md)

---

**JavaSSG** - 高速、安全、使いやすい静的サイトジェネレーター
