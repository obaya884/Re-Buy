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
| 2026-08-30 | iOS でも setQueryCoroutineContext は Dispatchers.IO にする | Native の `Dispatchers.IO` は internal ではなく、`Dispatchers` のメンバではなく拡張プロパティ。`import kotlinx.coroutines.IO` が要るだけだった。「internal だから使えない」と誤診して coroutines の版を上げ下げしたが、原因は import の不足 |
| 2026-08-30 | iosMain には使っていない compose.* も書く | `compose.material3` が自分より古い `foundation` を推移的に引くため、未使用として外すと版が 1.12.0 から 1.9.1 へ下がる。CMP の互換検査の警告で気づいた |
| 2026-08-30 | タイムゾーン固定の検査だけ androidHostTest に残す | 既定タイムゾーンを差し替える API が common に無い。CI は TZ=UTC で走るので、この検査を捨てるとタイムゾーン依存の混入が見えなくなる。共通の 20 件とは別クラスにして 2 件だけ JVM 側に置いた |
| 2026-08-30 | iOS の DB は Documents ディレクトリに置く | バックアップ対象で、アプリを消すまで残る。Android の `getDatabasePath` と対になる場所 |
| 2026-08-30 | iosApp のビルド設定は Xcode の GUI ではなく Config.xcconfig で持つ | GUI で保存すると pbxproj 全体がキャノニカル形式に書き直され、差分が読めなくなる。構成（Debug / Release）で差の無い設定は xcconfig に置き、pbxproj には差のあるものだけ残す |
| 2026-08-30 | pbxproj の疑似 UUID は Xcode と同じ 24 桁にする | 22 桁でも Xcode は動くが、外部ツール（xcodeproj gem・XcodeGen・Tuist）は 24 桁前提の実装が多い。連番かつ AA01 プレフィックスなので衝突の危険はランダム生成より小さい |
| 2026-08-30 | iOS の framework は段 3 では debug だけ作る | 既定の debug ＋ release だと `./gradlew build` が 7 分 17 秒になり、release のリンクがヒープ 4GB を要求する。段 3 の目的はシミュレータで動かすことなので release は要らない。実機配布が要る段 4 で足す |
| 2026-08-30 | iOS ターゲットは iosArm64 と iosSimulatorArm64 の 2 つだけにする | iosX64 は Intel Mac のシミュレータ用で、開発機も GitHub の macOS ランナーも arm64。要るようになったら build-logic に 1 行足せばよい |
| 2026-08-30 | ライセンス一覧はステップ 5〜14 の間だけ空を許容する | AboutLibraries 15.2.0 の KMP 経路が AGP の KMP android ターゲットから依存を拾えず、`:shared:ui` を KMP 化すると 0 件になる。プラグインは最新版で上げる先が無い。生成済み json をソースツリーに凍結する案は、生成物をコミットすることになるうえ debug の表示件数が変わる。ステップ 14 でどのみち composeResources へ移すので、そこまで待つ |
| 2026-08-30 | `Version.kt` を生成するタスクは convention plugin ではなくモジュールに置く | 計画は convention plugin に置くと書いていたが、`rebuy.versionName` を要るのは `:shared:ui` だけ。単一利用のタスクを共有 plugin に置くと、どのモジュールに効くのかが読めなくなる |
| 2026-08-30 | `jvmTarget` の暫定対応を T-28b で convention plugin へ移した | モジュール側に書く形は、KMP 化する 2 モジュール目・3 モジュール目で同じ書き忘れが起きうる。書き忘れてもビルドもテストも緑のままなので、モジュールの記述に頼らない形にした |
| 2026-08-30 | Compose Resources の `Res` は `publicResClass = true` で公開する | `:androidApp` の instrumented テストが画面のタイトルを文言で突き合わせている。テスト側に文字列をハードコードすると、文言を変えたときにテストが古い文言を主張したまま落ちる。既定の internal では外から引けない |
| 2026-08-30 | `compose.components.resources` だけは commonMain に置く | 「`compose.*` は iosMain だけ」という規約の唯一の例外。リソースが commonMain にある以上ほかに置き場が無い。Android では JetBrains の別名アーティファクトが androidx へ解決するので BOM とはぶつからない（21 画面の画素比較で確認）|
| 2026-08-30 | Android の表示同一性は画素で確かめる | 文言 49 件のうち `\n` を含む 4 件と `%1$s` を含む 3 件は、Compose Resources が Android と同じに解釈する保証が無い。`pm clear` から決まった順に操作する走査を作り、移送の前後で 21 画面のスクリーンショットと `uiautomator` の dump を撮って突き合わせた |
| 2026-08-30 | 未使用の文言 12 件はそのまま運ぶ | ステップ 11 は移送であって整理ではない。消す判断は「その画面を作るのか」という要求の判断で、④ の機能見直しに属する。T-36 に登録した |
| 2026-08-30 | 固定する文言は 49 件のうち 6 件だけにする | `\n` を含む 4 件と `%1$s` を含む 3 件（重なりを除いて 6 件）は AAPT と CMP で解釈が分かれうる。残る 43 件は読み方が 1 通りしかなく、固定しても文言を変えるたびにテストを直す負債になるだけ |
| 2026-08-30 | 固定テストの期待値だけはリテラルで持つ | ほかのテストが文言を `Res.string.*` から引くのは実装と同じ正を見るためで正しいが、そのぶん値が壊れても画面とテストが同じ壊れた値を見て素通りする。リソースから引いた瞬間に自己参照に戻り、網でなくなる |
| 2026-08-30 | 段 3 のステップ 12 と 13 の境界を引き直す | 「12 = 画面、13 = ナビ基盤」の順は依存の向きと逆だった。画面 6 枚はすべて `Navigator` を引数に取り、`Screen` は `ReBuyApp.kt` の中にある。`commonMain` から `androidMain` は見えないので、ナビ基盤が先に移らないと画面は 1 枚も動かせない。ステップ数は増やさず、12 を「ナビに依存しない部品」に絞って画面をまとめて 13 へ送った |
| 2026-08-30 | `ReBuyTheme` の `dynamicColor` 分岐は `expect/actual` にせず削除する | 既定が `false` で呼び出し側も指定しておらず、一度も通っていなかった。使うと決めたときに足すほうが、通らない分岐を 2 プラットフォームぶん抱えるより読める |
| 2026-08-30 | 日付書式の期待値はロケールとタイムゾーンを固定してリテラルで書く | `DateTimeFormatter` から期待値を作ると自己参照になって網でなくなる。固定書式へ「揃える」変更と UTC 固定への変更を変異で入れ、それぞれ 1 件ずつ落ちることを確認した |
