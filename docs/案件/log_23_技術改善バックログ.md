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
| 2026-08-30 | アイコンは JetBrains の凍結版 `material-icons-core` 1.7.3 を `commonMain` に引く | CMP に対応物が無く `compose.material3` も推移的に引かない。JB 版の android バリアントは androidx 1.7.6 を要求するだけなので、BOM を外した `androidMain` では解決版が 1.7.8 から 1.7.6 へ下がる。**1.7.6 と 1.7.8 のソースを 281 ファイル突き合わせたところ違いは著作権表記の年だけ**で、パスデータは同一だった。9 個を `ImageVector` としてコピーする案は依存ゼロで両ターゲット完全一致になるが 300〜500 行増える。drawable 化する案は `android:autoMirrored` を CMP のパーサが読むので RTL は保てるものの、パスデータを手で移す誤写か Material Symbols から取り直して別の形になるかのどちらかになる。**`material-icons-core` 自体は非推奨ではない**——「必要なものをコピーせよ」は `material-icons-extended` の話（2026-08-30 オーナー判断）|
| 2026-08-30 | `navigation3-ui` だけ JetBrains のフォークを使う | `NavDisplay` を持つこの成果物だけ androidx が iOS 向けを publish していない（`navigation3-runtime` と `lifecycle-viewmodel-navigation3` はある）。JB 版の android バリアントは androidx への依存を宣言するだけのリダイレクトなので、Android に載るものは変わらない。Google の KMP 公開が追いついたら外す |
| 2026-08-30 | `Screen` を `sealed class` から `@Serializable sealed interface` にする | 開いた多相のサブクラス登録に `subclass(Screen.Xxx::class)` が要り、そのために各ルートが独立して `@Serializable` である必要がある。`sealed interface` なら `sealedSubclasses` で列挙できるので、登録漏れの検査も書ける |
| 2026-08-30 | 登録漏れの検査は `sealedSubclasses` で列挙する | 登録側と検査側で同じ表を二重に持つと、両方に足し忘れる形の漏れを見逃す。reflection が要るので JVM 側に置くが、守っているのは iOS |
| 2026-08-30 | `LicenseScreen` は画面ごとではなく中身だけを `expect/actual` にする | 画面ごと分けると TopAppBar・戻るボタン・testTag の 20 行が 2 つの actual に重複し、片方だけ直す事故が起きる（instrumented は Android しか見ない）。プラットフォーム差があるのは AboutLibraries の読み込みだけなので、`LicenseContent(modifier)` に絞った |
| 2026-08-30 | `kotlinx-serialization` は json ではなく core を引く | `@Serializable` / `SerializersModule` / `PolymorphicSerializer` はすべて core の API で、`kotlinx.serialization.json` の import はリポジトリに 1 つも無い。保存形式は savedstate が決める |
| 2026-08-30 | 「登録漏れは iOS だけで出る」という前提を捨てる | Android を救っていたのは `NavKeySerializer` の reflection 経路だけで、ステップ 13 でそれを使うのをやめたので差が消えた。登録を落として instrumented を回すと Android でも `SerializationException` で落ちることを実測。**壊れ方が揃ったぶん Android 側に網を張れる**ようになったので、instrumented に復元テストを 3 件置いた |
| 2026-08-30 | ライセンス一覧の元データはコミットする | AboutLibraries も SCM に入れる運用を挙げている。Room のスキーマと同じ扱いで、**依存を足し引きしたときの一覧の変化が diff に出る**のが利点。1 行 88KB では読めないので `prettyPrint` を入れて 136KB にした。コミットしてもタスク依存は要る——生成物がリソースのソースディレクトリの中に出るので、読む側に `dependsOn("exportLibraryDefinitions")` を明示する |
| 2026-08-30 | 生成物を `build/` へ逃がす案は取れなかった | `compose.resources { customDirectory(...) }` は既定のディレクトリを**足すのではなく置き換える**（CMP プラグインの `?:`）。`commonMain` に指定した瞬間に文言 49 件と drawable 3 件が一斉に `Unresolved reference` になった。生成物をソースツリーの外に置く道はここで閉じている |
| 2026-08-30 | 収集は `filterVariants` で `android` に絞る | 絞らないと 139 件になり、Android に載らない 5 件（`skiko` / `ui-uikit` / `kotlinx-browser` / `atomicfu` / `kotlinx-datetime`）が混ざる。`:androidApp` の `debugRuntimeClasspath` に 1 つも無いものを表示するのは正しくない。**バリアント名（`androidMain`）で書くと 0 件に戻る**ことは変異で実測した。iOS 側の欠落は T-39 |
| 2026-08-30 | `androidResources { enable = true }` は R を引かなくなっても外さない | `:shared:ui` の Kotlin から `R` を引く場所は無くなったが、**Compose Resources は Android では assets 経由で載り、CMP はその配線を `variant.sources.assets` に繋いでいる**。無効にすると assets ごと消え、ビルドは緑・APK に `assets/composeResources/` が 1 件も入らない・起動して全画面が落ちる。実際に踏んだ |
| 2026-08-30 | 一覧が空になる退行を instrumented で止める | ステップ 5 から 9 ステップぶん、この退行を検出できるのは「人が実機で一覧を見ること」だけだった。リソースを画面と同じ経路で読んで件数を見る 1 件と、実際に画面へ遷移して 1 行目が描かれるまで待つ 1 件を置いた。**リソースが読めることと画面に届いていることは別**なので 2 件に分けている |
| 2026-08-30 | ライセンス一覧の網は件数だけでなく中身も見る | 収集の壊れ方は 2 方向ある。**絞りすぎると 0 件**（Kotlin ターゲット名ではなく AGP のバリアント名で `filterVariants` を書いた場合）、**絞らないと Android に無いものが混ざる**（`skiko` や `ui-uikit` がメタデータ解決から入る）。件数の下限は前者しか止められないので、Android の classpath からしか出ない依存（`koin-android` / `room-runtime`）が入っていることと、`skiko` / `uikit` が入っていないことを別々に見る。厳密な件数は書かない——依存を足すたびに数字を書き換えるだけになって網でなくなる |
| 2026-08-30 | リソースのパスは実装側の定数を公開してテストに使わせる | `Res.readBytes` は文字列で引くのでタイプミスをコンパイラが止められない。テスト側にパスを書き写すと、書き写しどうしが一致するだけで実装の誤りを止められない。**キーで引いた先に物があるかを見るテスト**なので、値そのものの正しさを見る `StringResourceFormatTest`（リテラルで持つ）とは逆の判断になる |
