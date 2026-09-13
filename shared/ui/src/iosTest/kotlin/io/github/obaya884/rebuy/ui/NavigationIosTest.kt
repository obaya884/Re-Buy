package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.setting_row_category_edit
import io.github.obaya884.rebuy.ui.resources.pool_empty_message
import io.github.obaya884.rebuy.ui.resources.pool_empty_title
import kotlin.test.Test

/**
 * iOS 側の画面遷移の特性テスト。**Android の `NavigationTest` に対応する iOS の網**。
 *
 * **`commonTest` には置けない**（素の JVM で実行されて落ちる）。理由と、この網が見ない範囲は
 * [テスト戦略定義書](../../../../../../../../docs/仕様/17_テスト戦略定義書.md) §1 と §6。
 *
 * **端末の戻るを踏む Android 版の 6 件は持たない。** 代わりに戻る矢印で 2 段降りる 1 件を持つ。
 * **iOS にも端末の戻り（端スワイプ）はある**——当初は無いと思って書いた記述だが、それは誤りだった
 * （FB-18）。この画面群でも踏めるはずで、そこは [T-63](../../../../../../../../docs/案件/23_技術改善バックログ.md#t-63) が持つ。
 *
 * DB は [startTestKoin] が [FakeDatabase] に差し替える。**品目を置くテストを先に宣言している**
 * のは、後続の空状態テストが「毎回空へ戻る」ことの観測者になるため。
 */
@OptIn(ExperimentalTestApi::class)
class NavigationIosTest {

    // 画面のタイトル・文字列の読み出し・1 件だけの種は [IosTestFixtures] が持つ（AppBarStateIosTest と共有）
    private val poolTitle = ScreenTitle.pool
    private val shoppingTitle = ScreenTitle.shoppingAll
    private val settingTitle = ScreenTitle.setting
    private val categoryManageTitle = ScreenTitle.categoryManage

    private val emptyTitle = string(Res.string.pool_empty_title)
    private val emptyMessage = string(Res.string.pool_empty_message)
    private val licenseLabel = ScreenTitle.license
    private val categoryEditLabel = string(Res.string.setting_row_category_edit)
    private val themeLabel = ScreenTitle.theme

    /**
     * **本番 iOS の構成**で [ReBuyApp] を描いて [block] を実行する（[runIosApp]）。
     *
     * 外枠は SwiftUI が描くので ⚙ や ← のノードは無く、画面の同定と遷移は
     * 渡ってきた内容（[IosAppProbe]）から起こす。
     */
    private fun app(
        prepare: FakeDatabase.() -> Unit = {},
        block: ComposeUiTest.(IosAppProbe) -> Unit
    ) = runIosApp(prepare, block = block)

    @Test
    fun 起動直後はプールが表示される() = app { probe ->
        probe.assertScreen(poolTitle)
    }

    /** 品目があるときは空状態ではなく行が出る。 */
    @Test
    fun 品目があるプールは空状態ではなく行を出す() = app(oneItem(ItemStatus.NO_DEAL)) {
        onNodeWithText("アイテム1").assertExists()
        onNodeWithText(emptyTitle).assertDoesNotExist()
    }

    @Test
    fun 品目が無いプールは空状態の文言を出す() = app {
        onNodeWithText(emptyTitle).assertExists()
        onNodeWithText(emptyMessage).assertExists()
    }

    @Test
    fun 設定からテーマへ遷移して戻る矢印で設定に帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithText(themeLabel).performClick()
        probe.assertScreen(themeLabel)

        probe.back()
        probe.assertScreen(settingTitle)
    }

    @Test
    fun プールから設定へ遷移して戻る矢印でプールに帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        probe.assertScreen(settingTitle)

        probe.back()
        probe.assertScreen(poolTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して戻る矢印で1段ずつプールまで帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithText(licenseLabel).performClick()
        probe.assertScreen(licenseLabel)

        probe.back()
        probe.assertScreen(settingTitle)

        probe.back()
        probe.assertScreen(poolTitle)
    }

    @Test
    fun カテゴリの管理の戻る矢印で設定に帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithText(categoryEditLabel).performClick()
        probe.assertScreen(categoryManageTitle)

        probe.back()
        probe.assertScreen(settingTitle)
    }

    /**
     * プールの CTA から買い物へ入り、← の離脱確認でプールへ戻る。
     *
     * **CTA はカゴが空だと押せない**ので、カゴに 1 件置いてから踏む（画面 01）。
     * 置いた 1 件は行き先なしなので**全件モードになり、03 を挟まず入る**（FB-04）。
     */
    @Test
    fun CTAから買い物へ入り離脱確認でプールに帰る() = app(oneItem(ItemStatus.IN_SHOPPING_LIST)) { probe ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        probe.assertScreen(shoppingTitle)

        probe.back()
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).performClick()

        probe.assertScreen(poolTitle)
    }

    /**
     * **行タップがカゴの出し入れに繋がっていること**（画面 01・§2）。
     *
     * `PoolViewModelTest` は ViewModel までしか見ないので、**行に `onClick` を付け忘れても
     * 全件緑になる**。カゴ件数が CTA の有効・無効に出るのを使って、UI 段で押さえる。
     */
    @Test
    fun 行タップでカゴに入りCTAが押せるようになる() = app(oneItem(ItemStatus.NO_DEAL)) {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsNotEnabled()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).performClick()

        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsEnabled()
    }

    /** もう一度タップすると出る。 */
    @Test
    fun もう一度タップするとカゴから出る() = app(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsEnabled()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).performClick()

        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsNotEnabled()
    }

    /** カゴが空のときは CTA が押せない（画面 01）。 */
    @Test
    fun カゴが空なら買い物を始められない() = app(oneItem(ItemStatus.NO_DEAL)) { probe ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsNotEnabled()

        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        probe.assertScreen(poolTitle)
    }
}
