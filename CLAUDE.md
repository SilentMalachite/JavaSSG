# JavaSSG プロジェクト

Java21で構築された、モダンで高速、かつセキュアな静的サイトジェネレーターです。

## 主な機能

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

## 技術スタック

- **言語**: Java21
- **対応OS**: macOS 13+、Windows10〜11

## プロジェクト構造

```
my-site/
├── config.yaml          # サイト設定
├── content/            # Markdownコンテンツ
│   ├── index.md       # ホームページ
│   ├── about.md       # Aboutページ
│   └── posts/         # ブログ記事
├── templates/          # HTMLテンプレート
│   ├── base.html      # ベースレイアウト
│   ├── default.html   # デフォルトページテンプレート
│   └── post.html      # ブログ記事テンプレート
├── static/            # 静的アセット
│   ├── css/          # スタイルシート
│   ├── js/           # JavaScript
│   └── images/       # 画像
└── _site/            # 生成された出力（gitignore対象）
```

## 主要コマンド

### 開発サーバーの起動
```bash
javassg serve
```
- ポート: 8080（デフォルト）
- ライブリロード: 有効
- URL: `http://localhost:8080`

### 本番用ビルド
```bash
javassg build
```
- 出力先: `_site`ディレクトリ
- 環境: production/development
- オプション: --drafts（下書きを含める）、--clean（クリーンビルド）

### 新規コンテンツの作成
```bash
# ブログ記事の作成
javassg new post "記事タイトル"

# ページの作成
javassg new page "ページタイトル"
```

## アーキテクチャの特徴

### 1. パッケージ管理
- 単一のクリーンなPackage.swift
- 適切な依存関係管理

### 2. 型安全性
- 全体を通じた強い型付けモデル
- 包括的なエラー型とヘルプメッセージ

### 3. パフォーマンス
- マルチレベルキャッシング（パース済みコンテンツ、レンダリング済みページ、テンプレート）
- ストリーミングによる効率的なメモリ使用

### 4. セキュリティ
- パストラバーサル保護
- 入力検証とサニタイゼーション
- セキュアなファイルパーミッション

### 5. プラグインシステム
組み込みプラグイン：
- **sitemap**: sitemap.xml生成
- **rss**: ブログのRSSフィード生成
- **minify**: HTML出力の最小化
- **imageOptimization**: 画像最適化とレスポンシブ画像作成
- **syntaxHighlight**: 拡張コードシンタックスハイライト

## 設定ファイル（config.yaml）

```yaml
site:
  title: "サイトタイトル"
  description: "サイトの説明"
  url: "https://example.com"
  language: "ja-JP"
  author:
    name: "著者名"
    email: "email@example.com"

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

# セキュリティとパフォーマンス制限（オプション）
limits:
  maxMarkdownFileSize: 10485760     # 10MB
  maxConfigFileSize: 1048576        # 1MB
  maxFrontMatterSize: 100000        # 100KB
  maxFilenameLength: 255
  maxTitleLength: 200
  maxDescriptionLength: 500

# プラグイン設定（オプション）
plugins:
  - name: "sitemap"
    enabled: true
  - name: "rss"
    enabled: true
  - name: "minify"
    enabled: true
    settings:
      minifyHTML: true
      minifyCSS: true
      minifyJS: false  # 安全のため無効
```

## テンプレート変数

利用可能な変数：
- `site`: サイト設定とメタデータ
- `page`: 現在のページデータ
- `pages`: 全ページ
- `posts`: 全ブログ記事
- `categories`: カテゴリーマップ
- `tags`: タグマップ
- `content`: レンダリングされたページコンテンツ

カスタムフィルター：
- `date`: 日付フォーマット
- `slugify`: URLスラグ作成
- `excerpt`: 抜粋抽出
- `absolute_url`: 絶対URL作成
- `markdown`: Markdownレンダリング

## その他

- 国際化（i18n）サポート
- CSS/JS処理のためのアセットパイプライン
- 高度なキャッシング戦略
- カスタムプラグイン開発サポート
- 複数テーマサポート
