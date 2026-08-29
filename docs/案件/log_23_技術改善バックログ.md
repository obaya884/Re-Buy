# 技術改善バックログ 決定ログ

- [技術改善バックログ](./23_技術改善バックログ.md) の「なぜそうしたか」を日付順に積む。本文は現在の状態だけを持つ

| 日付 | 決定 | 理由 |
|---|---|---|
| 2026-08-29 | T-28a の未確認事項（埋め込み Kotlin で KGP をコンパイルできるか）は解消した | Gradle 9.7.1 の埋め込み Kotlin は 2.2 ではなく 2.4.0 で、KGP 2.4.10 と同じマイナーだった。KGP の型に触れる捨てファイルを実際にコンパイルして確認したので、退避策（`java-gradle-plugin` ＋ 素の `Plugin` クラス）は不要 |
| 2026-08-29 | convention plugin は `extensions.configure<CommonExtension>` 1 つで application と library の両方を設定する | AGP 9 の `CommonExtension` は型引数を持たなくなり、`compileSdk` も `defaultConfig` も 1 つの型から触れる。Gradle の拡張検索は登録型の上位型にも一致するので、`ApplicationExtension` と `LibraryExtension` を分けて扱う必要がない（実測で確認）|
| 2026-08-29 | build-logic では `dependencyResolutionManagement` を使う | 版の二重管理を避けてルートと同じ catalog を読むため。CLAUDE.md の「使わない」はルートの settings の話で、build-logic は独立したビルドなので影響しない |
