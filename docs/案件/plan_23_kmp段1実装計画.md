# KMP 化 段 1（Koin 化）実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

- 作成日: 2026-08-29
- 位置づけ: [KMP 化検討](../検討/32_KMP化検討.md) §10 の**段 1（DI を Hilt から Koin へ差し替える）**の実装手順。台帳エントリは T-19（[技術改善バックログ](./23_技術改善バックログ.md)）。個々のタスクは台帳に起票せず本書のタスク番号で管理する
- 前段: [段 0 実装計画](./archive_23_kmp段0実装計画.md)（Navigation 3 化・完了）

**Goal:** DI を Hilt から Koin へ差し替える。**モジュールは分割しない**（段 2）。KMP 化もしない（段 3）。この段が終わった時点で、アプリの挙動は差し替え前と同一で、Hilt への依存が 1 つも残っていない。

**Architecture:** Koin モジュールを層ごとに 3 本（`dataModule` / `domainModule` / `uiModule`）に分け、**各モジュールは `includes` で 1 つ下の層だけを知る**。単一モジュールのうちは 3 本とも同じ `di/` に置くが、**段 2 でファイルを移すだけで済む形**にしておく。`Application` は `uiModule` 1 つだけを読み込み、画面は `koinViewModel()` で ViewModel を取る。

**なぜ分割より先にやるか:** Hilt のままモジュールを分割すると、各モジュールに `hilt-android` と `ksp(hilt-compiler)` を配線する作業が発生し、段 3 で丸ごと捨てることになる。Koin なら KSP もアノテーション処理も要らないので、段 2 で配線が要る KSP は Room の分だけになる（[KMP 化検討](../検討/32_KMP化検討.md) §10）。

**Tech Stack:** Kotlin 2.4.10 / AGP 9.3.2（built-in Kotlin）/ Compose BOM 2026.08.00 / Navigation 3 1.1.6 / Room 2.8.4 / Koin（BOM。バージョンは着手時に最新の安定版を確認する）/ Gradle Managed Device `pixel6Api35`

**Spec:** [docs/検討/32_KMP化検討.md](../検討/32_KMP化検討.md) §5

## Global Constraints

- **挙動を変えない**。画面の見た目・遷移・データの読み書きを変えない。変わったら実装が誤り
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17。**変えない**
- **Kotlin は AGP の built-in Kotlin でコンパイルされる。`org.jetbrains.kotlin.android` プラグインを足さない**
- **`ksp` プラグインは残す。** Room が使っている。外すのは Hilt 用の KSP 依存だけ
- 依存は必ず `gradle/libs.versions.toml` 経由で追加する
- `kotlinOptions {}` は使えない。`ksp {}` と `android.sourceSets {}` はトップレベル / `android {}` 直下に置く
- コードの変更は Edit / Write ツールで 1 箇所ずつ。機械的な一括置換に限り sed / python を使ってよく、その場合は直後に `git diff` を提示する
- git 運用: **ブランチ `t-19-koin` → PR → CI 緑 → オーナーの動作確認 → 合図で squash マージ**
- 表のセル内に `|` を書かない（`docs-check` が列を数えられなくなる）
- 完了した台帳エントリは、状態更新と同じコミットで `closed_23` へ移す（`sh scripts/ledger-move.sh T-19`）
- コミットメッセージは日本語。既存コミットに倣って `Co-Authored-By` と `Claude-Session` のトレーラを付ける

## 合否判定

**ユニットテスト 121 件が 1 行も変わらずに緑であること。** ViewModel のテストは ViewModel を直接生成しており DI に触れていないので、差し替えで 1 文字も変わらないはず。変える必要が出たら、それは差し替えが ViewModel の形を変えてしまった証拠。

**instrumented は Hilt のテスト用の足場を外すぶんだけ変わる。** アサーションと操作手順は変えない。

## Koin では壊れがコンパイルで出ない

Hilt は依存グラフをコンパイル時に検証するので、配線ミスはビルドエラーになった。**Koin は起動時まで分からない。** そのため各タスクの完了条件から GMD を落とさないこと。`./gradlew build` だけで「緑」と判定すると、`koinViewModel()` が解決できないアプリが緑で通る。

網になるのは `NavigationTest`（12 件）で、これは全画面を開くので、4 つの ViewModel すべてが Koin で解決できることを実質的に確かめている。

## 現状の DI

| ファイル | 中身 |
|---|---|
| `di/AppDatabaseModule.kt` | `AppDatabase.getDatabase(context)` を `@Singleton` で提供 |
| `di/ItemDaoModule.kt` | `appDatabase.itemDao()` |
| `di/CategoryDaoModule.kt` | `appDatabase.categoryDao()` |
| `di/ItemRepositoryModule.kt` | `ItemRepository(itemDao)` |
| `di/CategoryRepositoryModule.kt` | `CategoryRepository(categoryDao)` |
| `ReBuyApplication.kt` | `@HiltAndroidApp` |
| `ui/activity/MainActivity.kt` | `@AndroidEntryPoint` |
| ViewModel 4 本 | `@HiltViewModel class Xxx @Inject constructor(...)` |
| 画面 4 本 | `hiltViewModel<XxxViewModel>()`（Home / Shopping / CategoryEdit / ItemEdit） |
| `androidTest/HiltTestRunner.kt` | `HiltTestApplication` を差し込むランナー |
| `NavigationTest` / `ViewModelScopeTest` | `@HiltAndroidTest` ＋ `HiltAndroidRule` ＋ `hiltRule.inject()` |

Setting 画面と License 画面は ViewModel を持たないので触らない。

## Task 1: 起票とブランチ

- [ ] **Step 1: T-19 の状態を進行中にする**

T-19 は起票済み（状態は `未着手`）。着手時に一覧の状態を `進行中` に変える。

- [ ] **Step 2: ブランチを切る**

```
git checkout main && git pull
git checkout -b t-19-koin
```

**完了条件:** `sh scripts/docs-check.sh` 緑。`git branch --show-current` が `t-19-koin`。

## Task 2: Koin の依存を通す

Hilt はまだ外さない。**依存が解決してビルドが通ることだけ**を確かめる。

- [ ] **Step 1: version catalog に Koin を足す**

`gradle/libs.versions.toml` に BOM で入れる。**バージョンは着手時に最新の安定版を確認すること**——本書の作成時点で決め打ちにすると古くなる。

```
[versions]
koin = "<着手時に確認>"

[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel" }
```

`koin-compose-viewmodel` を選ぶのは、段 3 で `commonMain` にそのまま持っていける artifact だから（spec §5）。Android 専用の `koin-androidx-compose` を使うと段 3 で差し替えになる。

- [ ] **Step 2: `app/build.gradle.kts` に依存を足す**

`dependencies` に BOM を `implementation(platform(libs.koin.bom))` で入れ、`koin-android` と `koin-compose-viewmodel` を足す。Compose BOM と同じ書き方に揃える。

**完了条件:** `./gradlew build` 緑。`./gradlew app:dependencies --configuration debugRuntimeClasspath | grep koin` で 3 つとも解決している。**この時点ではコードは 1 行も変わっていない。**

## Task 3: Koin モジュールを書く（まだ使わない）

- [ ] **Step 1: 層ごとに 3 本書く**

`di/` に置く。**粒度が「1 依存 1 ファイル」から「1 層 1 ファイル」に変わる**——Koin では依存の宣言が数行で、1 依存 1 ファイルにすると層の境界が読めなくなるため。段 2 でこの 3 ファイルがそのまま 3 モジュールへ移る。

```kotlin
// di/DataModule.kt
val dataModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().categoryDao() }
}

// di/DomainModule.kt
val domainModule = module {
    includes(dataModule)

    singleOf(::ItemRepository)
    singleOf(::CategoryRepository)
}

// di/UiModule.kt — アプリが読み込む唯一の入口
val uiModule = module {
    includes(domainModule)

    viewModelOf(::HomeViewModel)
    viewModelOf(::ShoppingViewModel)
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::ItemEditViewModel)
}
```

**`includes` で 1 つ下の層だけを知る形にする。** 3 本を平坦なリストで束ねると、束ねるファイルが全層の名前を知ることになり、段 2 でモジュールを割ったときに `:shared:ui` の中に `:shared:data` の名前が出る。`includes` の連なりなら Gradle の依存の向きとそのまま対応する。

Hilt 側は DAO に `@Singleton` を付けていないが、`AppDatabase` がシングルトンで `itemDao()` が同じ実体を返すため実質シングルトンだった。`single` にすると挙動が変わらない。

**完了条件:** `./gradlew build` 緑。**まだ誰も `uiModule` を読み込んでいないので挙動は変わらない。**

## Task 4: 差し替える

**ここは 1 コミットにまとめる。** Hilt と Koin が半分ずつの状態でアプリは動かない。

- [ ] **Step 1: `ReBuyApplication` で Koin を起動する**

`@HiltAndroidApp` を外し、`onCreate` で `startKoin` する。

```kotlin
class ReBuyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReBuyApplication)
            modules(uiModule)
        }
    }
}
```

- [ ] **Step 2: `MainActivity` の `@AndroidEntryPoint` を外す**

Koin には対応する注釈が無い。import も消す。

- [ ] **Step 3: ViewModel 4 本から Hilt の注釈を外す**

`@HiltViewModel` と `@Inject` を外す。コンストラクタは `class HomeViewModel(private val itemRepository: ItemRepository, ...)` になる。**引数の順序と型は変えない**——ユニットテストが名前付き引数で呼んでいる。

- [ ] **Step 4: 画面 4 本の `hiltViewModel()` を `koinViewModel()` に替える**

`HomeScreen` / `ShoppingScreen` / `CategoryEditScreen` / `ItemEditScreen` の各 1 箇所。import も差し替える。

- [ ] **Step 5: `NavigationState.kt` の KDoc を追随させる**

74 行目付近に「`hiltViewModel()` が Activity の `ViewModelStore` にフォールバックする」という説明がある。`koinViewModel()` でも同じ話なので、名前だけ直す。

- [ ] **Step 6: instrumented テストから Hilt の足場を外す**

`NavigationTest` と `ViewModelScopeTest` から `@HiltAndroidTest`・`HiltAndroidRule`・`setUp()` の `hiltRule.inject()` を外す。ルールが 1 本になるので `@get:Rule(order = 1)` は `@get:Rule` に戻す。**アサーションと操作手順は 1 行も変えない。**

`HiltTestRunner.kt` を削除し、`app/build.gradle.kts` の `testInstrumentationRunner` を `"androidx.test.runner.AndroidJUnitRunner"` に戻す。

これで instrumented は**本物の `ReBuyApplication`**（＝ `startKoin` 済み）で走る。実 DB を使う点は変わらない（T-21 が扱う）。

**完了条件:** `./gradlew build` 緑（ユニット 121 件）。**`./gradlew pixel6Api35DebugAndroidTest` 緑（14 件）——ここが実質の合否判定**。`git diff app/src/test` が空。

## Task 5: Hilt を外す

- [ ] **Step 1: ビルドスクリプトから外す**

`app/build.gradle.kts` から次を消す。

- `alias(libs.plugins.google.dagger.hilt)`
- `ksp(libs.google.dagger.hilt.compiler)`
- `implementation(libs.google.dagger.hilt.android)`
- `implementation(libs.androidx.hilt.navigation.compose)`
- `androidTestImplementation(libs.google.dagger.hilt.android.testing)`
- `kspAndroidTest(libs.google.dagger.hilt.compiler)`

**`alias(libs.plugins.ksp)` と `ksp(libs.androidx.room.compiler)` は残す。** Room が使っている。

ルート `build.gradle.kts` の `plugins {}` にも Hilt の `apply false` があれば消す。

- [ ] **Step 2: version catalog から外す**

`google-dagger-hilt` の version と、`google-dagger-hilt-android` / `-compiler` / `-android-testing` / `androidx-hilt-navigation-compose` の library、`google-dagger-hilt` の plugin を消す。`androidx-hilt` の version が他で使われていないなら一緒に消す。

- [ ] **Step 3: 残骸が無いことを確かめる**

```
grep -rni "hilt\|dagger" app/src gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts
```

**0 件になること。**

**完了条件:** `./gradlew clean build` 緑（KSP の生成物が残っていると誤って通ることがあるので `clean` を挟む）。`pixel6Api35DebugAndroidTest` 緑。上の grep が 0 件。

## Task 6: docs を追随させる

- [ ] **Step 1: CLAUDE.md を直す**

| 箇所 | 直す内容 |
|---|---|
| 技術スタックの 1 行 | `Room / Hilt / Navigation 3 / AboutLibraries` → `Room / Koin / Navigation 3 / AboutLibraries` |
| アーキテクチャ冒頭 | 「DI は Hilt」→「DI は Koin」 |
| `### DI (di/)` 節 | 「1 依存 1 ファイル」の説明を「層ごとに 1 つの Koin モジュールを置き、`includes` で 1 つ下の層だけを知る」に書き換える。**なぜ粒度を変えたか**（Koin では宣言が 1 行で済み、1 依存 1 ファイルにすると層の境界が読めなくなる）も書く |
| `### UI 層 (ui/)` 節 | `hiltViewModel<XxxViewModel>()` → `koinViewModel<XxxViewModel>()` |

- [ ] **Step 2: spec の段 1 を完了にする**

[KMP 化検討](../検討/32_KMP化検討.md) §10 の段 1 に完了印を付ける必要は無い（§10 は計画表）。代わりに [ロードマップ](./24_ロードマップ.md) の ③ の現在地を更新する。

**完了条件:** `sh scripts/docs-check.sh` 緑。`grep -rni "hilt" CLAUDE.md docs/仕様 docs/検討` が、履歴文書（`log_` / `archive_` / `closed_`）以外で 0 件。

## Task 7: レビュー・動作確認・PR

- [ ] **Step 1: レビューを並列で起動する**

差分が本番コード・テスト・docs に触れるので 4 本すべて。`verifier` と `code-quality-reviewer` は常時、`test-reviewer`（instrumented を触った）と `spec-reviewer`（docs を触った）も条件に当たる。

- [ ] **Step 2: 実機で確認する**

```
./gradlew installDebug
```

**Koin は解決漏れが起動時に出る**ので、全画面を 1 回ずつ開くこと。確認する経路: ホーム → 品目編集 → カテゴリー編集 → 設定 → ライセンス → 買い物。加えてカゴへの出し入れ・買い物の終了（DB の読み書きが Koin 経由で動いていること）。

- [ ] **Step 3: PR を作り、CI を監視する**

```
gh pr create --base main --head t-19-koin --title "③ 段 1: DI を Koin にする（T-19）"
gh pr checks <番号> --watch
```

`--watch` はバックグラウンドで走らせ、確定したら報告する。

- [ ] **Step 4: マージと台帳の更新**

オーナーの合図を待って squash マージ。`sh scripts/ledger-move.sh T-19` で完了記録へ移し、状態を `完了 YYYY-MM-DD` にする。

## 落とし穴

1. **Koin は解決漏れがコンパイルで出ない。** `./gradlew build` の緑を合否にしないこと。GMD を必ず回す
2. **`hilt-navigation-compose` を外すと `hiltViewModel()` が消える。** Task 4 で全 4 箇所を置換してから Task 5 に進む。順序を逆にするとコンパイルエラーの山になる
3. **`ksp` プラグインを Hilt と一緒に消さない。** Room が使っている
4. **`clean` を挟んでから最終確認する。** KSP の生成物が残っていると、Hilt の依存を消してもビルドが通ってしまう場合がある
5. **ViewModel のコンストラクタ引数の順序を変えない。** ユニットテストが名前付き引数で呼んでいるので名前を変えても壊れるが、順序を変えると気づきにくい形で壊れる
6. **`startKoin` を 2 回呼ばない。** instrumented が本物の `Application` を使うので、テスト側で `startKoin` を足すと `KoinApplicationAlreadyStartedException` になる
7. **`koinViewModel()` の Koin コンテキスト解決**。`startKoin` のグローバルコンテキストから引ける想定だが、`koin-compose` のバージョンによっては `KoinContext` を明示的に張る必要がある。Task 4 の GMD で落ちたらここを疑う
