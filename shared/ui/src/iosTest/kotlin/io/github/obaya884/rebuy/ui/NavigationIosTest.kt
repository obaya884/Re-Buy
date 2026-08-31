package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.category_edit_title
import io.github.obaya884.rebuy.ui.resources.home_no_item_button
import io.github.obaya884.rebuy.ui.resources.home_no_item_message_all
import io.github.obaya884.rebuy.ui.resources.home_remove_item_button
import io.github.obaya884.rebuy.ui.resources.home_remove_item_button_from_shopping_list
import io.github.obaya884.rebuy.ui.resources.home_title
import io.github.obaya884.rebuy.ui.resources.item_edit_title
import io.github.obaya884.rebuy.ui.resources.setting_title
import io.github.obaya884.rebuy.ui.resources.shopping_title
import io.github.obaya884.rebuy.ui.resources.theme_title
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
import io.github.obaya884.rebuy.ui.screen.home.HomeTab
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/**
 * iOS 側の画面遷移の特性テスト。**Android の `NavigationTest` に対応する iOS の網**。
 *
 * **`commonTest` には置けない**（素の JVM で実行されて落ちる）。理由と、この網が見ない範囲は
 * [テスト戦略定義書](../../../../../../../../docs/仕様/17_テスト戦略定義書.md) §1 と §6。
 *
 * **端末の戻るを踏む Android 版の 6 件は持たない**（iOS にハードウェアの戻るが無いため）。
 * 代わりに戻る矢印で 2 段降りる 1 件を持つ。
 *
 * DB は [startTestKoin] が [FakeDatabase] に差し替える。**品目を置くテストを先に宣言している**
 * のは、後続の空状態テストが「毎回空へ戻る」ことの観測者になるため。
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
    private val removeLabel = string(Res.string.home_remove_item_button)
    private val removeFromListLabel = string(Res.string.home_remove_item_button_from_shopping_list)

    /** ライセンス画面のタイトルと設定画面の行は実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"
    private val themeLabel = string(Res.string.theme_title)

    /** 品目を 1 件だけ置く。ステータスを変えると通る分岐が変わるので、各テストが明示する。 */
    private fun oneItem(status: ItemStatus): FakeDatabase.() -> Unit =
        { seed(items = listOf(item(id = 1, name = "アイテム1", status = status))) }

    /** [ReBuyApp] を描いて [block] を実行する。Koin と DB の用意は [startTestKoin]。 */
    private fun app(
        prepare: FakeDatabase.() -> Unit = {},
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
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

    /**
     * 品目があるときは空状態ではなく行が出る。
     *
     * `IN_SHOPPING_LIST` なのは、`NO_DEAL` だと行のボタンが「リストに追加」側へ分岐して
     * タブによる出し分けに到達しないため。**その選択を [removeLabel] の assert で固定している。**
     */
    @Test
    fun 品目があるホームは空状態ではなく行を出す() = app(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        onNodeWithText("アイテム1").assertExists()
        onNodeWithText(removeLabel).assertExists()
        onNodeWithText(noItemMessage).assertDoesNotExist()
        onNodeWithText(noItemButton).assertDoesNotExist()
    }

    /**
     * カゴタブでは行のボタンの文言が変わる。
     *
     * **`HomeListItemRow` のタブ分岐を通す唯一の経路。** ここを assert しないと、
     * 分岐が壊れても行そのものは出るので気づけない。
     */
    @Test
    fun カゴタブの行はリストから削除を出す() = app(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        onNodeWithText(HomeTab.InBasket.title).performClick()
        onNodeWithText(removeFromListLabel).assertExists()
        onNodeWithText(removeLabel).assertDoesNotExist()
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
    fun 設定からテーマへ遷移して戻る矢印で設定に帰る() = app {
        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        onNodeWithText(themeLabel).performClick()
        assertCurrentScreenIs(themeLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
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
