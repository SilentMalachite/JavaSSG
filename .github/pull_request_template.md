# プルリクエスト

## 📋 概要

このプルリクエストの概要を簡潔に説明してください。

Closes #(issue番号)

## 🔄 変更の種類

該当するものにチェックを入れてください：

- [ ] 🐛 バグ修正（後方互換性のある修正）
- [ ] ✨ 新機能（後方互換性のある機能追加）
- [ ] 💥 破壊的変更（既存の機能に影響する修正や機能）
- [ ] 📝 ドキュメント更新
- [ ] 🎨 スタイル修正（フォーマット、セミコロン不足など、機能に影響しない変更）
- [ ] ♻️ リファクタリング（バグ修正や機能追加ではないコード変更）
- [ ] ⚡ パフォーマンス改善
- [ ] ✅ テスト追加・修正
- [ ] 🔧 ビルドシステムや外部依存関係の変更
- [ ] 🚀 CI/CD設定の変更

## 📝 変更内容

### 主な変更点

- 変更点1の詳細説明
- 変更点2の詳細説明
- 変更点3の詳細説明

### 技術的詳細

```java
// 重要なコード変更がある場合、ここに例を示してください
public class Example {
    public void newMethod() {
        // 新しい実装
    }
}
```

## 🧪 テスト

### テスト項目

- [ ] 既存のテストがすべて通ることを確認済み
- [ ] 新しいテストを追加済み
- [ ] 手動テストを実行済み
- [ ] セキュリティテストを実行済み（該当する場合）
- [ ] パフォーマンステストを実行済み（該当する場合）

### テストの詳細

```bash
# 実行したテストコマンド
mvn test
mvn test -Dtest=SpecificTest

# 手動テストの内容
java -jar target/javassg-*.jar build --test-option
```

### テスト結果

- ✅ 全単体テスト: 通過
- ✅ 統合テスト: 通過  
- ✅ セキュリティテスト: 通過
- ✅ 手動テスト: 通過

## 🔒 セキュリティ

セキュリティに関する変更がある場合はチェックしてください：

- [ ] 入力検証の追加・修正
- [ ] 出力エスケープの追加・修正
- [ ] 認証・認可の変更
- [ ] 暗号化の追加・修正
- [ ] セキュリティヘッダーの変更
- [ ] 脆弱性の修正

### セキュリティレビュー

- [ ] セキュリティガイドラインに準拠している
- [ ] 新しい攻撃ベクターを導入していない
- [ ] 適切な入力検証を実装している
- [ ] 機密情報を適切に処理している

## 📊 パフォーマンス

パフォーマンスに影響する変更がある場合：

### ベンチマーク結果

| 項目 | 変更前 | 変更後 | 改善 |
|------|--------|--------|------|
| ビルド時間 | 5.2秒 | 3.8秒 | +27% |
| メモリ使用量 | 256MB | 198MB | +23% |

### パフォーマンステスト

- [ ] ベンチマークテストを実行済み
- [ ] メモリリークテストを実行済み
- [ ] 大規模データでのテストを実行済み

## 📚 ドキュメント

- [ ] コードの変更に応じてドキュメントを更新済み
- [ ] README.mdを更新済み（該当する場合）
- [ ] CHANGELOGを更新済み
- [ ] APIドキュメント（JavaDoc）を更新済み
- [ ] 新機能の使用例を追加済み

## ✅ チェックリスト

実装完了前に以下を確認してください：

### コード品質

- [ ] コーディング規約に従っている
- [ ] 適切なコメントとJavaDocを記述している
- [ ] エラーハンドリングを適切に実装している
- [ ] ログ出力を適切に設定している
- [ ] 依存関係を最小限に抑えている

### 互換性

- [ ] 後方互換性を維持している
- [ ] API変更がある場合は適切にドキュメント化している
- [ ] 設定ファイルの変更がある場合は移行ガイドを提供している

### 品質保証

- [ ] 静的解析ツールでチェック済み
- [ ] コードレビューガイドラインに従っている
- [ ] エッジケースを考慮している
- [ ] 国際化対応を考慮している（該当する場合）

## 🚀 デプロイメント

本番環境への影響がある場合：

### デプロイ前の確認事項

- [ ] 設定変更が必要な場合は文書化済み
- [ ] データベース移行が必要な場合は手順を文書化済み
- [ ] 段階的ロールアウトが可能
- [ ] ロールバック手順を確認済み

### 影響範囲

- [ ] 既存ユーザーへの影響なし
- [ ] 既存の設定ファイルとの互換性あり
- [ ] 既存のプラグインとの互換性あり

## 🤝 レビュー指針

レビュアーの方へ：以下の点を重点的にレビューしてください

### 重要なレビューポイント

1. **セキュリティ**: 新しい脆弱性が導入されていないか
2. **パフォーマンス**: パフォーマンスの劣化がないか
3. **テスト**: 適切なテストカバレッジが確保されているか
4. **ドキュメント**: 変更に応じた文書更新が行われているか
5. **互換性**: 後方互換性が維持されているか

### 特に注意して確認してほしい箇所

```
src/main/java/com/javassg/[変更したパッケージ]/
src/test/java/com/javassg/[対応するテスト]/
docs/[関連ドキュメント]
```

## 📞 質問・議論

このプルリクエストについて質問や議論したい点があれば、お気軽にコメントしてください。

### 既知の課題

- [ ] 解決済みの問題はここから削除してください
- [ ] まだ解決していない問題があれば記載してください

### 今後の改善予定

- 次のバージョンで対応予定の改善点があれば記載

---

**レビューのお願い**: このプルリクエストのレビューをお願いします。特に [セキュリティ/パフォーマンス/API設計] の観点からの意見をお聞かせください。