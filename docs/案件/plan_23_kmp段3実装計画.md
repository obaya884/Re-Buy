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
| 完了時 | 122 が `commonTest`（android と iosSimulatorArm64 の両方で走る）＋ 5 が `androidHostTest` | 20（変わらず） |

ステップ 10 の時点では `NavigatorTest` 11 件がまだ `androidHostTest` に居る（`Navigator` が Compose 依存の `NavigationState` を持つため）。**ステップ 13 で `commonTest` へ移る。**

`commonTest` へ行けないのは 5 件。

- `KoinModulesTest` 1 件——`koin-test` の `verify()` が kotlin-reflect 依存で JVM 専用
- `InstantConverterTimeZoneTest` 4 件——既定タイムゾーンを差し替える API が common に無い。**この検査を捨てると、CI が TZ=UTC で走るせいでタイムゾーン依存の混入が見えなくなる**（ステップ 8 で実測して分けた）

**Android 側では 122 ＋ 5 = 127 件、iOS 側では 122 件が走る**ことになる。

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
| 8 | **`:shared:data` を `commonMain` へ** | Converter テスト **20 件が `commonTest` で android と iosSimulatorArm64 の両方緑**、＋ TZ 固定の 4 件が `androidHostTest`（`:shared:data` は Android 25 件 / iOS 21 件）。**`shared/data/schemas` に git 差分が出ない**。**`ItemDao` / `CategoryDao` が `commonMain` にある**——ここに無いと `FakeDatabase` がステップ 10 で `commonTest` へ行けず往復になる。Android 同一挙動。**既存端末の DB が引き継がれる**（アップグレードインストールで手動確認）。instrumented 20 件緑 |
| 9 | **`:shared:domain` を `commonMain` へ** | `./gradlew build` 緑。両ターゲットでコンパイル。テスト件数不変 |
| 10 | **`:shared:ui` の非 UI を `commonMain` へ、テストを `commonTest` へ** | **ViewModel テスト 90 件が `commonTest` で両ターゲット緑**（`:shared:ui` は Android 102 件 / iOS 90 件）。`NavigatorTest` 11 件は `Navigator` が `NavigationState`（Compose 依存）を持つのでステップ 13 まで動かせない。`KoinModulesTest` 1 件は `androidHostTest` に残る。**アサーションの中身は 1 行も変えていない** |
| 11 | **リソースを Compose Resources へ（文言 49 件 ＋ drawable 3 件）** | Android の表示・文言が完全に同一（**21 画面の画素比較**。「Android の表示同一性は画素で確かめる」）。**解釈の余地がある 6 件を固定する instrumented テストが増えて 22 件**緑。ホストテスト件数不変（Android 132 件 / iOS 116 件）。`shared/ui/src/androidMain/res` が消えている |
| 12 | **Compose を CMP へ一本化し、ナビに依存しない部品を `commonMain` へ（theme 4 本 ＋ ダイアログ 2 本 ＋ 日付書式の `expect/actual`）** | Android 同一挙動（**21 画面の画素比較**）。**「最終購入」の日付表示が移行前と 1 文字も違わない**。instrumented 22 件緑。**日付書式を固定するテストが増えて `:shared:ui` は Android 111 件 / iOS 98 件**（Android はロケールと TZ を固定してリテラルで 4 件、iOS は文字列を見ない不変条件で 3 件）。`:shared:ui` の `androidMain` に残る androidx の Compose が `material-icons-core`（と版を決めるための BOM）だけになっている。`Theme.kt` の `dynamicColor` 分岐が消えている |
| 13 | **Navigation 3 を CMP 対応に（`SavedStateConfiguration` ＋ 多相シリアライズ）、ナビ基盤と画面 5 枚を `commonMain` へ** | Android 同一挙動。**プロセス death からの復元が移行前と同じ**（開発者オプションの「アクティビティを保持しない」で手動確認）。`NavigatorTest` 11 件が `commonTest` へ移って両ターゲット緑。**登録漏れを止める `ScreenSerializationTest` 2 件が増えて `:shared:ui` は Android 113 件 / iOS 109 件**。**保存・復元の載せ替えを実際に通す `NavigationStateRestorationTest` 3 件が増えて instrumented 25 件**。`androidMain` に残るのは `LicenseScreen` の中身（と日付書式の actual）だけ |
| 14 | **AboutLibraries を composeResources 経由に** | **ライセンス一覧に 134 件が出る**（移設前は 133 件。ステップ 5 で 0 件になった退行がここで戻る。落とし穴 17）。**収集の壊れ方を止める `LicenseLibrariesTest` 4 件が増えて instrumented 29 件**（件数の下限・Android の依存だけを拾えているか・全件にライセンスが付いているか・画面に届いているか）——それまでこの退行を検出できるのは人が実機で一覧を見ることだけだった。`aboutlibraries.json` はコミットし、読む側のタスクに `dependsOn` を明示。`androidMain` に残るのは日付書式の `actual` だけ |
| 15 | **`iosMain` のスタブを `ReBuyApp()` に差し替え、Koin を起動する** | **iOS シミュレータで全画面が動いた**（ホーム・買い物・設定・カテゴリー編集・アイテム編集・ライセンス）。ライセンス一覧も iOS で出る。ダイアログと **DB の追加・削除**も通る（Room ＋ 同梱 driver の書き込み経路が iOS で動く）。**TopAppBar がステータスバーに重ならず、余白も二重になっていない**（「セーフエリアは Compose 側で処理する」）。**起動して数秒後にプロセスが生きている**（落とし穴 18）。Android は挙動・テスト件数とも不変（instrumented 29 件）。**ここで iOS 固有の不具合を 1 つ見つけて直した**（落とし穴 22） |
| 16 | **開発基盤の追随** | CI が `docs` / `build` / `instrumented` / `ios` の 4 ジョブで緑。allow のタスク名が実在する。§11 の 6 点が塞がっている。`docs/仕様/15_アーキテクチャ定義書.md` と `17_テスト戦略定義書.md` がある。**docs 内の `src/main` 表記が KMP の source set 名に追随している**（[技術改善バックログ](./23_技術改善バックログ.md) の種別表など）。**T-32**（テストが 0 件で緑になるのを機械で止める）が入っている。**ビルド後に `git diff --exit-code` を見る**——`aboutlibraries.json` と `shared/data/schemas` はビルドで再生成される生成物をコミットしているので、再生成し忘れが CI で止まるようにする |

ステップ 13 は commit が大きい。「ナビ基盤の CMP 対応」「画面ごと」に割ってよい。

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

ビルダーには `setDriver(BundledSQLiteDriver())` と `setQueryCoroutineContext(Dispatchers.IO)` を対で指定する。**Native では `Dispatchers.IO` が `Dispatchers` のメンバではなく拡張プロパティなので、`import kotlinx.coroutines.IO` が別途要る**（無いと「internal で触れない」というエラーになり、版のせいだと誤診しやすい）。DB ファイルパスだけ `expect/actual`（Android は `context.getDatabasePath("app_database")`、iOS は `NSDocumentDirectory`）。**Android 側は必ず `getDatabasePath("app_database")` の絶対パスを渡すこと**——名前の付け方を変えると既存端末の DB を見失う。

**`AppDatabase` の `@Volatile` ＋ `synchronized` ＋ `INSTANCE` キャッシュは畳んで削除する。** `synchronized` は JVM 専用で common に置けない。Koin の `single` が既に単一性を保証しており、`KoinGraphTest` はまさにこの畳み込みを見越して書いてある。ついでに T-25 の「テスト用に DB 名を差せる口」も開く。

### `RoomMigrationTest` は壊れないが、本番と違う経路を見ている

当初は「driver 導入で 2 か所壊れる」と見ていたが、**壊れなかった**（ステップ 8 で実測）。このテストは自前で `Room.databaseBuilder(context, klass, name)`（Android 専用の旧オーバーロード）を呼んでおり、driver を設定しないので `openHelper` 経路のまま動く。

問題はむしろ**壊れなかったこと**にある。本番は `BundledSQLiteDriver` ＋ 絶対パスになったのに、このテストは framework SQLite ＋ 相対名を開いている。**driver とパスの決め方の退行はこのテストでは捕まらない。** T-34 で揃える。

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

### androidx の lifecycle と Navigation 3 は iOS 成果物を持っている

**`navigation3-ui` だけ乗り換えが要る**（ステップ 13 で実測）。Google の KMP 公開はグループ単位ではなく
成果物ごとに進んでいる。

| 成果物 | iOS 向けの公開 |
|---|---|
| `androidx.navigation3:navigation3-runtime` | あり |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | あり |
| `androidx.navigation3:navigation3-ui`（`NavDisplay`） | **無い**（linux_x64 のみ） |

`org.jetbrains.androidx.navigation3:navigation3-ui:1.1.1` へ差し替える。**Android に載るものは変わらない**
——JB 版の android バリアントは `androidx.navigation3:navigation3-ui` への依存を宣言するだけの
リダイレクトで、`.module` を読んで確認した。`navigation3-runtime` は androidx のまま。

ステップ 9 の時点では「`navigation3-runtime-iosarm64` が実在するのでフォークは要らない」と結論したが、
**確かめたのは `-runtime` だけで、実際に使う `-ui` を見ていなかった**。
「対照を置く」以前に、**使う予定の成果物を全部並べる**ことが要る。

**確認するときは対照を置くこと。** 最初に `group-index.xml` を grep したときはパターンが壊れていて「どれも無い」という誤った結論が出た。`room-runtime` / `sqlite-bundled`（iOS で動いているのが既知）を同じ手順にかけて、そちらが「あり」と出ることを見てから読む。

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

### iOS が緑でも移送が成功したとは言えない

ステップ 10 で実測した（2026-08-30）。`ViewModelTestBase` の継承を 1 クラスから外すと、**Android は 19/19 落ちるが iOS は 8 落ちて 11 が素通りする**。`androidx.lifecycle` は `Dispatchers.Main` の不在を例外で捕まえるが、iOS には Darwin の main キューが実在するので例外にならず、コルーチンが積まれたまま走らない。否定形のアサート（`assertFalse`・「何も起きない」系・初期値系）はその状態で緑になる。

**移送の判定は必ず両ターゲットで見ること。** そして**変異を入れる側も両ターゲットで確かめる**——片方だけ見ていると「網が効いている」と誤読する。

### テストの `commonTest` 移送

`Dispatchers.setMain` / `resetMain` 自体は共通 API で K/N でも動く。壊れるのは JUnit4 の `TestWatcher` / `@get:Rule` のほう。基底クラスへ置き換える。

```kotlin
abstract class ViewModelTestBase {
    protected val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }
}
```

`StandardTestDispatcher` を選んだ意図（「まだ走っていない状態を観測できる」）は変わらないので、**`MainDispatcherRule` の KDoc をそのまま引き継ぐ**。`org.junit.Test` → `kotlin.test.Test`、`assertThrows` → `assertFailsWith<T>`。`assertEquals` の引数順は kotlin.test も `expected, actual` なので変わらない。

`FakeDatabase` と `TestData` は `java.time` の import 以外に JVM 依存が無く、そのまま移せる。ただし `FakeDatabase` は `ItemDao` / `CategoryDao` を実装しているので、**ステップ 8 で DAO 型が `commonMain` に居ることが前提**。

**ステップ 10 まで、message 付きの assert を足さないこと。** `kotlin.test` は `(actual, message)`、`org.junit.Assert` は `(message, actual)` で引数順が逆になる。いま message 付きの呼び出しは 0 件なので、機械的な import 差し替えで安全に移せる。

**iOS 側の Koin グラフは誰も検証しない。** `verify()` は JVM 専用で Android のグラフしか見ない。`:shared:ui` の `iosTest` に「起動して 5 つの型を `get()` する」テストを 1 本足し、`KoinGraphTest` の iOS 版に相当させる。

### Compose を CMP に寄せるときに引っかかる 2 つ（ステップ 12 で実測）

**`@Preview` は CMP でも `androidx.compose.ui.tooling.preview.Preview` が正。**
JetBrains 版の `org.jetbrains.compose.ui.tooling.preview.Preview`（`compose.components.uiToolingPreview`）は
1.12 で非推奨になり、「`org.jetbrains.compose.ui:ui-tooling-preview` の
`androidx.compose.ui.tooling.preview.Preview` を使え」という警告が出る。依存を **`compose.preview`** に
すれば **import は androidx のまま** multiplatform 化できる。androidx の `ui-tooling-preview` は落とせる。

**`material-icons-core` には CMP の対応物が無い。** `Icons.Default.*` / `Icons.AutoMirrored.*` は
`compose.material3` が推移的にも引かない（外して実測、9 ファイルが未解決になる）。ステップ 13 で
**JetBrains の凍結版 `org.jetbrains.compose.material:material-icons-core:1.7.3` を `commonMain` に引く**
ことにした（経緯は [決定ログ](./log_23_技術改善バックログ.md)）。

**Android の解決版は 1.7.8 から 1.7.6 へ下がるが、表示は変わらない。** JB 版の android バリアントは
`androidx.compose.material:material-icons-core:1.7.6` を要求するだけで、版を 1.7.8 に固定していた
androidx の BOM は `androidMain` から外れるため。**1.7.6 と 1.7.8 のソースを 281 ファイル突き合わせた
ところ、違うのは著作権表記の年だけだった**（パスデータは 1 文字も変わっていない）。
版を固定するためだけに BOM を残す必要は無い。iOS には 1.7.3 の klib が載る。

ついでに確かめた 2 点。**`material-icons-core` 自体は非推奨ではない**——ライブラリ内の `@Deprecated` は
`materialIcon()` ビルダーのバイナリ互換シムで、「必要なものをコピーせよ」という案内は
`material-icons-extended` の話。そして **CMP のベクタ XML パーサは `android:autoMirrored` を読む**
（`XmlVectorParser.kt:79`）ので、drawable 化しても RTL の自動反転は保てる。

### 画面はナビ基盤より先に移せない（ステップ 12 の着手前に判明）

**画面は 1 枚も `commonMain` へ移せない。** 依存の向きが一方通行になっている。

- 画面 6 枚と `BottomNavigationBar` はすべて `Navigator` を第 1 引数に取る
- `sealed class Screen : NavKey` は `ReBuyApp.kt`（＝ステップ 13 の対象そのもの）の中にある
- `TestTags` は `BottomNavigationItem`（`NavKey` と `Screen` を持つ）を参照し、`ReBuyAppScaffold` はその `TestTags` を参照する

`commonMain` から `androidMain` は見えないので、ナビ基盤が `androidMain` にいる限り動かせない。
逆向きの依存は無いので、**ナビ基盤が移れば画面は素直に続く**。ステップ 12 と 13 の境界はこの向きに沿って引いている。

| ステップ 12 の着手時点で `androidMain` にいた 19 本 | 行き先 |
|---|---|
| `theme/` 4 本、`TextFieldAddDialog`、`TextFieldEditDialog` | **ステップ 12** |
| `NavigationState`、`Navigator`、`ReBuyApp`（＋`Screen`）、`BottomNavigationItem`、`TestTags`、`ReBuyAppScaffold`、`BottomNavigationBar`、画面 5 枚 | **ステップ 13** |
| `LicenseScreen` | **ステップ 13 と 14 に割れた**。TopAppBar と戻るは 13 で共通化し、`R.raw` を引く一覧の中身だけ `expect/actual` で残して 14 で畳んだ |

`Theme.kt` の `dynamicColor` 分岐（落とし穴 13）は**実質デッドコード**だった。既定が `false` で
呼び出し側も指定していないので、`LocalContext` ごと落とせば挙動を変えずに `commonMain` へ行ける。

日付書式（`HomeScreen` の `formatShortDate`）は `HomeScreen` が動かなくても切り出せるので、
`expect/actual` はステップ 12 で済ませる。ステップ 12 の完了条件「日付表示が 1 文字も違わない」の
対象はこの 1 か所だけ。

### 保存・復元の載せ替えは、移送の中でいちばんテストが届いていなかった（ステップ 13）

ステップ 13 で実際に変わったのは `NavigationState` の保存・復元経路だけと言ってよい。ところが
**そこを通るテストが 1 件も無かった**（test-reviewer の指摘で気づいた）。

- `NavigatorTest` は `NavigationState` を直接組み立てるので `rememberNavigationState` を呼ばない
- `ScreenSerializationTest` は登録の中身だけを見て、呼び出し側との配線を見ない

`StateRestorationTester` を使う `NavigationStateRestorationTest`（instrumented 3 件）を足した。
`ReBuyApp()` をそのまま描いて `rememberSerializable` の Saver を実際に走らせるので、符号化・復号の
両方と、`screenSavedStateConfiguration` が実際に配線されていることが一度に入る。

**「変わった箇所を通るか」は件数では分からない。** `NavigatorTest` 11 件が iOS でも走るようになった
のは事実だが、その 11 件が触れているのは移送で 1 行も変わっていないコードだけだった。

### リソースはそのまま移せる（ステップ 11 で実測）

`shared/ui/src/androidMain/res` の `values/strings.xml`（49 件）と `drawable`（3 件）を
`shared/ui/src/commonMain/composeResources/` の同名ディレクトリへ動かすだけで、**Android の表示は
ステータスバーを除いて 1 画素も変わらなかった**。移送前に心配していた 2 点はどちらも Android と同じに解釈される。

- `\n` のエスケープ（4 件）——改行として描画され、行数も折り返し位置も同じ
- `%1$s` の位置指定（3 件）——`stringResource(Res.string.x, arg)` でそのまま置換される

参照側の書き換えは機械的。

| いま | 置き換え先 |
|---|---|
| `androidx.compose.ui.res.stringResource` | `org.jetbrains.compose.resources.stringResource` |
| `androidx.compose.ui.res.painterResource` | `org.jetbrains.compose.resources.painterResource` |
| `R.string.x` / `R.drawable.x` | `Res.string.x` / `Res.drawable.x` |
| `stringResource(id = ...)` | `stringResource(...)`——第 1 引数の名前が `resource` に変わる |

型が変わるのは `BottomNavigationItem` だけ（`titleId: Int` → `title: StringResource`）。

**`Res` は `publicResClass = true` で公開する。** `:androidApp` の instrumented テストが画面のタイトルを
文言で引いているため。読み出しの `getString` は suspend なので、テスト側は `runBlocking` で待ち合わせる。

**`compose.components.resources` は `commonMain` に置く。** リソースが `commonMain` にある以上
ほかに置き場が無い。ステップ 11 の時点では「`compose.*` は `iosMain` だけ」という落とし穴 12 の規約の
唯一の例外だったが、**ステップ 12 で `compose.*` がすべて `commonMain` へ集まり、この規約は消えた**。
Android では JetBrains の別名アーティファクトが androidx へ解決するので BOM とはぶつからない。

**未使用の文言は、これ以降どの機械も見つけられない。** 生成される `allStringResources` が全件を
`map.put` するので参照が無くても使われているように見え、Android lint の `UnusedResources` の視界からも
外れる。移送の時点で 12 件ある（[技術改善バックログ](./23_技術改善バックログ.md) の T-36）。

**書式の解釈エンジンが静かに入れ替わる。** CMP の置換は `String.format` ではなく `%(\d+)\$[ds]` の
正規表現置換で、**解釈できない書式（`%s`・`%.2f`・`%%`）は例外を出さずに生のまま画面へ出る**。
`%N$d` もロケール書式が効かず `toString()` が入るだけ。いまの 3 件は `%1$s` ＋ String 引数なので
Android 時代と一致するが、**画素比較は今回限りの人手確認で残らない**ので、`\n` 4 件と `%1$s` 3 件
（重なりを除いて 6 件）を `StringResourceFormatTest` で期待値リテラルとして固定した。ほかのテストは
文言を `Res.string.*` から引くので、値が壊れても画面と同じ壊れた値を見て素通りしてしまう。

**drawable も経路が変わる。** Android の VectorDrawable インフレータから CMP 自前の
`XmlVectorParser` になった。いまの 3 件は `<path>` 1 本と `fillColor` / `pathData` だけなので
差が出なかったが、`fillType` / `<clip-path>` / `<gradient>` / `?attr/` を使う drawable を足すと
**静かに違う絵が出る**。読み込みに失敗すれば composition ごと落ちるので「在ること」だけは守られる。

**テストと画面でリソース環境の解決経路が違う。** テストの `getString` は
`getSystemResourceEnvironment()`（`Locale.getDefault()`）、画面の `stringResource` は
`rememberResourceEnvironment()`（`Locale.current` ＋ `LocalDensity` ＋ `isSystemInDarkTheme`）を通る。
修飾子付きディレクトリが `values/` 1 つしか無いうちは同じ項目を引くが、`values-en/` や `values-night/`
を足すと「テストが引く文言 ≠ 画面が出す文言」になりうる。

### Android の表示同一性は画素で確かめる

ステップ 11 と 12 の完了条件「表示・文言が完全に同一」は目視では守れない。`pm clear` から決まった順に
操作して 21 画面のスクリーンショットと `uiautomator` の dump を撮る走査を作り、移送の前後で流して
突き合わせる（`adb` だけで完結する）。

- **テキストと bounds は dump の diff で見る。** `\n` が改行として効いているかは bounds の高さに出る
- **見た目は PNG の画素で見る。** 時計が写るステータスバー（上端 142px）は除く
- **起動直後の 1 枚はスプラッシュを撮りうる。** dump は正しい画面を返すのに画像だけがスプラッシュ、
  という食い違いが実際に起きた。1 枚だけ差が出たら、まず撮り直す

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

Kotlin 側は `:shared:ui` の `iosMain` に framework を宣言（`baseName = "ReBuyUi"`, `isStatic = true`）し、`ReBuyViewController()` を公開する。**Koin を起動する関数はステップ 15 で足す**——ステップ 6 のスタブには要らない。

Xcode 側は `FRAMEWORK_SEARCH_PATHS` に `$(SRCROOT)/../shared/ui/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` を、`OTHER_LDFLAGS` に `-framework ReBuyUi` を入れる。**`TEAM_ID` は空のままコミットする**——シミュレータで動かすだけなら要らず、このリポジトリは public なので各自が手元で入れる。

コマンドラインからの確認は次のとおり（ステップ 15 でも使う）。

```
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
xcrun simctl boot "iPhone 17 Pro"
xcrun simctl install booted <DerivedData>/Build/Products/Debug-iphonesimulator/ReBuy.app
xcrun simctl launch booted io.github.obaya884.rebuy
xcrun simctl io booted screenshot out.png
```

### iOS 固有の未決事項（段 4 で決める）

段 3 では Android の挙動を変えないことが制約なので、**iOS だけに現れる選択**は決め切らずに送る。いずれも `iosApp/` の中で完結し、Android には影響しない。

| 項目 | いまの値 | Android 側 |
|---|---|---|
| ホーム画面のアプリ名 | `ReBuy`（`PRODUCT_NAME`） | くりかえし使える買い物リスト |
| 画面の向き | 縦固定 | 指定なし（回転する） |
| 対応デバイス | iPhone のみ | タブレットにも入る（最適化はしない） |
| 配備下限 | iOS 17.0 | minSdk 31 |

アプリ名と向きは**利用者から見える挙動**なので、オーナーとの対話で決める。配備下限 17.0 は Compose Multiplatform の要件（iOS 13+）ではなく選択で、minSdk 31 と実カバー率が近い水準に置いている。

### セーフエリアは Compose 側で処理する

`ContentView` は `.ignoresSafeArea()` を付けて Compose に画面全体を渡し、インセットの処理は Material3 の `Scaffold`（`contentWindowInsets`）に任せる。**SwiftUI 側でセーフエリアを効かせると、SwiftUI が余白を入れたうえで `Scaffold` がさらに入れて二重になる。**

**ステップ 15 で確かめた。この選択で正しい。** TopAppBar はステータスバーと Dynamic Island に重ならず、下部ナビもホームインジケータを避けており、余白の二重も無い。`ContentView` は `.ignoresSafeArea()` のまま、`Scaffold` の既定の `contentWindowInsets` に任せている（追加の指定は要らなかった）。

### Swift から見える API 面はファイル名でも決まる

Kotlin/Native はトップレベル関数の入れ物クラス名を**ファイル名から**作る。`ReBuyViewController.kt` のトップレベル関数は Swift から `ReBuyViewControllerKt.ReBuyViewController()` になる。**iOS の入口関数は 1 ファイルにまとめる**——別ファイルに分けると Swift 側に `...Kt` が 2 つ並ぶ。

Koin を起動する関数は `setupKoin()` にする。`startKoin()` は `org.koin.core.context.startKoin` と紛らわしく、`iosMain` で import すると衝突する。

**Android 依存は `expect val platformDataModule: Module` に閉じ込め、`dataModule` がそれを `includes` する。** これで `uiModule → domainModule → dataModule → platformDataModule` という既存の連鎖の形を崩さずに済む。

## AboutLibraries（ステップ 14）

15.2.0 が Compose 1.12.x / AGP 9 / Kotlin 2.4 / API 37 をサポート版として明記しており、このリポジトリの構成に合う。出力先と読み出し API は AboutLibraries が KMP 向けに示している形をそのまま採った。

```kotlin
aboutLibraries {
    collect { filterVariants.set(setOf("android")) }
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}
```

```kotlin
val libraries by produceLibraries { Res.readBytes("files/aboutlibraries.json").decodeToString() }
LibrariesContainer(libraries, modifier)
```

### 0 件だった原因は「バリアント名で設定を探していた」こと

プラグインは Android のリソースに載せる json を、**AGP のバリアント名**を持つタスクで作る（`AboutLibrariesPluginAndroidExtension.kt` の `prepareLibraryDefinitions<バリアント名>`）。KMP android ライブラリのバリアント名は `androidMain` だが、依存が載る設定は Kotlin ターゲット名の `androidCompileClasspath` / `androidRuntimeClasspath`。`BaseAboutLibrariesTask.configure()` は `<バリアント名>CompileClasspath` を探すので、**どちらも存在するのに突き合わせが外れて 0 件になる**。`collect { all = true }` が効かなかったのも同じで、`all` は探す設定の範囲を広げるだけで名前の突き合わせは変えない。

**ステップ 14 でこの経路を使うのをやめたので、ずれ自体が消えた。** 出力先を composeResources にすると Android のリソース生成は無効になり、書き出すのはバリアントを持たない `exportLibraryDefinitions` になる。バリアントが無ければ絞り込みも無く、解決できる設定を全部見る。

### `filterVariants` は「Android に載るものだけ」に絞るために要る

絞らないと **139 件**になり、Android に載らない 5 件が混ざる——`skiko` / `ui-uikit` / `kotlinx-browser` / `atomicfu` / `kotlinx-datetime`。`commonMain` のメタデータ解決を通って入ってくるもので、`:androidApp` の `debugRuntimeClasspath` には 1 つも無い。`collect { filterVariants.set(setOf("android")) }` で `androidCompileClasspath` / `androidRuntimeClasspath` だけを見るようにすると **134 件**になり、移設前の 133 件と直接くらべられる。

**iOS を出すときはここを見直す。** いま `android` に絞っているぶん、iOS でだけ載るものが一覧から落ちる（T-39）。

### 出力先は `build/` に逃がせない

Compose Resources の `customDirectory(sourceSetName, provider)` は既定のディレクトリに**足すのではなく置き換える**（`PrepareComposeResources.kt` の `ext.customResourceDirectories[sourceSet.name] ?: 既定`）。`commonMain` に指定すると `composeResources/values` も `drawable` も見えなくなり、**文言 49 件と drawable 3 件が一斉に `Unresolved reference` になる**。生成物をソースツリーの外に置く道はここで閉じた。

そのうえでコミットする方を採った（AboutLibraries も SCM に入れる運用を挙げている）。Room のスキーマと同じ扱いで、**依存の増減がライセンス一覧の diff として見える**のが利点。1 行 88KB では読めないので `prettyPrint` を入れる。

コミットしても**タスク依存は要る**。生成物がリソースのソースディレクトリの中に出るので、読む側（`copyNonXmlValueResourcesForCommonMain` / `convertXmlValueResourcesForCommonMain`）に `dependsOn("exportLibraryDefinitions")` を明示する。暗黙依存のままだと、設定キャッシュが有効なぶん警告も出ないまま「ファイルが無い」か「古い内容が焼き込まれる」が起きる。

### 移設の前後で diff した

段 2 と同じく、移設前（`4054492~1`）の `exportLibraryDefinitionsDebug` と突き合わせた。**133 件 → 134 件**で、差分は 13 件すべてが段 3 で実際に起きた依存の変化だった（収集漏れは無い）。

| 消えた | 理由 |
|---|---|
| `ui-tooling` / `ui-tooling-data` | 落とし穴 11。ステップ 6・12 で落とした |
| `compose-bom` | ステップ 12 で CMP 一本化 |
| `room-ktx` | ステップ 8。KMP では `room-runtime` に統合 |

| 増えた | 理由 |
|---|---|
| `sqlite-bundled` | ステップ 8 |
| `navigation3-ui`（JetBrains） | ステップ 13 |
| `components-resources` | ステップ 11 |
| `material-icons-core`（JetBrains 1.7.3） | ステップ 13 |

版が動いたのは `material-icons-core` 1.7.8 → 1.7.6 と **`material-ripple` 1.12.0 → 1.9.3**。どちらも BOM を外したステップ 12 の結果で、そのときの画素比較では表示が変わっていない。

## 落とし穴

1. **ホストテストの opt-in 忘れで 122 件が黙って 0 件になる。** `withHostTestBuilder {}` は既定 off で `./gradlew build` は緑のまま。全ステップの完了条件に件数を書く
2. **build-logic を Gradle 埋め込み Kotlin 2.2 でコンパイルできない場合の退避**（優先順）: (a) precompiled script plugin をやめて `java-gradle-plugin` ＋ 素の `Plugin` クラスにする——Kotlin コンパイラが KGP 側になるので版の縛りが外れる、(b) `kotlin { compilerOptions { languageVersion } }` を明示、(c) root の `subprojects {}` / `plugins.withId {}` に寄せる——T-28 の目的（ターゲット定義の書き忘れ防止）は達成できるので退避として成立する
3. **build-logic の AGP / KGP 版が root とずれると、コンパイルは通って実行時に `NoSuchMethodError`**
4. **Room の KSP をターゲットごとに書き忘れると、そのターゲットだけリンク時に落ちる。** Android では一切現れない
5. **AGP の lint タスクが KSP の生成先を入力に取るのに依存を宣言しない。** KMP ライブラリプラグインの host test で `lintAnalyzeAndroidHostTest` と `generateAndroidHostTestLintModel` が `Property has implicit dependency` で落ちる。`dependsOn("kspAndroidHostTest")` を自分で繋ぐ（`:shared:data` に実例）
6. ~~**KMP 化すると JVM target 17 が黙って外れる。**~~ → **T-28b（ステップ 4）で塞いだ**（2026-08-30）。`rebuy.android.base` の KMP 用の枝が `jvmTarget` を入れるので、**モジュール側に書かない**。書き忘れてもビルドもテストも緑のままバイトコード版がビルド環境の JDK で決まる（手元の JBR 25 と CI の Temurin 21 で別物が出る）性質は変わらないので、KMP 化のたびに `javap -v <class> | grep "major version"` が 61 かを確かめる
7. **`room { schemaDirectory }` がバリアント別サブディレクトリを掘ると、assets 指定と `RoomMigrationTest` が同時に壊れる**
8. **`RoomMigrationTest` は driver 導入で 2 か所壊れる**
9. ~~**Navigation 3 の多相シリアライズの登録漏れは iOS だけで出る**~~ → **前提が誤りだった。Android でも同じように落ちる**（2026-08-30 に実測）。Android を救っていたのは `rememberNavBackStack(vararg)` ＝ `NavKeySerializer` の reflection 経路だけで、ステップ 13 でそれを使うのをやめたため差が消えた。登録を落として instrumented を回すと `SerializationException: Serializer for subclass 'License' is not found` で 2 件が落ちる。**壊れ方が揃ったので Android 側に網を張れる**——`ScreenSerializationTest`（JVM 段）と `NavigationStateRestorationTest`（instrumented）の 2 本で塞いだ
10. **`:shared:ui` の `BuildConfig` が消える。** 新プラグインは BuildConfig 非対応。`rebuy.versionName` から `Version.kt` を生成する小さなタスクを convention plugin に置く
11. ~~**`debugImplementation` が書けなくなる。** build type が無い~~ → **決着済み**（2026-08-30）。`compose.ui.tooling`（プレビューのレンダラ）はステップ 6 で落とし、`@Preview` の依存はステップ 12 で `compose.preview` に置き換えた。`debugImplementation` に置きたいものは残っていない
12. ~~**Compose の版が二重管理になる。**~~ → **ステップ 12 で CMP 一本にした**（2026-08-30）。`:shared:ui` の Compose は `commonMain` の `compose.*` だけになり、androidx の BOM は外れた。**踏んだ教訓は残る**——`compose.material3` は自分より古い `foundation` を推移的に引くので、`compose.foundation` を「未使用だから」と外すと版が 1.12.0 から 1.9.1 へ下がる（ステップ 8 で実際に踏み、`checkIos…ComposeLibrariesCompatibility` の警告で気づいた）。**依存には「使う」以外に「版を固定する」役目がある**
13. ~~**`Theme.kt` の `LocalContext` ＋ `dynamicDarkColorScheme`。**~~ → **ステップ 12 で分岐ごと削除した**（2026-08-30）。`dynamicColor` は既定が `false` で呼び出し側も指定しておらず、一度も通っていなかった。使うと決めたときに `expect/actual` で足す
14. **`AppDatabase` の `synchronized` が common に置けない**
15. **`DataModule.kt` の `androidContext()` が commonMain へ行けない唯一の依存。** `expect val platformDataModule` に閉じ込める
16. **`iosMain` の関数名を Objective-C の method family で始めない。** `init` / `new` / `copy` / `mutableCopy` / `alloc` の 5 つが該当し、いずれも Swift 側で `do` が付く（`initKoin()` → `doInitKoin()`）
17. ~~**AboutLibraries が KMP の android ターゲットから依存を拾えない。**~~ → **ステップ 14 で解けた**（2026-08-30）。原因はプラグインの KMP 非対応ではなく、Android のリソースへ載せる経路が **AGP のバリアント名（`androidMain`）で Kotlin ターゲット名の設定（`androidCompileClasspath`）を探していた**こと。出力先を composeResources にするとこの経路自体を通らなくなる。**バリアント名で `filterVariants` を書くと同じ 0 件が戻る**——変異で実測した——ので、`LicenseLibrariesTest`（instrumented）が件数を見て止める
18. **CMP は `Info.plist` に `CADisableMinimumFrameDurationOnPhone` を要求する。** 無いと `PlistSanityCheck` が例外を投げて `SIGABRT` で落ちる。**チェックが走るのは 1 フレーム描画した後**なので、スクリーンショットには正常な画面が写る。**iOS の確認でスクリーンショットは「動いた」証拠にならない**——`xcrun simctl spawn booted launchctl list | grep -i <app>` でプロセスが生き続けているかを見ること（ステップ 7 で実際に踏んだ）
19. **Linux CI は iOS のタスクを黙って無効化する。** 落ちるのではなく `Native task 'iosSimulatorArm64Test' is disabled` / `cannot run on the current host (linux-x86_64)` の警告を出して素通りする（ステップ 6 で実測）。ステップ 6 の時点では `commonMain` が空なので害が無いが、**ステップ 8 以降は `compileKotlinIosArm64` に実際のソースが入り、iOS 側のコンパイルエラーが CI をすり抜ける**。ステップ 16 で macOS ランナーの `ios` ジョブを足すまで、**各ステップでローカルの `linkDebugFrameworkIosSimulatorArm64` を自分で回すこと**が唯一の網になる
20. **Compose Resources を Android に載せているのは assets 経路で、`androidResources { enable = true }` に紐づいている。** CMP は `variant.sources.assets` に配線しており（`AndroidResources.kt`）、`assets` が null だと**何も言わずに配線を諦める**。`:shared:ui` の Kotlin から `R` を引く場所が無くなったからと無効にすると、**ビルドは緑・APK に `assets/composeResources/` が 1 つも入らない・起動して全画面が落ちる**（ステップ 14 で実際に踏んだ）。APK の中身は `unzip -l` で見る
21. **`compose.resources { customDirectory(...) }` は既定のディレクトリを置き換える。** `commonMain` に足すつもりで指定すると `composeResources/values` も `drawable` も見えなくなり、文言と drawable が一斉に `Unresolved reference` になる（ステップ 14 で実際に踏んだ）。生成物をソースツリーの外へ逃がす用途には使えない
22. **iOS では `is <data object>` が、分岐に `@Composable` 呼び出しを含む `when` の中で一致しない。** `tab is HomeTab.All` が `true` を返す直後に、同じ `tab` を見る `when` が `NoWhenBranchMatchedException` を投げる（ステップ 15 で実測）。**Android では起きない。** `is HomeTab.All` を `HomeTab.All`（等値）に書き換えると通る。`data object` は singleton なので等値のほうがそもそも読みやすく、書き換えは意味を変えない。**object の分岐は `is` で書かない**——リポジトリ内で `when` を使っているのは `HomeScreen` の 3 か所だけで、すべて等値に揃えた（T-41）

## spec §11「③ で壊れる開発基盤」の仕分け

| # | 項目 | 状態 |
|---|---|---|
| 1 | `./gradlew build` が Linux で iOS タスクを落とす | **段 3 で塞ぐ**。CI の `build` ジョブを `./gradlew build` からタスクの明示列挙へ（ステップ 16） |
| 2 | GMD のタスク名 | **済**（段 2 で `:androidApp:` 修飾を allow に追加済み） |
| 3 | レポートパス | **概ね済**。`verifier.md` のタスク名の記述だけ直す |
| 4 | `test-reviewer` の起動契機 | **段 3 で塞ぐ**。`shared/*/src/commonTest/**` `androidHostTest/**` `iosTest/**` へ |
| 5 | CLAUDE.md のアーキテクチャ節 → `docs/仕様/15` | **段 3 で塞ぐ**（§14 の完了条件）。`17_テスト戦略定義書` も |
| 6 | KMP のタスク名を allow に | **段 3 で塞ぐ**。ただし**ステップ 3 で追随させる**——16 まで待つと以降 12 commit ぶん許可プロンプトを踏む |
