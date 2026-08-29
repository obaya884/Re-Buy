# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Re-Buy（仮称）は「くりかえし使える買い物リスト」の Android アプリ（Kotlin + Jetpack Compose、単一モジュール `:app`）。
アプリ内の表示文言・コードコメントは日本語で書かれているので、それに合わせること。

- `applicationId` / `namespace`: `io.github.obaya884.rebuy`
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17
- AGP 9 / Gradle 9。ビルドに使う JDK は 17 以上（Android Studio 同梱の JBR 25 で動作確認済み）
- ビルドスクリプトは Kotlin DSL（`.gradle.kts`）。依存は必ず `gradle/libs.versions.toml`（version catalog）経由で追加する

## よく使うコマンド

```bash
./gradlew assembleDebug          # デバッグビルド
./gradlew build                  # 全ビルド + Lint + ユニットテスト
./gradlew testDebugUnitTest      # ユニットテスト（JVM, app/src/test）
./gradlew connectedDebugAndroidTest   # インストルメンテーションテスト（実機/エミュレータ必須）
./gradlew lint                   # Android Lint
./gradlew installDebug           # 端末へインストール
```

単一テストの実行:

```bash
./gradlew testDebugUnitTest --tests "io.github.obaya884.rebuy.InstantDateFormatStringConverterTest"
./gradlew testDebugUnitTest --tests "*.InstantDateFormatStringConverterTest.test_stringToInstant"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.obaya884.rebuy.RoomMigrationTest
```

KSP（Hilt / Room）の生成コードが壊れたときは `./gradlew clean` の後に再ビルドする。

## ビルド構成の注意点

- **Kotlin は AGP の built-in Kotlin でコンパイルされる。** `org.jetbrains.kotlin.android` プラグインは適用していないので、`app/build.gradle.kts` に足さないこと。
- そのため Kotlin / KSP のバージョンは、ルート `build.gradle.kts` の `buildscript { dependencies { classpath ... } }` で引き上げている。AGP 同梱の KGP より新しいものを使いたい場合はここを直す（version catalog の `kotlin` / `ksp` が実体）。
- `kotlinOptions {}` は使えない。コンパイラオプションが必要なら `kotlin { compilerOptions {} }` を使う。JVM target は `compileOptions.targetCompatibility` から引き継がれるので通常は指定不要。
- `ksp {}` と `android.sourceSets {}` はトップレベル / `android {}` 直下に置くこと。Groovy DSL 時代は `defaultConfig {}` の中に書いても暗黙の委譲で動いていたが、Kotlin DSL では解決できない。
- リポジトリはモジュールの `repositories {}` と `settings.gradle.kts` の `pluginManagement {}` で宣言している。`settings.gradle.kts` の `dependencyResolutionManagement {}` は Gradle 9.7 時点でも `@Incubating` で Kotlin DSL だと警告が出るため、あえて使っていない。モジュールを増やすときは各モジュールに `repositories {}` を書く。
- AGP 10 以降に上げるときは、`gradle.properties` に `android.builtInKotlin` / `android.newDsl` を足して退避する手は使えなくなる（すでに移行済みなので問題ない）。

## アーキテクチャ

UI (Compose) → ViewModel → Repository (`domain/`) → DAO (`data/`) → Room の単方向レイヤ構成。DI は Hilt。

### データ層 (`data/`)

- `AppDatabase`: Room。エンティティは `Item` と `Category` の 2 つ。`exportSchema = true` で `app/schemas/` に JSON を出力する
- `Item.categoryId` → `Category.id` の外部キー（`onDelete = SET_NULL`）。JOIN 済みの読み出しは `ItemWithCategory`（`@Embedded` + `@Relation`）を使う
- 日時は `Instant` で保持し、`InstantDateFormatStringConverter` が `YYYY-MM-DD HH:MM:SS`（UTC）文字列として保存する。0 年未満・10000 年以上は例外になる仕様（テスト済み）
- `ItemStatus` は `NO_DEAL(0)` / `IN_SHOPPING_LIST(1)` / `CHECKED_IN_SHOPPING_LIST(2)` の 3 状態。`ItemStatusConverter` で Int に変換して保存するため、**enum の `value` は既存 DB と互換を壊さない限り変更しない**
- DAO の更新系クエリは `updatedAt = Instant.now()` をデフォルト引数で受け取り、更新のたびにタイムスタンプを書き換える

### スキーマ変更時の手順

`AppDatabase` の `version` を上げ、`Migration` を追加し、`RoomMigrationTest.ALL_MIGRATIONS` に登録する。`app/schemas/` に生成された新しい JSON もコミットすること（`androidTest` はこのディレクトリを assets として参照している）。

### ドメイン層 (`domain/`)

`ItemRepository` / `CategoryRepository` は DAO の薄いラッパー。ステータス遷移（`updateStatusAsInBasket` など）はここに集約されており、同じ状態への更新は早期 return で握りつぶす。ビジネスルールを足すならここ。

### DI (`di/`)

`SingletonComponent` にインストールされた `@Provides` モジュールが 1 依存 1 ファイルで並んでいる（`AppDatabaseModule` → `ItemDaoModule` / `CategoryDaoModule` → `ItemRepositoryModule` / `CategoryRepositoryModule`）。新しい依存を足すときもこの粒度に合わせる。

### UI 層 (`ui/`)

- 画面遷移は `ui/ReBuyApp.kt` の `NavHost` に集約。ルートは `sealed class Screen` で定義し、画面追加時はここに `data object` と `composable(...)` を両方足す
- 画面 Composable のシグネチャは `(navController: NavController, snackbarHostState: SnackbarHostState)` で統一。ViewModel は `hiltViewModel<XxxViewModel>()` で画面内から取得する（引数で渡さない）
- 全画面が `ReBuyAppScaffold` を使い、TopAppBar / BottomBar / Snackbar / FAB の構成を共通化している
- ViewModel は「複数の `MutableStateFlow` を `combine` して 1 つの `XxxScreenUiState` にまとめ、`stateIn(viewModelScope, SharingStarted.Eagerly, 初期値)` で公開する」パターンで統一。Repository の Flow は `init` の `viewModelScope.launch` で collect して private StateFlow に流し込む
- Kotlin 標準の `combine` は 5 引数までなので、6 個をまとめるときはルートの `FlowExt.kt` の自作 `combine` を使う（`ItemEditViewModel` が例）
- ダイアログの開閉フラグも UiState に持たせ、`showXxxDialog()` / `hideXxxDialog()` を ViewModel に生やす
- 派生値（フィルタ済みリストなど）は UiState の `get()` プロパティで計算する（`HomeScreenUiState.inBasketItems` など）
- BottomNavigation を持つ画面の UiState は `BottomNavigationScreenUiState` を実装し、買い物リストのバッジ件数を提供する

### OSS ライセンス表示

AboutLibraries プラグインでビルド時にライセンス情報を生成し、`LicenseScreen` の `LibrariesContainer` が表示する。依存を追加したらここに自動反映されるので手動更新は不要。
