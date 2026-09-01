package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.category_edit_title
import io.github.obaya884.rebuy.ui.resources.setting_row_category_edit
import io.github.obaya884.rebuy.ui.resources.setting_title
import io.github.obaya884.rebuy.ui.resources.pool_empty_message
import io.github.obaya884.rebuy.ui.resources.pool_title
import io.github.obaya884.rebuy.ui.resources.pool_empty_title
import io.github.obaya884.rebuy.ui.resources.shopping_title_all
import io.github.obaya884.rebuy.ui.resources.theme_title
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

    private val poolTitle = string(Res.string.pool_title)
    private val shoppingTitle = string(Res.string.shopping_title_all)
    private val settingTitle = string(Res.string.setting_title)
    private val categoryEditTitle = string(Res.string.category_edit_title)
    private val emptyTitle = string(Res.string.pool_empty_title)
    private val emptyMessage = string(Res.string.pool_empty_message)

    /** ライセンス画面のタイトルは実装側がハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"
    private val categoryEditLabel = string(Res.string.setting_row_category_edit)
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

    @Test
    fun 起動直後はプールが表示される() = app {
        assertCurrentScreenIs(poolTitle)
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
    fun 設定からテーマへ遷移して戻る矢印で設定に帰る() = app {
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithText(themeLabel).performClick()
        assertCurrentScreenIs(themeLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun プールから設定へ遷移して戻る矢印でプールに帰る() = app {
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して戻る矢印で1段ずつプールまで帰る() = app {
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun カテゴリー一覧の戻る矢印で設定に帰る() = app {
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithText(categoryEditLabel).performClick()
        assertCurrentScreenIs(categoryEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    /**
     * プールの CTA から開始シート（03）を経て買い物へ入り、← の離脱確認でプールへ戻る。
     *
     * **CTA はカゴが空だと押せない**ので、カゴに 1 件置いてから踏む（画面 01）。
     */
    @Test
    fun CTAから買い物へ入り離脱確認でプールに帰る() = app(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        // 行き先付きが無いので全件モードの 1 行
        onNodeWithTag(TestTags.SHOPPING_START_ALL_ROW).performClick()
        assertCurrentScreenIs(shoppingTitle)

        tapBackArrow()
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).performClick()

        assertCurrentScreenIs(poolTitle)
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
    fun カゴが空なら買い物を始められない() = app(oneItem(ItemStatus.NO_DEAL)) {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertIsNotEnabled()

        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        assertCurrentScreenIs(poolTitle)
    }
}
