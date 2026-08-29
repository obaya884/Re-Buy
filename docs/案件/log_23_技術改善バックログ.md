# 技術改善バックログ 決定ログ

- [技術改善バックログ](./23_技術改善バックログ.md) の「なぜそうしたか」を日付順に積む。本文は現在の状態だけを持つ

| 日付 | 決定 | 理由 |
|---|---|---|
| 2026-08-29 | T-28a の未確認事項（埋め込み Kotlin で KGP をコンパイルできるか）は解消した | Gradle 9.7.1 の埋め込み Kotlin は 2.2 ではなく 2.4.0 で、KGP 2.4.10 と同じマイナーだった。KGP の型に触れる捨てファイルを実際にコンパイルして確認したので、退避策（`java-gradle-plugin` ＋ 素の `Plugin` クラス）は不要 |
| 2026-08-29 | convention plugin は `extensions.configure<CommonExtension>` 1 つで application と library の両方を設定する | AGP 9 の `CommonExtension` は型引数を持たなくなり、`compileSdk` も `defaultConfig` も 1 つの型から触れる。Gradle の拡張検索は登録型の上位型にも一致するので、`ApplicationExtension` と `LibraryExtension` を分けて扱う必要がない（実測で確認）|
| 2026-08-29 | build-logic では `dependencyResolutionManagement` を使う | 版の二重管理を避けてルートと同じ catalog を読むため。CLAUDE.md の「使わない」はルートの settings の話で、build-logic は独立したビルドなので影響しない |
| 2026-08-30 | `androidx.sqlite` は Room が引いてくる 2.6.2 に合わせ、lint の「2.7.0 がある」警告は残す | 版を上げると Room が内部で使う driver と実装が二重になりうる。警告が 1 件増えるより、Room が実際に解決する版と一致しているほうが読み手に正確 |
| 2026-08-30 | Compose Multiplatform は 1.12.0 を選ぶ | 最新の安定版。Kotlin 2.2.20 でビルドされているが、Kotlin コンパイラは古いメタデータを読めるので 2.4.10 から使える |
| 2026-08-30 | KMP 化したモジュールでは `jvmTarget` をモジュール側で指定する | `rebuy.android.base` の `compileOptions` は KMP モジュールに当たらず、書かないとバイトコード版がビルド環境の JDK で決まる。手元の JBR 25 と CI の Temurin 21 で別物が出る状態だった。T-28b で convention plugin へ運ぶまでの暫定 |
| 2026-08-30 | KMP の android ターゲットは `targets.withType` で型から引く | `KotlinMultiplatformAndroidLibraryTarget` は `KotlinTarget` を継承しているので `targets` から型で拾える。拡張名で引いて未チェックキャストする形より、DSL 名が変わったときにコンパイルで気づける。非 KMP 側を `extensions.configure<CommonExtension>` にしたのと同じ理屈 |
| 2026-08-30 | iOS の framework は段 3 では debug だけ作る | 既定の debug ＋ release だと `./gradlew build` が 7 分 17 秒になり、release のリンクがヒープ 4GB を要求する。段 3 の目的はシミュレータで動かすことなので release は要らない。実機配布が要る段 4 で足す |
| 2026-08-30 | iOS ターゲットは iosArm64 と iosSimulatorArm64 の 2 つだけにする | iosX64 は Intel Mac のシミュレータ用で、開発機も GitHub の macOS ランナーも arm64。要るようになったら build-logic に 1 行足せばよい |
| 2026-08-30 | ライセンス一覧はステップ 5〜14 の間だけ空を許容する | AboutLibraries 15.2.0 の KMP 経路が AGP の KMP android ターゲットから依存を拾えず、`:shared:ui` を KMP 化すると 0 件になる。プラグインは最新版で上げる先が無い。生成済み json をソースツリーに凍結する案は、生成物をコミットすることになるうえ debug の表示件数が変わる。ステップ 14 でどのみち composeResources へ移すので、そこまで待つ |
| 2026-08-30 | `Version.kt` を生成するタスクは convention plugin ではなくモジュールに置く | 計画は convention plugin に置くと書いていたが、`rebuy.versionName` を要るのは `:shared:ui` だけ。単一利用のタスクを共有 plugin に置くと、どのモジュールに効くのかが読めなくなる |
| 2026-08-30 | `jvmTarget` の暫定対応を T-28b で convention plugin へ移した | モジュール側に書く形は、KMP 化する 2 モジュール目・3 モジュール目で同じ書き忘れが起きうる。書き忘れてもビルドもテストも緑のままなので、モジュールの記述に頼らない形にした |
