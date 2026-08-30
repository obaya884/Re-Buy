package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.category_edit_title
import io.github.obaya884.rebuy.ui.resources.home_no_item_button
import io.github.obaya884.rebuy.ui.resources.home_no_item_message_all
import io.github.obaya884.rebuy.ui.resources.home_title
import io.github.obaya884.rebuy.ui.resources.item_edit_title
import io.github.obaya884.rebuy.ui.resources.setting_title
import io.github.obaya884.rebuy.ui.resources.shopping_title
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/**
 * iOS 側の画面遷移の特性テスト。**Android の `NavigationTest` に対応する iOS の網**。
 *
 * **`commonTest` には置けない**（素の JVM で実行されて落ちる）。理由は
 * [テスト戦略定義書](../../../../../../../../docs/仕様/17_テスト戦略定義書.md) §1。
 *
 * ### Android 版との差
 *
 * **端末の戻るを踏む 6 件は持たない。** iOS にハードウェアの戻るが無いため。
 * 代わりに戻る矢印で 2 段降りる 1 件を持つ（Android 版の「1 段ずつ帰る」に対応）。
 *
 * ### 前提と穴
 *
 * **DB が空であることを前提にしている。** 空状態を assert する 2 件がその前提を守る番人で、
 * ここが落ちたら DB にデータが残っている。書き込むテストを足すときは [ensureKoinStarted] を読むこと。
 *
 * **実物の `.app` は起動しない**ので Swift の起動経路と同梱は見ない（T-46）。
 * **ライセンス画面はタイトルまでしか見ていない**——一覧の中身は非同期に読むので、
 * そこは Android の `LicenseLibrariesTest` にあたるものが iOS に要る（T-39）。
 */
@OptIn(ExperimentalTestApi::class)
class NavigationIosTest {

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private val homeTitle = string(Res.string.home_title)
    private val shoppingTitle = string(Res.string.shopping_title)
    private val settingTitle = string(Res.string.setting_title)
    private val itemEditTitle = string(Res.string.item_edit_title)
    private val categoryEditTitle = string(Res.string.category_edit_title)
    private val noItemMessage = string(Res.string.home_no_item_message_all)
    private val noItemButton = string(Res.string.home_no_item_button)

    /** ライセンス画面のタイトルと設定画面の行は実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"

    /** Koin を起動して [ReBuyApp] を描き、[block] を実行する。起動の作法は [ensureKoinStarted]。 */
    private fun app(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        ensureKoinStarted()
        setContent { ReBuyApp() }
        block()
    }

    /**
     * 現在表示されている画面を TopAppBar のタイトルで判定する。
     *
     * **5 つのタイトルが互いに異なることに依存している。** 同じ語になる画面が出たら、
     * 遷移先を取り違えても全件緑になるので、そのときは画面ごとの `testTag` に切り替える。
     */
    private fun ComposeUiTest.assertCurrentScreenIs(title: String) {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    private fun ComposeUiTest.tapBackArrow() {
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
    }

    private fun ComposeUiTest.tapTab(item: BottomNavigationItem) {
        onNodeWithTag(TestTags.bottomNavItem(item)).performClick()
    }

    @Test
    fun 起動直後はホームが表示される() = app {
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun 品目が無いホームは空状態の文言とボタンを出す() = app {
        onNodeWithText(noItemMessage).assertExists()
        onNodeWithText(noItemButton).assertExists()
    }

    @Test
    fun 空状態のボタンからアイテム一覧へ遷移する() = app {
        onNodeWithText(noItemButton).performClick()
        assertCurrentScreenIs(itemEditTitle)
    }

    @Test
    fun ホームから設定へ遷移して戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して戻る矢印で1段ずつホームまで帰る() = app {
        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun アイテム一覧の戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_ITEM_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(itemEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun カテゴリー一覧の戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_CATEGORY_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(categoryEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ボトムナビでホームと買い物を往復できる() = app {
        tapTab(BottomNavigationItem.Shopping)
        assertCurrentScreenIs(shoppingTitle)

        tapTab(BottomNavigationItem.Home)
        assertCurrentScreenIs(homeTitle)
    }
}
