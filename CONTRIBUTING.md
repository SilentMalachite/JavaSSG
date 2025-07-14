# コントリビューションガイド

JavaSSGプロジェクトへのコントリビューションに興味を持っていただき、ありがとうございます！このガイドでは、プロジェクトに貢献する方法について説明します。

## 📋 目次

- [行動規範](#行動規範)
- [コントリビューションの種類](#コントリビューションの種類)
- [開発環境のセットアップ](#開発環境のセットアップ)
- [コーディング規約](#コーディング規約)
- [プルリクエストの流れ](#プルリクエストの流れ)
- [テストについて](#テストについて)
- [ドキュメントについて](#ドキュメントについて)
- [質問・サポート](#質問サポート)

## 行動規範

このプロジェクトでは、すべての参加者に対して敬意を持った行動を期待しています：

- 建設的で丁寧なコミュニケーションを心がける
- 異なる視点や経験を尊重する
- 批判は人ではなく、コードやアイデアに対して行う
- 学習と成長の機会として捉える

## コントリビューションの種類

以下のような方法でプロジェクトに貢献できます：

### 🐛 バグ報告
- 詳細な再現手順
- エラーメッセージやログ
- 環境情報（OS、Javaバージョンなど）

### 💡 機能要望
- 新機能の詳細な説明
- 使用例やユースケース
- 既存の代替手段との比較

### 📝 ドキュメント改善
- README、チュートリアル、APIドキュメントの改善
- 誤字脱字の修正
- 新しい例やサンプルの追加

### 🔧 コード貢献
- バグ修正
- 新機能の実装
- パフォーマンス改善
- テストの追加

## 開発環境のセットアップ

### 必要な環境

```bash
# Java 21の確認
java -version

# Mavenの確認
mvn -version
```

### リポジトリのクローンとセットアップ

```bash
# フォークしたリポジトリをクローン
git clone https://github.com/yourusername/JavaSSG.git
cd JavaSSG

# 依存関係のインストール
mvn clean install

# テストの実行
mvn test

# 動作確認
mvn exec:java -Dexec.mainClass="com.javassg.JavaSSG" -Dexec.args="--help"
```

### IDEの設定

#### IntelliJ IDEA
1. プロジェクトをMavenプロジェクトとして開く
2. Java 21が設定されていることを確認
3. コードフォーマッター設定をインポート（`.idea/codeStyles/`）

#### VS Code
1. Java Extension Packをインストール
2. `settings.json`で以下を設定：
```json
{
  "java.home": "/path/to/java21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java21"
    }
  ]
}
```

## コーディング規約

### Java コーディングスタイル

```java
// クラス名：パスカルケース
public class HtmlGenerator {

    // 定数：SCREAMING_SNAKE_CASE
    private static final String DEFAULT_TEMPLATE = "default";
    
    // フィールド：キャメルケース
    private final SiteConfig siteConfig;
    
    // メソッド名：キャメルケース
    public String generatePageHtml(Page page) {
        // ローカル変数：キャメルケース
        String templateName = getTemplateName(page);
        return processTemplate(templateName, page);
    }
}
```

### 重要な原則

1. **セキュリティファースト**: すべての入力を検証し、出力をサニタイズ
2. **型安全性**: できる限り型安全なコードを書く
3. **null安全性**: Optional型を活用し、nullチェックを徹底
4. **不変性**: できる限りimmutableなオブジェクトを使用
5. **テスタビリティ**: テストしやすい設計を心がける

### コメントとドキュメント

```java
/**
 * ページのHTMLを生成します。
 * 
 * @param page 変換するページオブジェクト
 * @param templateName 使用するテンプレート名
 * @return 生成されたHTML文字列
 * @throws HtmlGenerationException HTML生成中にエラーが発生した場合
 */
public String generatePageHtml(Page page, String templateName) throws HtmlGenerationException {
    // テンプレートの取得と検証
    Template template = getTemplate(templateName);
    
    // セキュリティ検証
    securityValidator.validateContent(page.rawContent());
    
    return renderTemplate(template, page);
}
```

## プルリクエストの流れ

### 1. Issue の作成・確認

新機能やバグ修正を行う前に、関連するIssueが存在するか確認してください。存在しない場合は、新しいIssueを作成して議論してください。

### 2. ブランチの作成

```bash
# 最新のmainブランチをフェッチ
git checkout main
git pull upstream main

# 機能ブランチを作成
git checkout -b feature/new-template-engine
# または
git checkout -b fix/security-vulnerability
```

### 3. 開発とコミット

```bash
# 変更を加える
# ...

# テストを実行
mvn test

# 変更をコミット
git add .
git commit -m "feat: 新しいテンプレートエンジンのサポートを追加

- Mustacheテンプレートエンジンの統合
- 既存のテンプレートとの互換性を維持
- セキュリティ検証の強化

Closes #123"
```

### 4. プルリクエストの作成

プルリクエストのテンプレート：

```markdown
## 概要
この変更の概要を説明してください。

## 変更内容
- [ ] 新機能の追加
- [ ] バグ修正
- [ ] ドキュメント更新
- [ ] テスト追加
- [ ] リファクタリング

## テスト
- [ ] 既存のテストがすべて通ることを確認
- [ ] 新しいテストを追加（該当する場合）
- [ ] 手動テストを実行（該当する場合）

## チェックリスト
- [ ] コーディング規約に従っている
- [ ] セキュリティ要件を満たしている
- [ ] ドキュメントを更新している（該当する場合）
- [ ] 後方互換性を維持している

## 関連Issue
Closes #(issue番号)
```

## テストについて

### テストの種類

1. **単体テスト**: 個別のクラス・メソッドのテスト
2. **統合テスト**: 複数のコンポーネント間の連携テスト
3. **セキュリティテスト**: セキュリティ機能の検証
4. **パフォーマンステスト**: 性能要件の確認

### テストの書き方

```java
@DisplayName("SecurityValidator Tests")
class SecurityValidatorTest {

    @BeforeEach
    void setUp() {
        validator = new SecurityValidator(SecurityLimits.defaultLimits());
    }

    @Test
    @DisplayName("XSS攻撃を検出すること")
    void shouldDetectXssAttack() {
        String maliciousContent = "<script>alert('xss')</script>";
        
        assertThatThrownBy(() -> validator.validateContent(maliciousContent))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");
    }
}
```

### テストの実行

```bash
# 全テストの実行
mvn test

# 特定のテストクラスの実行
mvn test -Dtest=SecurityValidatorTest

# 特定のテストメソッドの実行
mvn test -Dtest=SecurityValidatorTest#shouldDetectXssAttack

# カバレッジレポートの生成
mvn jacoco:report
```

## ドキュメントについて

### ドキュメントの種類

- **README.md**: プロジェクトの概要とクイックスタート
- **API ドキュメント**: JavaDocによる詳細なAPI説明
- **チュートリアル**: ステップバイステップのガイド
- **設定リファレンス**: 設定オプションの詳細

### ドキュメントの更新

コードの変更に伴って、関連するドキュメントも更新してください：

```bash
# JavaDocの生成
mvn javadoc:javadoc

# サイトドキュメントの生成
mvn site
```

## 質問・サポート

### 質問する前に

1. [FAQ](docs/faq.md)を確認する
2. 既存のIssueやDiscussionsを検索する
3. ドキュメントを確認する

### 質問の仕方

良い質問の例：

```markdown
## 環境
- OS: macOS 14.0
- Java: 21.0.1
- JavaSSG: 1.0.0

## 問題
カスタムテンプレートエンジンを作成したいのですが、既存のTemplateEngineインターフェースを拡張する方法がわかりません。

## 試したこと
1. TemplateEngineインターフェースを調査
2. 既存の実装（SimpleTemplateEngine）を参考にした
3. ドキュメントを確認した

## 期待する結果
Mustacheテンプレートエンジンを統合したい

## 実際の結果
コンパイルエラーが発生する

## エラーメッセージ
```
java.lang.ClassCastException: ...
```
```

### サポートチャンネル

- **GitHub Issues**: バグ報告・機能要望
- **GitHub Discussions**: 一般的な質問・議論
- **メール**: セキュリティに関する報告（security@example.com）

## リリースプロセス

### バージョニング

[Semantic Versioning](https://semver.org/)に従います：

- **MAJOR**: 後方互換性のない変更
- **MINOR**: 後方互換性のある機能追加
- **PATCH**: 後方互換性のあるバグ修正

### リリースの流れ

1. 機能の完成とテスト
2. ドキュメントの更新
3. CHANGELOGの更新
4. バージョンタグの作成
5. GitHub Releaseの作成

## 謝辞

JavaSSGプロジェクトへのコントリビューションに感謝します！あなたの貢献により、より良いツールを作ることができます。

---

何か質問がある場合は、お気軽にIssueやDiscussionで質問してください。