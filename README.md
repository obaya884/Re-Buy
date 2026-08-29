# Re-Buy（仮称）

くりかえし使える買い物リスト。品目を使い捨てにせず、買い物のたびにカゴへ入れ → 店でチェック → 終了でプールに戻す。一人で買い物する人のための、端末内で完結するアプリ。

- [憲章](./docs/仕様/11_憲章.md) — 何のために・誰のために・何をしないか
- [ロードマップ](./docs/案件/24_ロードマップ.md) — 現在地と、この先の順序

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
