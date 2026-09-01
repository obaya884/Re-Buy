# Re-Buy（仮称）

くりかえし使える買い物リスト。品目を使い捨てにせず、買い物のたびにカゴへ入れ → 店でチェック → 終了でプールに戻す。一人で買い物する人のための、端末内で完結するアプリ。

- [要求定義書](./docs/仕様/11_要求定義書.md) — 何のために・誰のために・何をしないか
- [要件バックログ](./docs/案件/22_要件バックログ.md)・[技術改善バックログ](./docs/案件/23_技術改善バックログ.md) — 現在地（現況節）と未実装の案件

## セットアップ

clone したら 1 回だけ実行する。設定しないと pre-commit フックが**エラーも出さずに無効**になる（[git 運用定義書](./docs/仕様/16_git運用定義書.md) §1.4）。

```sh
git config core.hooksPath .githooks
```

## ビルド

JDK 17 以上と Android SDK（compileSdk 37）が必要。

```sh
./gradlew assembleDebug                 # デバッグビルド
./gradlew build                         # lint・unit test 込み
./gradlew pixel6Api35DebugAndroidTest   # インストルメンテーションテスト（Gradle Managed Device）
```

## 開発体制

Claude Code による AI 主導開発。運用は [CLAUDE.md](./CLAUDE.md)、仕様は `docs/` が正。

## ライセンス

本リポジトリのソースコードおよびドキュメントは閲覧のために公開している。**複製・改変・再配布・商用利用は許諾しない。** 著作権は obaya884 が保持する。Pull Request は受け付けない。
