package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 画面遷移の特性テスト。**遷移の仕様そのものを変えない限り書き換えない**——
 * ナビゲーション基盤を差し替えたときに挙動が変わっていないことを、ここで機械的に確かめる。
 *
 * 遷移規則そのもの（スタックの積み方・タブごとの履歴保持）は JVM 段の `NavigatorTest` が持つ。
 * ここが見るのは「UI の操作がその規則に正しく結線されているか」。
 *
 * DB の中身に依存する遷移（買い物終了でホームへ戻る）は、データを用意する必要があるため扱わない。
 *
 * **iOS 側の対は `shared/ui/src/iosTest` の `NavigationIosTest`。** 共通化する手立てが無い
 * （モジュールも source set も別）ので、**遷移を足したら両方に足すこと**。
 * 向こうは端末の戻るを踏む 6 件を持たない代わりに、空状態を見る 2 件を持つ。
 */
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private val homeTitle = string(Res.string.home_title)
    private val shoppingTitle = string(Res.string.shopping_title)
    private val settingTitle = string(Res.string.setting_title)
    private val itemEditTitle = string(Res.string.item_edit_title)
    private val categoryEditTitle = string(Res.string.category_edit_title)

    /** ライセンス画面のタイトルと設定画面の行は実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"

    /** 現在表示されている画面を TopAppBar のタイトルで判定する。 */
    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    private fun pressBack() {
        Espresso.pressBack()
        composeRule.waitForIdle()
    }

    private fun tapBackArrow() {
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
    }

    private fun tapTab(item: BottomNavigationItem) {
        composeRule.onNodeWithTag(TestTags.bottomNavItem(item)).performClick()
    }

    @Test
    fun 起動直後はホームが表示される() {
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームから設定へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        pressBack()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームから設定へ遷移して戻る矢印でホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して端末の戻るで1段ずつホームまで帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        pressBack()
        assertCurrentScreenIs(settingTitle)

        pressBack()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ライセンスの戻る矢印で設定に帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun ホームからアイテム一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_ITEM_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(itemEditTitle)

        pressBack()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun アイテム一覧の戻る矢印でホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_ITEM_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(itemEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームからカテゴリー一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_CATEGORY_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(categoryEditTitle)

        pressBack()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun カテゴリー一覧の戻る矢印でホームに帰る() {
        composeRule.onNodeWithTag(TestTags.HOME_CATEGORY_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(categoryEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ボトムナビでホームと買い物を往復できる() {
        tapTab(BottomNavigationItem.Shopping)
        assertCurrentScreenIs(shoppingTitle)

        tapTab(BottomNavigationItem.Home)
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun 買い物タブから端末の戻るでホームに帰る() {
        tapTab(BottomNavigationItem.Shopping)
        assertCurrentScreenIs(shoppingTitle)

        pressBack()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームで戻るとアプリが終了する() {
        assertCurrentScreenIs(homeTitle)

        // Activity が破棄されると composeRule.activity は取得自体が落ちるので、先に掴んでおく
        val activity = composeRule.activity

        // pressBack だとアプリ終了時に NoActivityResumedException になるので無条件版を使う
        Espresso.pressBackUnconditionally()
        // composeRule.waitForIdle() は composition が消えた後だと当てにならないので Espresso 側で待つ
        Espresso.onIdle()

        assertTrue(activity.isFinishing || activity.isDestroyed)
    }
}
