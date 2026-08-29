# KMP 化 段 2（モジュール分割）実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

- 作成日: 2026-08-29
- 位置づけ: [KMP 化検討](../検討/32_KMP化検討.md) §10 の**段 2（モジュールを 4 つに分ける）**の実装手順。台帳エントリは T-20（[技術改善バックログ](./23_技術改善バックログ.md)） **段 2（モジュール分割）の完了（2026-08-29）により凍結。以後は書き換えない**
- 前段: [段 1 実装計画](./archive_23_kmp段1実装計画.md)（Koin 化）。**段 1 の完了が前提**
- **本書は段 1 に入る前に書かれている。** 調査の結果を失わないための先行執筆で、着手時に段 1 の結果を踏まえて見直すこと。特に Koin モジュールの配置（段 1 Task 3。`uiModule` → `domainModule` → `dataModule` の `includes` 連鎖）が本書の前提

**Goal:** 単一モジュール `:app` を `:androidApp` / `:shared:ui` / `:shared:domain` / `:shared:data` の 4 つに割る。**Android のまま**で、KMP 化はしない（段 3）。挙動は変えない。

**Architecture:** 層別 4 モジュール。依存の向きは `:androidApp` → `:shared:ui` → `:shared:domain` → `:shared:data` の一方向で、Gradle が機械的に強制する（[KMP 化検討](../検討/32_KMP化検討.md) §3）。

**Tech Stack:** Kotlin 2.4.10 / AGP 9.3.2（built-in Kotlin）/ Compose BOM 2026.08.00 / Room 2.8.4 / Koin / AboutLibraries 15.2.0 / Gradle Managed Device `pixel6Api35`

**Spec:** [docs/検討/32_KMP化検討.md](../検討/32_KMP化検討.md) §3・§11

## Global Constraints

- **挙動を変えない。** 画面・遷移・データの読み書き・ライセンス表示の中身を変えない
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17。**4 モジュールすべてに書く**
- **`org.jetbrains.kotlin.android` を足さない。** AGP 9 の built-in Kotlin は `com.android.library` にも同じように効く
- **各モジュールに `repositories { google(); mavenCentral() }` を書く。** `dependencyResolutionManagement` を使っていないため
- ルート `build.gradle.kts` の `buildscript { classpath }`（Kotlin / KSP / serialization の版）は**触らない**。プロジェクト全体のクラスパスなので新モジュールにも効く
- 依存は必ず `gradle/libs.versions.toml` 経由で追加する
- `kotlinOptions {}` は使えない。`ksp {}` はトップレベル、`sourceSets {}` は `android {}` 直下
- git 運用: **ブランチ `t-20-modules` → PR → CI 緑 → オーナーの動作確認 → 合図で squash マージ**
- 表のセル内に `|` を書かない

## 合否判定

**既存のテストが 1 行も変わらずに緑であること。** ユニット 121 件・instrumented 14 件。Gradle の構成変更しかしないので、これが成立するはず。成立しないなら、分割の設計が何かを動かしてしまっている。

具体的な機械判定は **`git diff --stat androidApp/src/androidTest` が空**であること。

**Koin なので DI の壊れがコンパイルで出ない。** 各ステップの完了条件から GMD を落とさないこと。`./gradlew build` だけで緑と判定すると、`koinViewModel()` が解決できないアプリが緑で通る。

## モジュールとプラグイン

```
:shared:data     com.android.library, com.google.devtools.ksp
:shared:domain   com.android.library
:shared:ui       com.android.library, kotlin.plugin.compose,
                 kotlin.plugin.serialization, com.mikepenz.aboutlibraries.plugin
:androidApp      com.android.application, kotlin.plugin.compose
```

- **KSP が要るのは `:shared:data` だけ**（Room compiler）。段 1 で Hilt を捨てたので、他のモジュールに KSP は要らない
- serialization は `Screen` の `@Serializable` があるので `:shared:ui` だけ。版指定なしの `id("org.jetbrains.kotlin.plugin.serialization")` で書く
- `:androidApp` にも Compose コンパイラは要る（`MainActivity.setContent`）。`buildFeatures { compose = true }` も
- `com.android.library` の plugin alias が version catalog に無いので足す。ルート `build.gradle.kts` の `plugins {}` に `alias(libs.plugins.android.library) apply false`
- `:shared:domain` を `java-library` にしない。Android API を使っていないので JVM モジュールにできるが、`org.jetbrains.kotlin.jvm` が要り、「KGP を明示適用しない」構成に穴が開く。段 3 で KMP モジュールに変えるときも作り直しになる
- `include(":shared:data")` で `:shared` が中間プロジェクトとして生まれるが、ビルドファイルも `repositories {}` も要らない

## namespace とリソース — ここが合否の分岐点

> **この節の方針はのちに覆った（2026-08-29、T-29）。** 本節は「既存の `R` の import を 1 行も変えない」ことを優先して `:shared:ui` にルート namespace を渡す形を採ったが、調査の結果このやり方は実例が無く、`:shared:ui` に androidTest を足すとテスト APK の applicationId が衝突するという実害もあった。現在の方針は [KMP 化検討](../検討/32_KMP化検討.md) §3 が正。以下は段 2 当時の記録として残す。

`gradle.properties` に `android.nonTransitiveRClass=true` があり、AGP 9.3.2 ではアプリモジュールの `R` も非推移的（`NON_TRANSITIVE_APP_R_CLASS` が `Enforced(VERSION_7_0)`）。**`:androidApp` の `R` には、ライブラリへ移した `strings.xml` の項目は入らない。**

一方 `NavigationTest` / `ViewModelScopeTest` は `import io.github.obaya884.rebuy.R` を持ち、`R.string.home_title` を引いている。`home_title` は `:shared:ui` へ移る。**この 2 ファイルを 1 行も変えずに通すには、`io.github.obaya884.rebuy.R` が `:shared:ui` の `R` である必要がある。**

| モジュール | `namespace` | `R` |
|---|---|---|
| `:shared:data` | `io.github.obaya884.rebuy.data` | リソースなし |
| `:shared:domain` | `io.github.obaya884.rebuy.domain` | リソースなし |
| `:shared:ui` | **`io.github.obaya884.rebuy`** | `io.github.obaya884.rebuy.R` |
| `:androidApp` | `io.github.obaya884.rebuy.app` | `io.github.obaya884.rebuy.app.R` |

`applicationId` は `io.github.obaya884.rebuy` のまま。namespace と applicationId は別物なので、**端末上のパッケージ名・`context.packageName`・DB のファイルパスは一切変わらない**（spec §3 に反映済み）。

これで本番の UI 9 ファイルと instrumented 2 ファイルの `import io.github.obaya884.rebuy.R` が無変更で通る。

払うコスト: `AndroidManifest.xml` の `android:name=".ReBuyApplication"` と `.ui.activity.MainActivity` は **namespace 相対**なので完全修飾名に書き換える。Kotlin のパッケージは変えないので `NavigationTest` の `import ...ui.activity.MainActivity` は無変更。

### 却下した案

| 案 | 却下の理由 |
|---|---|
| `:shared:ui` を `io.github.obaya884.rebuy.ui` にする | 本番 9 ファイルと**テスト 2 ファイル**の import が変わり、合否判定を満たせない |
| `android.nonTransitiveRClass=false` に戻す | テストは通るが、非推移的 R は AGP の既定・推奨方向。ビルド時間とインクリメンタリティを捨てる後退で、しかも本番 9 ファイルの import はどのみち変わる |
| `:shared:ui` と `:androidApp` に同じ namespace | AGP 9.3.2 の Gradle 側に重複を弾くエラーは見つからなかったが、両モジュールが同一 FQN の `R` を生成するため dex で衝突する経路が残る。**やらない** |

### リソースの分け方

| 置き場所 | 中身 |
|---|---|
| `shared/ui/src/main/res/values/strings.xml` | `app_name` **以外の全て**（画面文言 50 件強） |
| `shared/ui/src/main/res/drawable/` | `icon_category.xml` / `icon_check_list.xml` / `icon_shopping_bug.xml` |
| `androidApp/src/main/res/values/strings.xml` | `app_name` だけ（マニフェストの `android:label`） |
| `androidApp/src/main/res/` | `mipmap-*`・`drawable/ic_launcher_*`・`values/themes.xml`・`values/colors.xml`・`xml/backup_rules.xml`・`xml/data_extraction_rules.xml` |

`themes.xml` が `@color/purple_700` を参照しているので `colors.xml` は同じモジュールに置く。Compose の色は `ui/theme/Color.kt` で完結しており `R.color` 参照は無いので、`colors.xml` は `:androidApp` で問題ない。

`vectorDrawables { useSupportLibrary = true }` は `icon_*.xml` と一緒に `:shared:ui` へ。

### `BuildConfig.VERSION_NAME` — 見落としやすい 1 箇所

`SettingScreen.kt` がバージョン表示に使っている。**ライブラリモジュールの `BuildConfig` に `VERSION_NAME` は生成されない**（AGP 8 以降、library は `DEBUG` と `LIBRARY_PACKAGE_NAME` 程度）。

対処は `gradle.properties` に `rebuy.versionName` を置き、`:androidApp` の `versionName` と `:shared:ui` の `buildConfigField("String", "VERSION_NAME", ...)` の両方から読む（`:shared:ui` に `buildFeatures { buildConfig = true }` が要る）。表示が 1 文字も変わらない。

段 3 で Compose Multiplatform には `BuildConfig` が無いので作り直しになるが、段 2 ではこの最小手当てで十分。

## AboutLibraries — `:shared:ui` に適用する

プラグインのソース（`AboutLibrariesPluginAndroidExtension.kt`）を読んで確認した。`configureAndroidTasks` は `com.android.application` / `com.android.library` / `com.android.kotlin.multiplatform.library` の 3 つを `pluginManager.withPlugin` で見ており、いずれも同じ処理を回す。処理の実体は

```
val resultsResDirectory = project.layout.buildDirectory.dir("generated/aboutLibraries/<variant>/res/")
variant.sources.res?.addGeneratedSourceDirectory(task) { it.outputDirectory }
```

で、**`res/raw/aboutlibraries.json` を「そのモジュール自身の res ソース」として登録する**。したがって `LicenseScreen` と同じモジュールに適用しないと `R.raw.aboutlibraries` が引けない。

→ **プラグインを `:androidApp` から `:shared:ui` へ移す。** namespace の設計と組み合わせれば `LicenseScreen` は 1 文字も変わらない。`:androidApp` 側にプラグインは不要。`aboutLibraries {}` の拡張設定は現在無いので持ち越すものは無い。

### 収集範囲が変わる — 必ず diff する

`util/DependencyCollector.kt` は、適用モジュールの `<variant>RuntimeClasspath` / `<variant>CompileClasspath` を解決して依存グラフを歩く。`ProjectComponentIdentifier`（プロジェクト依存）は座標としてはスキップするがその先の依存はたどるので、`:shared:ui` に適用すれば `:shared:domain` → `:shared:data` 経由の Room や coroutines も拾える。

**拾えなくなるのは `:androidApp` にしか宣言していない依存**（`activity-ktx`・`core-ktx` など）。ビルドは通り、ライセンス一覧の中身だけが静かに減る。**diff しないと気づけない類の劣化**なので、移設前に `androidApp/build/generated/aboutLibraries/release/res/raw/aboutlibraries.json` を退避し、移設後の `shared/ui/build/generated/aboutLibraries/release/res/raw/aboutlibraries.json` と突き合わせる。差分が出た依存は `:shared:ui` 側に宣言を寄せるか、対象外でよいかをオーナー判断にする。

## テストの置き場所

**androidTest は 3 本とも `:androidApp` に据え置き、ユニットテストだけ層に沿って割る。**

| ファイル | 移動先 | 件数 |
|---|---|---|
| `InstantConverterTest.kt` | `shared/data/src/test/` | 11 |
| `ItemStatusConverterTest.kt` | `shared/data/src/test/` | 9 |
| `NavigatorTest.kt` | `shared/ui/src/test/` | 11 |
| `HomeViewModelTest.kt` | `shared/ui/src/test/` | 20 |
| `ShoppingViewModelTest.kt` | `shared/ui/src/test/` | 28 |
| `ItemEditViewModelTest.kt` | `shared/ui/src/test/` | 23 |
| `CategoryEditViewModelTest.kt` | `shared/ui/src/test/` | 19 |
| `FakeDatabase.kt` / `TestData.kt` / `MainDispatcherRule.kt` | `shared/ui/src/test/` | ヘルパー |
| `NavigationTest.kt` / `ViewModelScopeTest.kt` | `androidApp/src/androidTest/`（据え置き） | 13 |
| `RoomMigrationTest.kt` | `androidApp/src/androidTest/`（据え置き） | 1 |

`:shared:data` が 20 件、`:shared:ui` が 101 件で合計 121 件。

**ヘルパー 3 本を `:shared:ui` に置く理由:** `FakeDatabase` は package `io.github.obaya884.rebuy.data` だが、使っているのは ViewModel テスト（`:shared:ui`）だけ。`:shared:data` の `src/test` に置くと `:shared:ui` の test から見えない。`testFixtures` で公開する手もあるが、built-in Kotlin との組み合わせが未検証で、段 3 で `commonTest` に移す対象でもある。`shared/ui/src/test/java/io/github/obaya884/rebuy/data/FakeDatabase.kt` とパッケージどおりに切れば中身は 1 行も変わらない。

**`RoomMigrationTest` を `:shared:data` に移さない理由:** androidTest を持つモジュールが 2 つになると GMD の定義も 2 つ・エミュレータ起動も 2 回・レポートパスも 2 箇所になる。spec §8 も「現行 `RoomMigrationTest` は Android 側で維持する」と言っている。**トレードオフ**: `:shared:data` の DB テストが `:androidApp` にある形は局所性が悪く、段 3 で `androidInstrumentedTest` へ移す作業が発生する。段 2 で GMD を 1 本に保つ価値のほうが大きいと判断した。

### `schemas` の移動

`androidApp/schemas/` → `shared/data/schemas/`。ディレクトリ名 `io.github.obaya884.rebuy.data.AppDatabase` は FQCN 由来で Kotlin パッケージは変わらないので、**中身も名前も無変更**。設定は 2 箇所。

- `:shared:data` の `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
- `:androidApp` の `sourceSets.named("androidTest") { assets.directories.add("$projectDir/../shared/data/schemas") }`

2 つ目がモジュール境界をまたぐ。**`project(":shared:data").projectDir` とは書かないこと**——`org.gradle.configuration-cache=true` が入っており、構成時の他プロジェクト参照はプロジェクト分離が有効化されたときに壊れる。素直な相対パス文字列で書く。

現行 `defaultConfig` の `javaCompileOptions.annotationProcessorOptions.arguments` は javac/kapt 向けで KSP には効いていないので、移設時に落としてよい。

## Gradle Managed Device

**`:androidApp` の `testOptions` にだけ定義する。** ルートの `subprojects {}` で共通化する案は config cache とプロジェクト分離に逆行し、spec §3 の「convention plugin は作らない」とも合わない。

タスク名について、spec §11 の「GMD のタスク名に `:androidApp:` 修飾が要る」は**厳しすぎる**。Gradle はルートから修飾なしのタスク名を渡すと全サブプロジェクトから一致するものを探すので、`./gradlew pixel6Api35DebugAndroidTest` は**そのまま動く**（`:androidApp` にしか存在しないので曖昧さも無い）。`.claude/settings.json` の allow も外れない。

ただし**明示的に `:androidApp:pixel6Api35DebugAndroidTest` へ切り替えることを推奨**する。理由は (a) 段 3 で `:shared:*` に androidTest が生まれたとき無修飾だと意図せず複数モジュールで走る、(b) verifier / CI / CLAUDE.md の記述が一意になる。切り替えるなら allow に 2 行を**追加**する（既存 2 行は当面残す）。

```
"Bash(./gradlew :androidApp:pixel6Api35DebugAndroidTest)",
"Bash(./gradlew :androidApp:pixel6Api35DebugAndroidTest -P*)",
```

## Task 1: docs の先行更新

- [ ] **Step 1: spec を確かめる**

namespace（§3 の表）と AboutLibraries の所在は**すでに反映済み**（2026-08-29）。着手時に §3 を読み直し、段 1 の結果で変わった前提が無いか確かめる。

- [ ] **Step 2: T-20 の状態を進行中にする**

T-20 は起票済み（状態は `未着手`）。着手時に一覧の状態を `進行中` に変える。

**完了条件:** `sh scripts/docs-check.sh` 緑。

## Task 2: `:app` を `:androidApp` にリネームする

コードは 1 行も動かさない。**リネームだけで 1 コミット。**

- [ ] `git mv app androidApp`
- [ ] `settings.gradle.kts` の `include(":androidApp")`
- [ ] `.github/workflows/ci.yml` のアーティファクトパス（`app/build/...` → `androidApp/build/...`）
- [ ] `.claude/agents/verifier.md`・`test-reviewer.md` のパス
- [ ] CLAUDE.md の「開発コマンド」

**完了条件:** `./gradlew build` 緑・`./gradlew pixel6Api35DebugAndroidTest` 緑（14 件）・`sh scripts/docs-check.sh` 緑・`grep -rn "\bapp/" CLAUDE.md README.md .claude docs` で残存参照ゼロ（`docs/検討/31` の履歴記述は除外判断をオーナーに確認）。

## Task 3: 空の `:shared:data` を作って配線だけ通す

**ソースは 0 ファイル。** built-in Kotlin の下で library + KSP が configure できることを、最小差分で先に確かめる——このリポジトリで一番読めない部分なので、ここで躓いたら止まれるようにする。

- [ ] `shared/data/build.gradle.kts`（`com.android.library` + `ksp` + `repositories {}` + namespace + compileSdk 37 / minSdk 31 / Java 17）
- [ ] `settings.gradle.kts` に `include(":shared:data")`
- [ ] `:androidApp` に `implementation(project(":shared:data"))`

**完了条件:** `./gradlew build` 緑で `:shared:data:assembleDebug` がタスクグラフに出ている。`./gradlew :shared:data:dependencies` が解決できる。

## Task 4: データ層を `:shared:data` へ

- [ ] `data/**`（`AppDatabase` / `Item` / `Category` / DAO / Converter）を移す
- [ ] Room 依存 3 本と `ksp(room-compiler)`、`ksp { arg(...) }` を `:shared:data` へ
- [ ] `androidApp/schemas` → `shared/data/schemas`
- [ ] Koin の `dataModule` を `:shared:data` へ
- [ ] `:androidApp` から Room と KSP を外す
- [ ] `:androidApp` の androidTest assets を `../shared/data/schemas` に向ける
- [ ] ユニットテスト 2 本（20 件）を `shared/data/src/test` へ

**完了条件:** `./gradlew build` 緑（ユニット 121 件が 2 モジュールに分かれて全数緑）・GMD 緑（`RoomMigrationTest` が assets からスキーマを読めている＝ここが assets 設定の唯一の検証）・`./gradlew installDebug` で起動し品目の追加/削除が移設前と同一。`shared/data/build.gradle.kts` に Compose も koin-compose も無い。

## Task 5: ドメイン層を `:shared:domain` へ

- [ ] `domain/**` と Koin の `domainModule` を移す
- [ ] `api(project(":shared:data"))`
- [ ] `:androidApp` は `:shared:data` への直接依存を落として `implementation(project(":shared:domain"))` に

**完了条件:** `build` 緑・GMD 緑。**`shared/domain/build.gradle.kts` のプラグインが `com.android.library` 1 本だけ**であること（＝レイヤの純度が Gradle で証明されている）。

## Task 6: UI 層を `:shared:ui` へ（最大の山）

namespace の入れ替え・res の移動・`R` の解決先の変化が**分割不能に絡む**ため、**1 コミットにまとめる。**

作業順序（1 コミット内）:

- [ ] **① `:shared:ui` の骨だけ作り、AboutLibraries を適用して `assembleDebug` で `res/raw/aboutlibraries.json` が生成されることを確認する。**ここで躓いたら止まれる
- [ ] ② namespace 入れ替え（`:shared:ui` = `io.github.obaya884.rebuy` / `:androidApp` = `io.github.obaya884.rebuy.app`）と res 移動
- [ ] ③ `ui/**`（`MainActivity` を除く）・`FlowExt.kt`・Koin の `uiModule` を移動
- [ ] ④ `AndroidManifest.xml` の `android:name` を完全修飾へ
- [ ] ⑤ `SettingScreen` の `BuildConfig.VERSION_NAME` 対応
- [ ] ⑥ ユニットテスト 5 本 + ヘルパー 3 本（101 件）を `shared/ui/src/test` へ
- [ ] ⑦ `:androidApp` は `implementation(project(":shared:ui"))` のみ（Koin は `:shared:ui` の `uiModule` だけを読み込む）

**完了条件:**

- `./gradlew build` 緑（ユニット 121 件）
- `./gradlew :androidApp:pixel6Api35DebugAndroidTest` 緑（14 件）
- **`git diff --stat androidApp/src/androidTest` が空**（合否判定そのもの）
- `aboutlibraries.json` の移設前後 diff がゼロ、または説明可能
- `installDebug` で全 6 画面・ライセンス画面・設定画面のバージョン表示が移設前と同一（オーナー動作確認）

## Task 7: 開発基盤の追随

- [ ] `.claude/settings.json` の allow（GMD の修飾名・`--tests` の修飾名）
- [ ] `.claude/agents/test-reviewer.md` の起動契機とテスト表（`androidApp/src/test/**` → `shared/*/src/test/**` + `androidApp/src/androidTest/**`）
- [ ] `.claude/agents/verifier.md` のレポートパス
- [ ] `.claude/agents/spec-reviewer.md` の `androidApp/schemas/` → `shared/data/schemas/`
- [ ] `docs/検討/31_開発基盤検討.md` の `androidApp/schemas/` 参照
- [ ] `ci.yml` の `build` ジョブのアーティファクトを `**/build/reports/` に（ユニットテストのレポートが `shared/*/build/reports/tests/` に分散するため）
- [ ] CLAUDE.md の「技術スタックとビルド」「ビルド構成の注意点」「アーキテクチャ」「開発コマンド」
- [ ] ルート `.gitignore` に `**/build/` を 1 行

**`./gradlew testDebugUnitTest --tests "..."` が壊れる。** CLAUDE.md「開発コマンド」に載っている単一クラス実行で、ユニットテストを持つモジュールが 2 つになると、無修飾で `--tests` を渡したとき一致しない側の `Test` タスクが `No tests found for given includes` で**ビルドを失敗させる**。CLAUDE.md を `./gradlew :shared:data:testDebugUnitTest --tests "..."` の形に直し、allow にも修飾名を追加する。

**完了条件:** `sh scripts/docs-check.sh` 緑。`grep -rn "app/build\|app/src" .claude .github CLAUDE.md` がゼロ。`./gradlew :shared:data:testDebugUnitTest --tests "io.github.obaya884.rebuy.InstantConverterTest"` が allow に引っかからず通る。

## Task 8: レビュー・動作確認・PR

段 1 の Task 7 と同じ手順。レビュー 4 本を並列起動し、実機で全画面を確認し、PR を作って `gh pr checks --watch` をバックグラウンドに置く。マージ後に `sh scripts/ledger-move.sh T-20`。

## spec §11「③ で壊れる開発基盤」の仕分け

| # | 項目 | 段 2 で塞がるか | 中身 |
|---|---|---|---|
| 1 | `./gradlew build` が全モジュール対象 | **段 3** | 段 2 では 4 モジュールになるだけで Linux CI は問題なし。iOS ターゲットを `build` から外す作業は段 3 |
| 2 | GMD のタスク名 + allow | **段 2**（性質が違う） | 無修飾名は**そのまま動く**ので「外れる」は誤り。修飾名を推奨として allow に追加する形で塞ぐ |
| 3 | `androidApp/build/` 決め打ちのレポートパス | **段 2** | Task 2 で `androidApp/build/` へ。加えてユニットテストのレポートが分散するので CI は `**/build/reports/` のグロブに |
| 4 | `test-reviewer` の起動契機 | **段 2 で半分** | `commonTest` への対応は段 3 |
| 5 | CLAUDE.md アーキテクチャ節 → 15 へ | **段 2 は現状更新のみ** | 15 として切り出すのは構造が最終形になる段 3 が適切（段 3 でもう一度大きく書き換わる） |
| 6 | KMP タスク名の allow | **段 3** | 段 2 では KMP タスクが存在しない |

§11 に無いが段 2 で塞ぐ必要があるもの: 上記の `--tests` の件、`spec-reviewer` と `31` の `androidApp/schemas/` 参照、`.gitignore`。

## 落とし穴

1. **`compileOptions` の書き忘れ → JVM target 不一致。** built-in Kotlin は JVM target を `compileOptions.targetCompatibility` から引き継ぐ。新 3 モジュールで `VERSION_17` を書き忘れると既定の 1.8 になり、`:androidApp` とのリンク時に "Inconsistent JVM-target compatibility" で落ちる。convention plugin を作らない決定なので、**4 ファイルに手で 4 回書く**
2. **`minSdk` を library に書き忘れる** → マニフェストマージャが `uses-sdk:minSdkVersion 1 cannot be smaller than version 31` で落ちる
3. **`kotlinOptions {}` を書きたくなったら止まる。** 使えない。`kotlin { compilerOptions {} }`
4. **`ksp {}` と `sourceSets {}` の位置。** `ksp {}` はトップレベル、`sourceSets {}` は `android {}` 直下。`defaultConfig {}` の中に書くと Kotlin DSL では解決できない。`:shared:data` の Room schemaLocation で踏みやすい
5. **`repositories {}` の書き忘れ。** 新モジュール 3 つ全部に要る。忘れると「Could not find androidx.room:room-runtime」で落ちるが原因が見えづらい
6. **`R` の解決先。** namespace 設計を間違えると instrumented 2 ファイルが赤くなり、合否判定に直撃する
7. **`BuildConfig.VERSION_NAME`。** library には生えない。`grep -rn BuildConfig` で 1 箇所しかないので見落としやすい
8. **AboutLibraries の収集範囲。** ビルドは通るがライセンス一覧の中身が静かに減る。diff で確かめないと気づけない
9. **`schemas` の assets パスで `project(":shared:data").projectDir` を使わない。** config cache が有効
10. **Koin なので DI の壊れがコンパイルで出ない。** Task 4〜6 の完了条件から GMD を落とさない
11. **`packaging { resources { excludes ... } }`** は `:androidApp` に残す（APK パッケージング時の設定なので library に置いても効かない）
12. **`proguard-rules.pro`** は `:androidApp` のまま。library に移すと `consumerProguardFiles` の話になり別物。`isMinifyEnabled = false` なので段 2 では実害なし
