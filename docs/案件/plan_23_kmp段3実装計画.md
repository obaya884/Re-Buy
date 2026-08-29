# KMP 化 段 3（KMP/CMP 移植）実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

- 作成日: 2026-08-29
- 位置づけ: [KMP 化検討](../検討/32_KMP化検討.md) §10 の**段 3（KMP/CMP 移植 ＋ iOS の薄い SwiftUI ホスト）**の実装手順。台帳エントリは T-31（[技術改善バックログ](./23_技術改善バックログ.md)）。build-logic の導入は T-28a / T-28b
- 前段: [段 2 実装計画](./archive_23_kmp段2実装計画.md)（モジュール分割）と T-29（パッケージ整理）。どちらも完了

**Goal:** `:shared:*` の 3 モジュールを KMP 化し、コードを `commonMain` へ移す。iOS シミュレータで全画面が動く。**Android の挙動は一切変えない。**

**Architecture:** 各モジュールで **「KMP 化」と「`commonMain` への移送」を別のコミットに分ける**。まず KMP モジュールにするが中身は全部 `androidMain` に置いたままにし、そのあとで少しずつ `commonMain` へ歩かせる。こうすると KMP 化のコミットでは同じコードが同じターゲットでコンパイルされるだけなので Android の挙動が構造的に変わりようがなく、どこで壊れても原因が「Gradle の構成」か「コードの移送」かに必ず切り分かる。

**Tech Stack:** Kotlin 2.4.10 / AGP 9.3.2 / Gradle 9.7.1 / Room 2.8.4（KMP は 2.7 で stable）/ Koin 4.1.0 / Compose Multiplatform / Navigation 3 / AboutLibraries 15.2.0 / Xcode

**Spec:** [docs/検討/32_KMP化検討.md](../検討/32_KMP化検討.md) §3〜§9・§11・§13

## Global Constraints

- **Android の挙動を変えない。** 画面・遷移・データの読み書き・**日付の表示文字列**・ライセンス一覧の中身を変えない
- **既存端末の DB を引き継ぐ。** DB ファイル名（`app_database`）とパスの決め方を変えない。変えると利用者のデータが消える
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17
- **`:androidApp` は `com.android.application` ＋ built-in Kotlin のまま。** `org.jetbrains.kotlin.android` を足さない。KMP 化するのは `:shared:*` だけで、そちらは KGP を明示適用するので built-in Kotlin は関与しない
- 依存は必ず `gradle/libs.versions.toml` 経由で追加する
- namespace = Kotlin package。`shared` は package にも namespace にも入れない（[KMP 化検討](../検討/32_KMP化検討.md) §3）
- git 運用: **1 ステップ 1 コミット**（PR は squash マージなので、`main` では 1 PR = 1 コミットにまとまる）。ブランチは分けてよいが、**ステップ 5 と 8 は途中で切らない**（KMP 化とデータ層移送は中断すると赤のまま止まる）
- 表のセル内に `|` を書かない

## 合否判定

**各ステップの完了条件に必ずテストの件数を書く。** 新しい Android KMP ライブラリプラグインは host test / device test が**既定で無効**で、`withHostTestBuilder {}` を書き忘れると `./gradlew build` は緑のままユニットテストが 0 件になる。件数を見ていないと 122 件が消えたことに気づけない。

最終的な形は次のとおり。

| 段階 | ユニット | instrumented |
|---|---|---|
| 着手前 | 122（`:shared:ui` 102 ＋ `:shared:data` 20） | 20（`:androidApp`） |
| 完了時 | 121 が `commonTest`（android と iosSimulatorArm64 の両方で走る）＋ 1 が `androidHostTest` | 20（変わらず） |

`commonTest` へ行けないのは `KoinModulesTest` 1 件だけ。`koin-test` の `verify()` が kotlin-reflect 依存で JVM 専用のため。

## iOS は 2 回出す

段取り（[KMP 化検討](../検討/32_KMP化検討.md) §10）が「iOS を後ろへ送らない」と決めた理由は、未確認事項が iOS で 1 回動かすまで潰せないからだった。ただし段 3 の最後に 1 回だけ出すと、**Xcode の配線が悪いのか CMP が悪いのかが同時に判明する**。

- **1 回目（ステップ 7）**: `ComposeUIViewController { Text("ReBuy") }` だけを出す。ここで潰れるのは Xcode プロジェクト・framework 埋め込み・Gradle 連携・シミュレータ起動という**アプリと無関係な層**
- **2 回目（ステップ 15）**: `ReBuyApp()` に差し替える。ここで出る問題は必ず CMP かアプリ側

追加コストは commit 1 本、得られるのは切り分け。

## 着手前に潰す未確認事項

優先順。実測で潰したものは結果を残す。

1. ~~Gradle の埋め込み Kotlin で KGP 2.4.10 を使う convention plugin をコンパイルできるか~~ → **できる**（2026-08-29）。Gradle 9.7.1 の埋め込み Kotlin は 2.2 ではなく **2.4.0** で、KGP と同じマイナーだった。退避策は不要
2. ~~AGP 9.3.2 の KMP ライブラリ DSL のブロック名と host test の source set 名~~ → **確定**（2026-08-30）。プラグイン id `com.android.kotlin.multiplatform.library`、DSL は **`kotlin { android { } }`**、host test は `withHostTest { }` で開き、source set は **`androidHostTest`**、テストタスクは **`testAndroidHostTest`**（`testDebugUnitTest` ではない）。`androidLibrary { }` も通るが**非推奨**——返る型に `@Deprecated("The 'androidLibrary' block is deprecated. Please use 'android' instead.")` が付いている。警告はビルドスクリプトのコンパイル時にしか出ず、Gradle がコンパイル済みスクリプトをキャッシュするので**タスク実行の出力には現れない**
3. ~~`kotlin.time.Instant` が Kotlin 2.4.10 で stable か~~ → **stable**（2026-08-30）。`Clock.System.now()` と `Instant.fromEpochMilliseconds()` を使う捨てファイルを `:shared:data` でコンパイルし、警告もエラーも出ないことを確認した。`@OptIn` の伝播は起きない
4. ~~`androidx.room` Gradle プラグインの `schemaDirectory` がバリアント別サブディレクトリを掘らないか~~ → **掘らない**（2026-08-30）。`2.json` を消して再ビルドすると同じ場所に同じ内容で戻る。`androidApp` の assets 指定は無変更でよい
5. ~~Compose Multiplatform のどの版が Kotlin 2.4.10 に対応するか~~ → **1.12.0**（2026-08-30）。Kotlin 2.2.20 でビルドされているが、Kotlin は古いメタデータを読めるので 2.4.10 から使える。Compose BOM 2026.08.00 との突き合わせはステップ 12 で行う

## build-logic（T-28a / T-28b）

### included build は root の `buildscript { classpath }` を継承しない

これが全ての前提。現状は root の `buildscript { classpath }` が KGP / KSP / serialization を、`plugins { ... apply false }` が AGP を root の buildscript classpath に載せており、子プロジェクトの classloader が root の子なので `:shared:ui` が `id("org.jetbrains.kotlin.plugin.serialization")` を**版なしで**書けている。

included build は**別の Gradle ビルド**なのでこの classpath を見ない。convention plugin のコンパイルに要る AGP / KGP は `build-logic/build.gradle.kts` で自前に宣言する。

**実行時に効くのは root の版。** convention plugin を適用すると build-logic の classpath が子プロジェクトの buildscript classpath に足されるが、その classloader は root の子なので root 側の版が先に見つかる。したがって **build-logic の AGP / KGP は catalog と同じ版に固定することが必須**——ずれると**コンパイルは通って実行時に `NoSuchMethodError`** になる。

### 形

```
build-logic/
  settings.gradle.kts   dependencyResolutionManagement {
                            versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
                        }
  build.gradle.kts      plugins { `kotlin-dsl` }
                        repositories { google(); mavenCentral(); gradlePluginPortal() }
                        dependencies { compileOnly(AGP); compileOnly(KGP) }
  src/main/kotlin/rebuy.android.base.gradle.kts など
```

`kotlin-dsl` は Gradle 同梱なので、build-logic 側の `pluginManagement { repositories }` は要らない。

ルート `settings.gradle.kts` の先頭に `pluginManagement { includeBuild("build-logic") }` を足す。

**CLAUDE.md の「`dependencyResolutionManagement` を使わない」は root の settings の話**で、build-logic は独立したビルドなので影響しない。ここでは使う——使わないと版が二重管理になる。

**`compileOnly` を使う。** 実行時は root の classpath が勝つので、`implementation` にすると build-logic の推移依存が子 classloader に流れ込み、どちらが効いているのか読めなくなる。ただし precompiled script plugin の `plugins {}` に書いた id は build-logic の `implementation` 依存として要求されるため、`compileOnly` と両立しない。そこで **convention plugin は AGP を適用せず、`plugins.withId(...)` で「適用されたら設定する」形にする**（モジュール側の `alias(libs.plugins.android.*)` は残る）。

AGP 9 の `CommonExtension` は型引数を持たないので、`extensions.configure<CommonExtension>` 1 つで application と library の両方に効く。**T-28a の実装はこの形**（`build-logic/src/main/kotlin/rebuy.android.base.gradle.kts`）。

**KMP 化するとこのプラグインは当たらなくなる。** `com.android.kotlin.multiplatform.library` は android 拡張を project ではなく kotlin 拡張の下に登録するため、`:shared:*` を KMP 化した時点で `rebuy.android.base` は何もしなくなる。`compileSdk` 未設定でビルドが落ちるので気づけるが、**T-28b では KMP ライブラリ用の口を足す**こと。

precompiled script plugin から `libs` を型安全に参照することはできない。`project.extensions.getByType<VersionCatalogsExtension>().named("libs")` で取るヘルパーを 1 本書く。

## ステップ

| # | ステップ | 完了条件 |
|---|---|---|
| 1 | **build-logic を included build として立てる（T-28a）** | `./gradlew build` 緑。`:shared:*` 3 本と `:androidApp` から `repositories {}` と `compileOptions {}` が消えている。ユニット 122・instrumented 20 が緑。`--configuration-cache` の 2 回目が `reused`。build-logic の AGP / KGP が catalog と同じ版で宣言されている |
| 2 | **catalog に KMP / CMP / Room-KMP / sqlite-bundled の版を足す（適用はしない）** | `./gradlew build` 緑。生成物に差分なし。root の `plugins { ... apply false }` に KMP と CMP と Android-KMP-library が並んでいる |
| 3 | **`:shared:data` を KMP 化（android ターゲットのみ、コードは全部 `androidMain`）** | `:shared:data` のホストテストが**20 件**緑。`./gradlew build` 緑。`shared/data/schemas/**/2.json` の場所と中身が移行前と一致。instrumented 20 件緑。**`.claude/settings.json` の allow をここで追随させる**（タスク名が変わるので、待つと以降 12 commit ぶん許可プロンプトを踏む） |
| 4 | **ターゲット定義を convention plugin へ抽出（T-28b）** | `./gradlew build` 緑。テスト件数不変。`shared/data/build.gradle.kts` からターゲット定義が消えている。**`jvmTarget` とホストテストの有効化も一緒に運ぶ**——運ばないとステップ 5 で `:shared:ui` と `:shared:domain` が落とし穴 1 と 6 を踏む |
| 5 | **`:shared:domain` と `:shared:ui` を KMP 化（`androidMain` のまま）** | Android 同一挙動（**設定画面のバージョン表示が `0.0.1`**）。ホストテスト **102 件**緑。instrumented 20 件緑。`BuildConfig` → 生成 `Version.kt`。**唯一の例外がライセンス画面**——落とし穴 17 でステップ 14 まで空になる |
| 6 | **3 モジュールに iOS ターゲットを足し、`:shared:ui` の `iosMain` に framework と `Text` 1 個のスタブを置く** | `./gradlew :shared:ui:linkDebugFrameworkIosSimulatorArm64` が通る。Android 側のテスト件数と挙動は不変。**`./gradlew build` の所要時間が段 3 着手前と同程度**——framework を debug に絞らないと 2 倍以上になる |
| 7 | **`iosApp/` の Xcode プロジェクトを置き、シミュレータで Compose の 1 画面を出す** | **iOS シミュレータに `Text` が出る**。`.gitignore` に Xcode の生成物（`iosApp/build/` `xcuserdata/`）が入り、`project.pbxproj` はコミットされている。Android 無変更 |
| 8 | **`:shared:data` を `commonMain` へ** | Converter テスト **20 件が `commonTest` で android と iosSimulatorArm64 の両方緑**。**`ItemDao` / `CategoryDao` が `commonMain` にある**——ここに無いと `FakeDatabase` がステップ 10 で `commonTest` へ行けず往復になる。Android 同一挙動。**既存端末の DB が引き継がれる**（アップグレードインストールで手動確認）。instrumented 20 件緑 |
| 9 | **`:shared:domain` を `commonMain` へ** | `./gradlew build` 緑。両ターゲットでコンパイル。テスト件数不変 |
| 10 | **`:shared:ui` の非 UI を `commonMain` へ、テストを `commonTest` へ** | **101 件が `commonTest` で両ターゲット緑**、`KoinModulesTest` 1 件が `androidHostTest` で緑。合計 122 件。**アサーションの中身は 1 行も変えていない** |
| 11 | **リソースを Compose Resources へ（文言 49 件 ＋ drawable 3 件）** | Android の表示・文言が完全に同一。instrumented 20 件緑。`shared/ui/src/main/res` が消えている |
| 12 | **theme と画面 Composable を `commonMain` へ** | Android 同一挙動。**「最終購入」の日付表示が移行前と 1 文字も違わない**。instrumented 20 件緑 |
| 13 | **Navigation 3 を CMP 対応に（`SavedStateConfiguration` ＋ 多相シリアライズ）、`ReBuyApp` を `commonMain` へ** | Android 同一挙動。**プロセス death からの復元が移行前と同じ**（開発者オプションの「アクティビティを保持しない」で手動確認）。`NavigatorTest` 11 件が両ターゲット緑 |
| 14 | **AboutLibraries を composeResources 経由に** | **ライセンス一覧に 131 件以上が出る**（ステップ 5 で 0 件になった退行がここで戻る。落とし穴 17）。**同じことを見る instrumented テストを 1 本足す**——いまこの退行を検出できるのは人がこの表を読むことだけなので、戻したら機械の網に載せる。Android のライセンス一覧の表示が移設前と同一。生成物の commit / ignore 方針が決まり、タスク依存が明示されている |
| 15 | **`iosMain` のスタブを `ReBuyApp()` に差し替え、Koin を起動する** | **iOS シミュレータで全画面が動く**（ホーム・買い物・設定・カテゴリー編集・アイテム編集・ライセンス）。Android 無変更 |
| 16 | **開発基盤の追随** | CI が `docs` / `build` / `instrumented` / `ios` の 4 ジョブで緑。allow のタスク名が実在する。§11 の 6 点が塞がっている。`docs/仕様/15_アーキテクチャ定義書.md` と `17_テスト戦略定義書.md` がある。**docs 内の `src/main` 表記が KMP の source set 名に追随している**（[技術改善バックログ](./23_技術改善バックログ.md) の種別表など）。**T-32**（テストが 0 件で緑になるのを機械で止める）が入っている |

ステップ 11（リソース 52 件）と 12（画面 6 枚）は commit が大きい。11 を「文言」「drawable」、12 を「theme」「画面ごと」に割ってよい。

## データ層（ステップ 3・8）

### Room KMP

Room 2.8.4 のまま移せる（KMP は 2.7.0 で stable）。**Room 3.0 への移行は別件で、段 3 に混ぜない。**

```kotlin
plugins { id("androidx.room"); alias(libs.plugins.ksp) }
room { schemaDirectory("$projectDir/schemas") }   // ksp arg の room.schemaLocation は廃止
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
}
```

**`ksp(...)` 一発では効かない。** ターゲットごとに `add("ksp<Target>", ...)` が要り、**書き忘れてもビルドは通り、そのターゲットだけリンク時に落ちる**。`room-ktx` は落とす（KMP では `room-runtime` に統合）。`androidx.sqlite:sqlite-bundled` を `commonMain` へ。

```kotlin
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() { ... }

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
```

ビルダーには `setDriver(BundledSQLiteDriver())` と `setQueryCoroutineContext(Dispatchers.IO)` を対で指定する。DB ファイルパスだけ `expect/actual`（Android は `context.getDatabasePath("app_database")`、iOS は `NSDocumentDirectory`）。**Android 側は必ず `getDatabasePath("app_database")` の絶対パスを渡すこと**——名前の付け方を変えると既存端末の DB を見失う。

**`AppDatabase` の `@Volatile` ＋ `synchronized` ＋ `INSTANCE` キャッシュは畳んで削除する。** `synchronized` は JVM 専用で common に置けない。Koin の `single` が既に単一性を保証しており、`KoinGraphTest` はまさにこの畳み込みを見越して書いてある。ついでに T-25 の「テスト用に DB 名を差せる口」も開く。

### `RoomMigrationTest` は 2 か所壊れる

1. `MigrationTestHelper` の Android 専用コンストラクタが KMP 版（`databaseFactory` ラムダと driver を取る形）に変わる
2. **driver を設定した DB では `openHelper` が使えない**ので `openHelper.writableDatabase.close()` が例外になる。`db.close()` へ書き換える

### `java.time` → `kotlin.time`

kotlinx-datetime 0.7 で `Instant` / `Clock` は stdlib へ移管され、`kotlinx.datetime.Instant` は typealias になった。**選択の問題ではない。**

| いま | 置き換え先 |
|---|---|
| `java.time.Instant` | `kotlin.time.Instant` |
| `Instant.now()` | `Clock.System.now()` |
| `Instant.ofEpochMilli(l)` | `Instant.fromEpochMilliseconds(l)` |
| `instant.toEpochMilli()` | `instant.toEpochMilliseconds()` |
| `Instant.MAX` / `MIN`（テスト） | `Instant.DISTANT_FUTURE` / `DISTANT_PAST` |

**保存形式はエポックミリ秒の `INTEGER` のままなので DB 互換は壊れない**（T-24 で文字列保存をやめた恩恵）。影響範囲は `:shared:data` の 6 ファイルとテスト 3 ファイル。`:shared:domain` は JVM 専用 API を 1 つも使っていないのでそのまま移せる。

オーバーフロー時の例外型が `ArithmeticException` から変わる可能性があるので、`InstantConverter` の KDoc と `InstantConverterTest` の 2 件を実測で直す。

## UI 層（ステップ 10〜13）

### 日付の書式化だけは `expect/actual`

`HomeScreen` の `DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)` はロケール依存で、stdlib にも kotlinx-datetime にも等価物が無い。

`expect fun formatShortDate(instant: Instant): String` を `:shared:ui` に置き、

- `androidMain` の actual は**いまのコードをそのまま**（`java.time` を使う）→ **Android の表示が 1 文字も変わらない**
- `iosMain` の actual は `NSDateFormatter` の `dateStyle = .short` → iOS の作法に合う（憲章 C-5 の解釈と整合）

common で `yyyy/MM/dd` 固定にする案は採らない。実装は 3 行で済むが Android の表示が変わり、段 3 の前提を崩す。

### `debugImplementation` の行き先

KMP ライブラリには build type が無く、宣言できるのは `androidCompilationApi` / `androidCompilationCompileOnly` / `androidCompilationImplementation` / `androidCompilationRuntimeOnly` の 4 つだけ（`androidRuntimeClasspath` は解決用なので宣言先にできない）。

`debugImplementation(compose.ui.tooling)` は**外す**。プレビューのレンダラなので `RuntimeOnly` にすると release にも載る。`@Preview` アノテーション自体は `ui-tooling-preview`（`implementation`）から来るのでコンパイルは通る。

`defaultConfig` も無いので `vectorDrawables { useSupportLibrary = true }` が書けなくなる。minSdk 31 では実害が無いので落とす。

### テストの `commonTest` 移送

`Dispatchers.setMain` / `resetMain` 自体は共通 API で K/N でも動く。壊れるのは JUnit4 の `TestWatcher` / `@get:Rule` のほう。基底クラスへ置き換える。

```kotlin
abstract class ViewModelTest {
    protected val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }
}
```

`StandardTestDispatcher` を選んだ意図（「まだ走っていない状態を観測できる」）は変わらないので、**`MainDispatcherRule` の KDoc をそのまま引き継ぐ**。`org.junit.Test` → `kotlin.test.Test`、`assertThrows` → `assertFailsWith<T>`。`assertEquals` の引数順は kotlin.test も `expected, actual` なので変わらない。

`FakeDatabase` と `TestData` は `java.time` の import 以外に JVM 依存が無く、そのまま移せる。ただし `FakeDatabase` は `ItemDao` / `CategoryDao` を実装しているので、**ステップ 8 で DAO 型が `commonMain` に居ることが前提**。

**ステップ 10 まで、message 付きの assert を足さないこと。** `kotlin.test` は `(actual, message)`、`org.junit.Assert` は `(message, actual)` で引数順が逆になる。いま message 付きの呼び出しは 0 件なので、機械的な import 差し替えで安全に移せる。

**iOS 側の Koin グラフは誰も検証しない。** `verify()` は JVM 専用で Android のグラフしか見ない。`:shared:ui` の `iosTest` に「起動して 5 つの型を `get()` する」テストを 1 本足し、`KoinGraphTest` の iOS 版に相当させる。

### Navigation 3 の多相シリアライズ

`rememberNavBackStack(key)` の 1 引数版は iOS で使えない。`NavigationState.kt` の `rememberNavBackStack` と `rememberSerializable(serializer = MutableStateSerializer(NavKeySerializer()))` の**両方**に `SavedStateConfiguration` を渡し、`polymorphic(NavKey::class)` に `Screen` のサブクラスを登録する。**`sealed class Screen : NavKey` を `@Serializable sealed interface` にするのが前提。**

iOS には reflection ベースの多相シリアライズが無いので、**登録漏れは Android では動いたまま iOS だけプロセス復元時に落ちる**。

## iOS ホスト（ステップ 6・7・15）

### framework は debug だけ作る

`binaries.framework { }` は既定で debug と release の両方を作る。**release のリンクは重い**——`./gradlew build` が 2〜4 分から **7 分 17 秒**になり、さらに Kotlin/Native のコンパイラが `org.gradle.jvmargs=-Xmx2048m` では `OutOfMemoryError` で落ちて 4GB を要求した。

段 3 の目的はシミュレータで動かすことなので `binaries.framework(listOf(NativeBuildType.DEBUG))` と絞る。debug だけなら 2GB のまま通る（2 ターゲット分の強制再リンクで 21 秒）。**実機配布で release が要る段 4 で足す。そのときヒープを 4GB に上げる必要がある。**

### Xcode プロジェクト

`iosApp/` に Xcode プロジェクトを丸ごとコミットする（KMP ウィザードと同じ形）。CocoaPods も SPM も段 3 では不要。

```
iosApp/
  iosApp.xcodeproj/project.pbxproj      コミットする
  iosApp/
    iOSApp.swift            @main struct、init で Koin 起動
    ContentView.swift       UIViewControllerRepresentable で ComposeUIViewController を包む
    Info.plist
  Configuration/Config.xcconfig          bundle id とチーム ID を Xcode UI の外に出す
```

Build Phases に Run Script を 1 本足す。

```
cd "$SRCROOT/.." && ./gradlew :shared:ui:embedAndSignAppleFrameworkForXcode
```

Kotlin 側は `:shared:ui` の `iosMain` に framework を宣言（`baseName = "ReBuyUi"`, `isStatic = true`）し、`startReBuyKoin()` と `ReBuyViewController()` を公開する。

**Android 依存は `expect val platformDataModule: Module` に閉じ込め、`dataModule` がそれを `includes` する。** これで `uiModule → domainModule → dataModule → platformDataModule` という既存の連鎖の形を崩さずに済む。

## AboutLibraries（ステップ 14）

15.2.0 が Compose 1.12.x / AGP 9 / Kotlin 2.4 / API 37 をサポート版として明記しており、このリポジトリの構成に合う。変更は 2 か所だけ。

```kotlin
aboutLibraries { export { outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json") } }
```

```kotlin
val libraries by produceLibraries { Res.readBytes("files/aboutlibraries.json").decodeToString() }
```

**生成物がソースディレクトリの中に入る**ので、コミットするか `.gitignore` するかを決める。コミットしないなら、リソースコピータスクが `exportLibraryDefinitions` に依存するよう明示的に配線しないと、初回ビルドで「ファイルが無い」か「古い内容が焼き込まれる」が起きる。設定キャッシュが有効なので暗黙依存だと警告も出ない。

**収集範囲は KMP で再び変わる。** 段 2 でやった「移設の前後で `aboutlibraries.json` を diff する」をここでもう一度行う。

## 落とし穴

1. **ホストテストの opt-in 忘れで 122 件が黙って 0 件になる。** `withHostTestBuilder {}` は既定 off で `./gradlew build` は緑のまま。全ステップの完了条件に件数を書く
2. **build-logic を Gradle 埋め込み Kotlin 2.2 でコンパイルできない場合の退避**（優先順）: (a) precompiled script plugin をやめて `java-gradle-plugin` ＋ 素の `Plugin` クラスにする——Kotlin コンパイラが KGP 側になるので版の縛りが外れる、(b) `kotlin { compilerOptions { languageVersion } }` を明示、(c) root の `subprojects {}` / `plugins.withId {}` に寄せる——T-28 の目的（ターゲット定義の書き忘れ防止）は達成できるので退避として成立する
3. **build-logic の AGP / KGP 版が root とずれると、コンパイルは通って実行時に `NoSuchMethodError`**
4. **Room の KSP をターゲットごとに書き忘れると、そのターゲットだけリンク時に落ちる。** Android では一切現れない
5. **AGP の lint タスクが KSP の生成先を入力に取るのに依存を宣言しない。** KMP ライブラリプラグインの host test で `lintAnalyzeAndroidHostTest` と `generateAndroidHostTestLintModel` が `Property has implicit dependency` で落ちる。`dependsOn("kspAndroidHostTest")` を自分で繋ぐ（`:shared:data` に実例）
6. ~~**KMP 化すると JVM target 17 が黙って外れる。**~~ → **T-28b（ステップ 4）で塞いだ**（2026-08-30）。`rebuy.android.base` の KMP 用の枝が `jvmTarget` を入れるので、**モジュール側に書かない**。書き忘れてもビルドもテストも緑のままバイトコード版がビルド環境の JDK で決まる（手元の JBR 25 と CI の Temurin 21 で別物が出る）性質は変わらないので、KMP 化のたびに `javap -v <class> | grep "major version"` が 61 かを確かめる
7. **`room { schemaDirectory }` がバリアント別サブディレクトリを掘ると、assets 指定と `RoomMigrationTest` が同時に壊れる**
8. **`RoomMigrationTest` は driver 導入で 2 か所壊れる**
9. **Navigation 3 の多相シリアライズの登録漏れは iOS だけで出る**
10. **`:shared:ui` の `BuildConfig` が消える。** 新プラグインは BuildConfig 非対応。`rebuy.versionName` から `Version.kt` を生成する小さなタスクを convention plugin に置く
11. **`debugImplementation` が書けなくなる。** build type が無い
12. **Compose の版が二重管理になる。** `:shared:ui` は CMP プラグインの `compose.*`、`:androidApp` は androidx の BOM。ずれると `NoSuchMethodError` か重複クラス
13. **`Theme.kt` の `LocalContext` ＋ `dynamicDarkColorScheme`。** `dynamicColor` の既定が `false` なので、この分岐を `expect/actual` に切るか削るかを決めれば挙動は変わらない
14. **`AppDatabase` の `synchronized` が common に置けない**
15. **`DataModule.kt` の `androidContext()` が commonMain へ行けない唯一の依存。** `expect val platformDataModule` に閉じ込める
16. **`iosMain` の関数名を `init` で始めない。** `initKoin()` は Swift 側で `doInitKoin()` に化ける
17. **AboutLibraries が KMP の android ターゲットから依存を拾えない。** 15.2.0（最新）の KMP 経路は AGP の KMP android ライブラリに追随しておらず、`:shared:ui` を KMP 化すると `aboutlibraries.json` が **0 件**になる（`collect { all = true }` でも `commonMain` に依存を置いても変わらない）。**ビルドもテストも緑のまま、ライセンス画面だけが空になる。** ステップ 5〜14 の間は空を許容し、ステップ 14 で戻す（2026-08-30 オーナー判断）。あわせて `aboutlibraries.json` が生成物なのにソースツリー内に出る点もここで片づける
18. **`./gradlew build` が Linux CI で iOS 側に触る。** CI の `build` ジョブはタスクを明示列挙する形へ

## spec §11「③ で壊れる開発基盤」の仕分け

| # | 項目 | 状態 |
|---|---|---|
| 1 | `./gradlew build` が Linux で iOS タスクを落とす | **段 3 で塞ぐ**。CI の `build` ジョブを `./gradlew build` からタスクの明示列挙へ（ステップ 16） |
| 2 | GMD のタスク名 | **済**（段 2 で `:androidApp:` 修飾を allow に追加済み） |
| 3 | レポートパス | **概ね済**。`verifier.md` のタスク名の記述だけ直す |
| 4 | `test-reviewer` の起動契機 | **段 3 で塞ぐ**。`shared/*/src/commonTest/**` `androidHostTest/**` `iosTest/**` へ |
| 5 | CLAUDE.md のアーキテクチャ節 → `docs/仕様/15` | **段 3 で塞ぐ**（§14 の完了条件）。`17_テスト戦略定義書` も |
| 6 | KMP のタスク名を allow に | **段 3 で塞ぐ**。ただし**ステップ 3 で追随させる**——16 まで待つと以降 12 commit ぶん許可プロンプトを踏む |
