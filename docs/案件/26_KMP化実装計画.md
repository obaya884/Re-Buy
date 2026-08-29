# KMP 化（ロードマップ ③）実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

- 作成日: 2026-08-29
- 位置づけ: [KMP 化検討](../検討/32_KMP化検討.md) の実装手順。spec §10 の 3 段のうち **段 0（Navigation 3 化）だけを本書に展開する**。段 1（KMP 移植）と段 2（iOS シェル）は、前の段が完了した時点で本書に追記する——後の段の手順は前の段の結果に依存しており、先に書いても推測になるため
- 段と台帳の対応: 段 0 = T-18 ／ 段 1 = T-19 ／ 段 2 = T-20（[技術改善バックログ](./23_技術改善バックログ.md)）。個々のタスクは台帳に起票せず本書のタスク番号で管理する

**Goal:** Android 単体のまま `NavHost` を Navigation 3 に置き換え、挙動を一切変えずに、iOS のネイティブシェルと同期できる「backstack を自分で持つ」形にする。

**Architecture:** 公式の [Navigation 2 → 3 移行ガイド](https://developer.android.com/guide/navigation/navigation-3/migration-guide)の構成をそのまま採る。`NavigationState`（トップレベルルートごとの backstack を持つ状態）と `Navigator`（遷移イベントを受けて状態を更新する）を追加し、`NavController` を捨てる。`NavHost` は `NavDisplay` に置き換え、画面の定義は `entryProvider` に移す。挙動が変わっていないことは、**移行前に書いた特性テスト（characterization test）が移行後も緑のまま通ること**で確かめる。

**Tech Stack:** Kotlin 2.4.10 / AGP 9.3.2（built-in Kotlin）/ Compose BOM 2026.08.00 / Navigation 3 1.1.6 / lifecycle-viewmodel-navigation3 2.11.0 / kotlinx-serialization / Hilt 2.60.1（段 0 では維持）/ Gradle Managed Device `pixel6Api35`

**Spec:** [docs/検討/32_KMP化検討.md](../検討/32_KMP化検討.md)

## Global Constraints

- **挙動を変えない**。画面の見た目・遷移の結果・戻るの効き方を変えない。変わったら実装が誤り
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17。**変えない**
- **Kotlin は AGP の built-in Kotlin でコンパイルされる。`org.jetbrains.kotlin.android` プラグインを足さない**。Kotlin 由来のプラグインが要るときはルート `build.gradle.kts` の `buildscript { dependencies { classpath ... } }` に足す
- 依存は必ず `gradle/libs.versions.toml` 経由で追加する
- `kotlinOptions {}` は使えない。`ksp {}` と `android.sourceSets {}` はトップレベル / `android {}` 直下に置く
- コードの変更は Edit / Write ツールで 1 箇所ずつ。機械的な一括置換に限り sed / python を使ってよく、その場合は直後に `git diff` を提示する
- git 運用: 段 0 はコードと挙動に触れるので **ブランチ `t-18-nav3` → PR → CI 緑 → オーナーの動作確認 → 合図で squash マージ**
- 表のセル内に `|` を書かない（`docs-check` が列を数えられなくなる）
- 完了した台帳エントリは、状態更新と同じコミットで `closed_23` へ移す（`sh scripts/ledger-move.sh T-18`）
- コミットメッセージは日本語。本書のコミット例は要点だけを書いているので、実際には既存コミットに倣って `Co-Authored-By` と `Claude-Session` のトレーラを付ける

## 移行ガイドの前提条件との照合

移行ガイドは AI エージェントに対して、着手前に前提・非対応機能を照合するよう求めている。照合済みの結果を以下に置く。**着手時に再確認は不要。**

| ガイドの条件 | 本プロジェクトの状態 |
|---|---|
| compileSdk 36 以上 | 37。適合 |
| minSdk 23 以上 | 31。適合 |
| 遷移先が Composable 関数 | 6 画面すべて Composable。適合 |
| ルートが型安全 | **不適合**。`sealed class Screen(val route: String)` の文字列ルート。ただし 6 ルートすべて引数を持たない `data object` なので、Nav2 の型安全ルートを経由せず直接 `NavKey` にする（Task 4）。引数の受け渡しが無いため中間段階を挟む利得がない |
| トップレベルルートが複数あり各々 backstack を持つ | ホームと買い物の 2 つ。適合 |
| タブを切り替えても各スタックの状態が保持される | 現行は `saveState` / `restoreState` で保持。適合 |
| ホーム画面から出てアプリを終了する | 開始遷移先はホーム。適合 |
| 一括で移行する（Nav2 と Nav3 の併存をしない） | そうする |
| ダイアログ遷移先 | **無し**。`TextFieldAddDialog` 等は UiState のフラグで出す Composable で、遷移先ではない |
| ViewModel を使う（レシピ対象） | 使う。`rememberViewModelStoreNavEntryDecorator()` を entry decorator に足して対応する（Task 3） |
| 2 段以上のネストしたナビゲーション | 無し |
| スタック間を移動する共有遷移先 | 無し。設定・ライセンス・アイテム・カテゴリはホームのスタックからのみ到達する |
| カスタム遷移先型 | 無し |
| ディープリンク | 無し |

## 現行の遷移の全体像

移行後もこの通りに動かす。

| 起点 | 操作 | 結果 |
|---|---|---|
| ホーム | TopAppBar のアイテムアイコン | アイテム一覧へ push |
| ホーム | TopAppBar のカテゴリアイコン | カテゴリ一覧へ push |
| ホーム | TopAppBar の設定アイコン | 設定へ push |
| 設定 | 「ライセンス」行 | ライセンスへ push |
| 設定・ライセンス・アイテム・カテゴリ | 戻るアイコン / 端末の戻る | 1 つ pop |
| ホーム・買い物 | ボトムナビ | タブを切り替える。各タブの履歴は保持される |
| 買い物 | 「買い物を終わる」→ 確認ダイアログで確定 | ホームタブへ移り、買い物タブのスタックはルートに戻る |

最後の行は現行 `NavController.navigateAsRoot()` の挙動。移行後は `Navigator.navigateAsRoot()` が担う（Task 3）。

---

### Task 1: ナビゲーションの特性テストを用意する

移行前に、現行 Nav2 の挙動を固定するテストを書いて緑にする。**このテストは移行後も 1 行も変えずに緑であること**が段 0 の合否判定になる。

**Files:**
- Modify: `docs/案件/23_技術改善バックログ.md`（T-18 の起票）
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/ReBuyAppScaffold.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/home/HomeScreen.kt`
- Create: `app/src/androidTest/java/io/github/obaya884/rebuy/HiltTestRunner.kt`
- Create: `app/src/androidTest/java/io/github/obaya884/rebuy/ui/NavigationTest.kt`

**Interfaces:**
- Produces: `top_app_bar_title` / `home_item_edit_button` / `home_category_edit_button` / `home_settings_button` の 4 つの testTag。Task 4 で画面を書き換えるときも**この 4 つを消さない**

- [ ] **Step 1: T-18 を台帳に起票して main へ直コミット**

`docs/案件/23_技術改善バックログ.md` の一覧の末尾に 1 行足す。

```
| T-18 | ③ 段 0 Navigation 3 化 | 内部設計 | 高 | 進行中 | [詳細](#t-18) |
```

詳細節の末尾に足す。

```markdown
### T-18

- 背景: ③ で iOS の外枠を SwiftUI が持つため、共有側は backstack を自分で持つ形である必要がある。現行の `NavHost` は backstack を内部に隠しており、SwiftUI の `NavigationStack` と二重管理になる
- 対応方針: Android 単体のまま Navigation 3 へ移行する。KMP 移植と同時にやると落ちたときに切り分けられないので段を分ける
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §10 ／ [実装計画](./26_KMP化実装計画.md)
```

```bash
sh scripts/docs-check.sh
git add docs/案件/23_技術改善バックログ.md
git commit -m "T-18（③ 段 0 Navigation 3 化）を起票"
git push
```

- [ ] **Step 2: ブランチを切る**

```bash
git checkout -b t-18-nav3
```

- [ ] **Step 3: Hilt のテスト依存を catalog に足す**

`gradle/libs.versions.toml` の `[libraries]` に足す。

```toml
google-dagger-hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "google-dagger-hilt" }
```

- [ ] **Step 4: `app/build.gradle.kts` にテスト依存とテストランナーを設定する**

`defaultConfig` の `testInstrumentationRunner` を差し替える。

```kotlin
testInstrumentationRunner = "io.github.obaya884.rebuy.HiltTestRunner"
```

`dependencies` の Test ブロックに足す。

```kotlin
androidTestImplementation(libs.google.dagger.hilt.android.testing)
kspAndroidTest(libs.google.dagger.hilt.compiler)
```

- [ ] **Step 5: `HiltTestRunner` を作る**

Hilt のテストは `HiltTestApplication` を使う必要があり、既定の `AndroidJUnitRunner` では `ReBuyApplication` が起動してしまう。

```kotlin
package io.github.obaya884.rebuy

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

- [ ] **Step 6: 画面の判定に使う testTag を足す**

現行の TopAppBar のアイコンはすべて `contentDescription = null` で、セマンティクスから触れない。テストのために `testTag` を足す。**`contentDescription` は変えない**——読み上げの内容が変わるのは挙動の変更にあたるため、アクセシビリティの改善は ④ で別途扱う。

`ReBuyAppScaffold.kt` の `TopAppBar` のタイトルに足す（インポート `androidx.compose.ui.Modifier` と `androidx.compose.ui.platform.testTag` が要る）。

```kotlin
title = { Text(topBarTitle, modifier = Modifier.testTag("top_app_bar_title")) },
```

`HomeScreen.kt` の `topBarActions` の 3 つの `IconButton` にそれぞれ足す。

```kotlin
IconButton(
    modifier = Modifier.testTag("home_item_edit_button"),
    onClick = { navController.navigate(Screen.ItemEdit.route) }
) {
```

```kotlin
IconButton(
    modifier = Modifier.testTag("home_category_edit_button"),
    onClick = { navController.navigate(Screen.CategoryEdit.route) }
) {
```

```kotlin
IconButton(
    modifier = Modifier.testTag("home_settings_button"),
    onClick = { navController.navigate(Screen.Setting.route) }
) {
```

- [ ] **Step 7: 特性テストを書く**

`app/src/androidTest/java/io/github/obaya884/rebuy/ui/NavigationTest.kt` を作る。**DB の中身に依存しない遷移だけを対象にする**——データを用意する必要があるケース（買い物終了）は自動化せず、Task 6 のオーナー動作確認で見る。

```kotlin
package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.obaya884.rebuy.ui.activity.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag("top_app_bar_title").assertTextEquals(title)
    }

    @Test
    fun 起動直後はホームが表示される() {
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ホームから設定へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_settings_button").performClick()
        assertCurrentScreenIs("設定")

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun 設定からライセンスへ遷移して端末の戻るで設定に帰る() {
        composeRule.onNodeWithTag("home_settings_button").performClick()
        composeRule.onNodeWithText("ライセンス").performClick()
        assertCurrentScreenIs("ライセンス")

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertCurrentScreenIs("設定")
    }

    @Test
    fun ホームからアイテム一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_item_edit_button").performClick()
        assertCurrentScreenIs("アイテム")

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ホームからカテゴリー一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_category_edit_button").performClick()
        assertCurrentScreenIs("カテゴリー")

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ボトムナビでホームと買い物を往復できる() {
        composeRule.onNodeWithText("買い物").performClick()
        assertCurrentScreenIs("買い物")

        composeRule.onNodeWithText("ホーム").performClick()
        assertCurrentScreenIs("ホーム")
    }
}
```

- [ ] **Step 8: テストが緑になることを確認する**

Run: `./gradlew pixel6Api35DebugAndroidTest`
Expected: `NavigationTest` の 6 件と `RoomMigrationTest` が PASS。

**赤になったら移行に進まない。** 特性テストは現行の挙動を写し取るものなので、ここで赤なら想定した挙動が実際と違っているということ。テストの側を実際の挙動に合わせて直す。

- [ ] **Step 9: コミット**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main app/src/androidTest
git commit -m "ナビゲーションの特性テストを追加（T-18）"
```

---

### Task 2: Navigation 3 と kotlinx-serialization の依存を通す

コードは書かず、**依存が解決してビルドが通ることだけ**を確かめる。AGP の built-in Kotlin と serialization プラグインの組み合わせがこのリポジトリで成立するかは未検証で、ここが段 0 で最も転びやすい。

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`（ルート）
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `libs.androidx.navigation3.runtime` / `libs.androidx.navigation3.ui` / `libs.androidx.lifecycle.viewmodel.navigation3` と、`org.jetbrains.kotlin.plugin.serialization` プラグイン

- [ ] **Step 1: catalog にバージョンとライブラリを足す**

`[versions]` に足す。

```toml
nav3Core = "1.1.6"
kotlinx-serialization-json = "1.9.0"
```

`[libraries]` に足す。`lifecycleViewmodelNav3` は既存の `androidx-lifecycle`（2.11.0）と同じバージョン系列なので、新しい version は作らず既存を参照する。

```toml
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "androidx-lifecycle" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization-json" }
kotlin-serialization-gradle-plugin = { group = "org.jetbrains.kotlin", name = "kotlin-serialization", version.ref = "kotlin" }
```

`[plugins]` に足す。

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: ルート `build.gradle.kts` の buildscript classpath に serialization プラグインを足す**

built-in Kotlin を使っているため、Kotlin コンパイラプラグインはここから供給する（既存の `kotlin` / `ksp` と同じ扱い）。

```kotlin
classpath(libs.kotlin.serialization.gradle.plugin)
```

`org.jetbrains.kotlin:kotlin-serialization` が解決できない場合は、プラグインマーカー座標 `org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin` を同じ Kotlin バージョンで指定する。

- [ ] **Step 3: `app/build.gradle.kts` にプラグインと依存を足す**

`plugins` ブロックに足す。

```kotlin
alias(libs.plugins.kotlin.serialization)
```

`dependencies` の Navigation ブロックを差し替える（Nav2 の削除は Task 5 で行うので、この時点では**両方入っている**）。

```kotlin
// Navigation
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.navigation3.runtime)
implementation(libs.androidx.navigation3.ui)
implementation(libs.androidx.lifecycle.viewmodel.navigation3)
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 4: ビルドが通ることを確認する**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL。

失敗した場合は Step 2 の代替座標を試す。それでも通らない場合は**ここで止めてオーナーに報告する**——built-in Kotlin と serialization プラグインが両立しないなら、段 0 の設計（`rememberNavBackStack` による状態保存）を組み直す必要があり、実装で回避してよい判断ではない。

- [ ] **Step 5: コミット**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "Navigation 3 と kotlinx-serialization の依存を追加（T-18）"
```

---

### Task 3: NavigationState と Navigator を追加する

移行ガイドが配布している状態保持クラスをそのまま置く。この時点では誰も使わないので挙動は変わらない。

**Files:**
- Create: `app/src/main/java/io/github/obaya884/rebuy/ui/navigation/NavigationState.kt`
- Create: `app/src/main/java/io/github/obaya884/rebuy/ui/navigation/Navigator.kt`

**Interfaces:**
- Produces: `rememberNavigationState(startRoute: NavKey, topLevelRoutes: Set<NavKey>): NavigationState` ／ `NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>>` ／ `NavigationState.topLevelRoute: NavKey` ／ `Navigator.navigate(route: NavKey)` ／ `Navigator.goBack()` ／ `Navigator.navigateAsRoot(route: NavKey)`。Task 4 がこれらを使う

- [ ] **Step 1: `NavigationState.kt` を作る**

移行ガイドのコードをパッケージだけ合わせて置く。**`rememberSerializable` を `rememberSaveable` に変えない**（ガイドが明示している）。`toEntries` の decorator には、ガイドの保存用に加えて **ViewModel 用の decorator を足す**——本アプリは全画面が `hiltViewModel()` を使っており、これが無いと ViewModel が entry ごとにスコープされない。

```kotlin
package io.github.obaya884.rebuy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * 構成変更とプロセス death をまたいで保持されるナビゲーション状態を作る。
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

/**
 * ナビゲーション状態の保持者。
 *
 * @param startRoute 開始ルート。ユーザーはここからアプリを抜ける
 * @param topLevelRoute 現在のトップレベルルート
 * @param backStacks トップレベルルートごとの backstack
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * NavigationState を NavEntry の列に変換する。
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>()
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
```

`androidx.compose.runtime.getValue` / `setValue` のインポートが要る場合は IDE の補完に従って足す（`by topLevelRoute` の委譲に必要）。

- [ ] **Step 2: `Navigator.kt` を作る**

ガイドの `navigate` / `goBack` に加えて、現行の `NavController.navigateAsRoot()` に対応する `navigateAsRoot` を持たせる。買い物終了時にホームへ戻り、買い物タブの履歴をルートまで畳む挙動を再現する。

```kotlin
package io.github.obaya884.rebuy.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * ナビゲーションのイベント（前進・後退）を受けて状態を更新する。
 */
class Navigator(val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // トップレベルルートなので切り替えるだけ
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("${state.topLevelRoute} のスタックが無い")
        val currentRoute = currentStack.last()

        // 現在のトップレベルルートの根にいるなら、開始ルートのスタックへ戻る
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * すべてのスタックを根まで畳んでから指定のトップレベルルートへ移る。
     * 買い物終了時にホームへ戻る動きに使う。
     */
    fun navigateAsRoot(route: NavKey) {
        state.backStacks.values.forEach { stack ->
            while (stack.size > 1) {
                stack.removeLastOrNull()
            }
        }
        state.topLevelRoute = route
    }
}
```

- [ ] **Step 3: ビルドが通ることを確認する**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL。未使用の警告は出てよい。

- [ ] **Step 4: コミット**

```bash
git add app/src/main/java/io/github/obaya884/rebuy/ui/navigation
git commit -m "NavigationState と Navigator を追加（T-18）"
```

---

### Task 4: NavHost を NavDisplay に置き換える

ここが一括変更。ルートの `NavKey` 化・`entryProvider` 化・画面シグネチャの差し替え・ボトムナビの判定変更を**1 コミットで**行う。途中の状態はコンパイルが通らないので分割できない。

**Files:**
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/ReBuyApp.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/BottomNavigationBar.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/BottomNavigationItem.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/home/HomeScreen.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/shopping/ShoppingScreen.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/setting/SettingScreen.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/license/LicenseScreen.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/item_edit/ItemEditScreen.kt`
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/screen/category_edit/CategoryEditScreen.kt`

**Interfaces:**
- Consumes: Task 3 の `rememberNavigationState` / `toEntries` / `Navigator`
- Produces: 画面 Composable の新しいシグネチャ `(navigator: Navigator, snackbarHostState: SnackbarHostState)`。Task 6 で CLAUDE.md に反映する

- [ ] **Step 1: `ReBuyApp.kt` を書き換える**

ファイル全体を次で置き換える。`Screen` は `NavKey` を実装した `@Serializable` な `data object` になり、`route` 文字列は消える。

```kotlin
package io.github.obaya884.rebuy.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.navigation.rememberNavigationState
import io.github.obaya884.rebuy.ui.navigation.toEntries
import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.rebuy.ui.screen.home.HomeScreen
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.rebuy.ui.screen.license.LicenseScreen
import io.github.obaya884.rebuy.ui.screen.setting.SettingScreen
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingScreen
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import kotlinx.serialization.Serializable

@Composable
fun ReBuyApp() {
    val snackbarHostState = remember { SnackbarHostState() }

    val navigationState = rememberNavigationState(
        startRoute = Screen.Home,
        topLevelRoutes = setOf(Screen.Home, Screen.Shopping)
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = entryProvider<NavKey> {
        entry<Screen.Home> { HomeScreen(navigator, snackbarHostState) }
        entry<Screen.Shopping> { ShoppingScreen(navigator, snackbarHostState) }
        entry<Screen.Setting> { SettingScreen(navigator, snackbarHostState) }
        entry<Screen.CategoryEdit> { CategoryEditScreen(navigator, snackbarHostState) }
        entry<Screen.ItemEdit> { ItemEditScreen(navigator, snackbarHostState) }
        entry<Screen.License> { LicenseScreen(navigator, snackbarHostState) }
    }

    ReBuyTheme {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}

sealed class Screen : NavKey {
    @Serializable
    data object Home : Screen()

    @Serializable
    data object Setting : Screen()

    @Serializable
    data object CategoryEdit : Screen()

    @Serializable
    data object ItemEdit : Screen()

    @Serializable
    data object Shopping : Screen()

    @Serializable
    data object License : Screen()
}
```

`entryProvider` が `sealed class` の各サブタイプを解決できない場合は、`sealed class Screen : NavKey` をやめて `@Serializable data object Home : NavKey` の 6 つの独立した宣言にする（移行ガイドの形）。その場合 `Screen.` の修飾が外れるので参照側も直す。

- [ ] **Step 2: `BottomNavigationItem.kt` から route 文字列を外す**

`route: String` を捨てて `NavKey` を持たせる。

```kotlin
package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.R
import io.github.obaya884.rebuy.ui.Screen

sealed class BottomNavigationItem(val key: NavKey, val icon: ImageVector, val titleId: Int) {
    data object Home :
        BottomNavigationItem(Screen.Home, Icons.AutoMirrored.Filled.List, R.string.home_title)

    data object Shopping :
        BottomNavigationItem(Screen.Shopping, Icons.Default.ShoppingCart, R.string.shopping_title)
}
```

- [ ] **Step 3: `BottomNavigationBar.kt` を書き換える**

`currentBackStackEntryAsState()` による判定を `topLevelRoute` の比較に変える。

```kotlin
package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.obaya884.rebuy.ui.navigation.Navigator

@Composable
fun BottomNavigationBar(
    navigator: Navigator,
    shoppingTabBadgeCount: Int
) {
    val items = listOf(
        BottomNavigationItem.Home,
        BottomNavigationItem.Shopping
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (item == BottomNavigationItem.Shopping && shoppingTabBadgeCount > 0) {
                                Badge {
                                    Text(
                                        text = shoppingTabBadgeCount.toString()
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = item.icon, contentDescription = null)
                    }
                },
                label = { Text(stringResource(item.titleId)) },
                selected = item.key == navigator.state.topLevelRoute,
                onClick = { navigator.navigate(item.key) }
            )
        }
    }
}
```

- [ ] **Step 4: 6 画面のシグネチャと遷移呼び出しを差し替える**

各ファイルで次の 3 つを機械的に置き換える。`import androidx.navigation.NavController` は消し、`import io.github.obaya884.rebuy.ui.navigation.Navigator` を足す。

| 変更前 | 変更後 |
|---|---|
| `navController: NavController` | `navigator: Navigator` |
| `navController.navigate(Screen.Xxx.route)` | `navigator.navigate(Screen.Xxx)` |
| `navController.navigateUp()` | `navigator.goBack()` |
| `BottomNavigationBar(navController, n)` | `BottomNavigationBar(navigator, n)` |
| `navController.navigateAsRoot(Screen.Home)` | `navigator.navigateAsRoot(Screen.Home)` |

対象は次の 9 箇所。

- `HomeScreen.kt`: シグネチャ 2 箇所（`HomeScreen` と内部の Composable）、`navigate` 4 箇所、`BottomNavigationBar` 1 箇所
- `ShoppingScreen.kt`: シグネチャ 1 箇所、`BottomNavigationBar` 1 箇所、`navigateAsRoot` 1 箇所
- `SettingScreen.kt`: シグネチャ 1 箇所、`navigateUp` 1 箇所、`navigate` 1 箇所
- `LicenseScreen.kt`: シグネチャ 1 箇所、`navigateUp` 1 箇所
- `ItemEditScreen.kt`: シグネチャ 1 箇所、`navigateUp` 1 箇所
- `CategoryEditScreen.kt`: シグネチャ 1 箇所、`navigateUp` 1 箇所

**Task 1 の Step 6 で足した 3 つの `testTag` を消さないこと。**

- [ ] **Step 5: ビルドを通す**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL。`NavController` への参照が残っていれば未解決参照で落ちるので、落ちなくなるまで直す。

- [ ] **Step 6: 特性テストが緑のままであることを確認する**

Run: `./gradlew pixel6Api35DebugAndroidTest`
Expected: Task 1 で書いた 6 件が**1 行も変えずに** PASS。

赤になったら、直すのはテストではなく実装。テストを緩める変更をした時点で段 0 の目的（挙動を変えないことの証明）が失われる。

- [ ] **Step 7: コミット**

```bash
git add app/src/main
git commit -m "NavHost を NavDisplay に置き換える（T-18）"
```

---

### Task 5: Navigation 2 の依存を外す

**Files:**
- Modify: `app/src/main/java/io/github/obaya884/rebuy/ui/ReBuyApp.kt`（`navigateAsRoot` 拡張関数の削除。Task 4 で置き換え済みなら確認のみ）
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Nav2 の直接依存を外す**

`app/build.gradle.kts` の Navigation ブロックから 1 行削る。

```kotlin
implementation(libs.androidx.navigation.compose)
```

**`libs.androidx.hilt.navigation.compose` は残す**——`hiltViewModel()` がこの artifact に入っており、全画面が使っている。この artifact が navigation-compose を推移的に引き込むため Nav2 はビルドから完全には消えないが、自分のコードからの参照は無くなる。完全な除去は段 1 で Hilt を Koin に替えるときに済む。

- [ ] **Step 2: Nav2 への参照が残っていないことを確認する**

Run: `grep -rn "androidx.navigation\." app/src/main --include="*.kt"`
Expected: 出力なし（`androidx.navigation3.` は別物なので、ドットの位置に注意して確認する）。

- [ ] **Step 3: ビルドとテスト**

Run: `./gradlew assembleDebug && ./gradlew pixel6Api35DebugAndroidTest`
Expected: BUILD SUCCESSFUL、テスト全件 PASS。

- [ ] **Step 4: コミット**

```bash
git add app/build.gradle.kts app/src/main
git commit -m "Navigation 2 の直接依存を外す（T-18）"
```

---

### Task 6: docs の追随・レビュー・動作確認・PR

**Files:**
- Modify: `CLAUDE.md`
- Modify: `docs/案件/23_技術改善バックログ.md` と `docs/案件/closed_23_技術改善バックログ.md`

- [ ] **Step 1: CLAUDE.md の UI 層の記述を直す**

「UI 層 (`ui/`)」節の次の 2 項目が古くなる。

変更前:

```
- 画面遷移は `ui/ReBuyApp.kt` の `NavHost` に集約。ルートは `sealed class Screen` で定義し、画面追加時はここに `data object` と `composable(...)` を両方足す
- 画面 Composable のシグネチャは `(navController: NavController, snackbarHostState: SnackbarHostState)` で統一。ViewModel は `hiltViewModel<XxxViewModel>()` で画面内から取得する（引数で渡さない）
```

変更後:

```
- 画面遷移は Navigation 3。`ui/ReBuyApp.kt` の `NavDisplay` と `entryProvider` に集約し、ルートは `sealed class Screen : NavKey` で定義する。画面追加時は `data object` と `entry<...>` を両方足す。backstack は `ui/navigation/NavigationState.kt` が保持し、遷移イベントは `Navigator` が受ける
- 画面 Composable のシグネチャは `(navigator: Navigator, snackbarHostState: SnackbarHostState)` で統一。ViewModel は `hiltViewModel<XxxViewModel>()` で画面内から取得する（引数で渡さない）
```

- [ ] **Step 2: レビュアーを起動する**

差分にコード・テスト・docs が含まれるので 4 本すべてが対象。バックグラウンドで並列に起動し、指摘はメインセッションで対処する。

- `verifier`（build・lint・unit test・GMD・docs-check）
- `code-quality-reviewer`
- `test-reviewer`（差分に `app/src/androidTest/**` が含まれる）
- `spec-reviewer`（差分に docs が含まれる）

- [ ] **Step 3: オーナーの動作確認**

```bash
./gradlew installDebug
```

自動テストで見ていない挙動を手で確認する。**ここが段 0 の最後の関門。**

1. ホームで品目を買い物リストに追加 → 買い物タブ → 品目をチェック → 「買い物を終わる」→ 確定。**ホームタブに戻り、買い物タブの履歴がルートに戻っていること**
2. ホーム → 設定 → 端末を回転。**設定画面のままであること**（`rememberSerializable` による状態保存）
3. ホーム → 設定 → アプリをバックグラウンドへ → プロセスが殺された後に復帰。**設定画面に戻ること**
4. 買い物タブでスクロールした位置がホームタブへ往復しても保たれること
5. ホームで戻るを押すとアプリが終了すること

- [ ] **Step 4: 台帳の T-18 を完了にする**

`--status` を省略すると状態列が「進行中」のまま移るので、必ず付ける。

```bash
sh scripts/ledger-move.sh T-18 --status "完了 $(date +%Y-%m-%d)"
sh scripts/docs-check.sh
```

- [ ] **Step 5: コミットして PR を出す**

```bash
git add CLAUDE.md docs
git commit -m "CLAUDE.md を Navigation 3 に追随させ T-18 を完了にする"
git push -u origin t-18-nav3
gh pr create --base main --head t-18-nav3 --title "③ 段 0: Navigation 3 へ移行する（T-18）"
```

CI が緑になり、オーナーの合図が出たら squash マージする。

- [ ] **Step 6: 段 1 の計画を本書に追記する**

段 0 で分かったこと（serialization プラグインの成否、Nav3 の使い勝手、特性テストの効き方）を踏まえて、spec §10 の段 1 を本書に展開する。

---

## 段 1・段 2

段 0 の完了時に本書へ追記する。範囲は [KMP 化検討](../検討/32_KMP化検討.md) §10 のとおり。
