package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 買い物モード（画面 04）の**画面段**。
 *
 * `ShoppingViewModelTest` が見るのは一覧の中身と書き込みまでで、**アプリバーの組み立て
 * （タイトル・進捗）と、行タップ・終了・離脱が繋がっていること**はここでしか見られない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ShoppingIosTest {

    private val inBasket = ItemStatus.IN_SHOPPING_LIST
    private val checked = ItemStatus.CHECKED_IN_SHOPPING_LIST

    /** 03 の行き先の行を踏んで 04 へ入る。 */
    private fun shopping(
        prepare: FakeDatabase.() -> Unit,
        destinationId: Int? = 1,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        val row = destinationId
            ?.let { TestTags.shoppingStartRow(it) }
            ?: TestTags.SHOPPING_START_ALL_ROW
        onNodeWithTag(row).performClick()
        block()
    }

    /** 画面上の縦位置。並びの assert に使う。 */
    private fun ComposeUiTest.topOf(tag: String): Float =
        onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /** 行き先 1 に 2 件、どこでも買えるものが 1 件。 */
    private val withAnywhere: FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1, name = "アイテムA"),
                item(2, status = inBasket, destinationId = 1, name = "アイテムB"),
                item(3, status = inBasket, name = "どこでも品")
            ),
            destinations = listOf(destination(1))
        )
    }

    @Test
    fun アプリバーに行き先名と進捗が出る() = shopping(withAnywhere) {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("行き先1で買い物中")
        // どこでも買えるものも一覧の一部なので分母に入る
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("0 / 3")
    }

    /**
     * 並びは「行き先の品目 → 区切り → どこでも買えるもの」（データモデル定義書 §4）。
     * **存在だけ見ると 2 群を入れ替えても緑になる**ので、画面上の y で見る。
     */
    @Test
    fun どこでも買えるものは区切りの下に並ぶ() = shopping(withAnywhere) {
        val ofDestination = topOf(TestTags.shoppingRow(itemId = 2))
        val section = topOf(TestTags.SHOPPING_ANYWHERE_SECTION)
        val anywhere = topOf(TestTags.shoppingRow(itemId = 3))

        assertTrue(ofDestination < section, "行き先の品目は区切りより上")
        assertTrue(section < anywhere, "区切りはどこでも買えるものより上")
    }

    /** 行き先モードでも、どこでも買えるものが無ければ区切りは出ない。 */
    @Test
    fun どこでも買えるものが無ければ区切りも出ない() = shopping({
        seed(
            items = listOf(item(1, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
    }) {
        onNodeWithTag(TestTags.SHOPPING_ANYWHERE_SECTION).assertDoesNotExist()
    }

    /**
     * **タップした行き先が 04 へ渡っていること**（03 → `Screen.Shopping(destinationId)` →
     * Koin → ViewModel）。行き先が 1 件しか無いと、どの行を押しても同じ結果になって
     * この配線が無防備になる。
     */
    @Test
    fun 選んだ行き先の一覧が出る() = shopping(
        prepare = {
            seed(
                items = listOf(
                    item(1, status = inBasket, destinationId = 1),
                    item(2, status = inBasket, destinationId = 2),
                    item(3, status = inBasket, name = "どこでも品")
                ),
                destinations = listOf(destination(1), destination(2))
            )
        },
        destinationId = 2
    ) {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("行き先2で買い物中")
        onNodeWithTag(TestTags.shoppingRow(itemId = 2)).assertExists()
        onNodeWithTag(TestTags.shoppingRow(itemId = 1)).assertDoesNotExist()
        // 行き先 2 の 1 件 ＋ どこでも 1 件
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("0 / 2")
    }

    /** 行の長押しは無効（画面 04）。01 と違って編集シートは開かない。 */
    @Test
    fun 行の長押しでは何も開かない() = shopping(withAnywhere) {
        onNodeWithTag(TestTags.shoppingRow(itemId = 1)).performTouchInput { longClick() }

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("行き先1で買い物中")
    }

    /** 全件モードには「どこでも買えるもの」の区切りが無い（群が 1 つしかない）。 */
    @Test
    fun 全件モードは区切りなしで買い物中と出る() = shopping(
        prepare = { seed(items = listOf(item(1, status = inBasket))) },
        destinationId = null
    ) {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("買い物中")
        onNodeWithTag(TestTags.SHOPPING_ANYWHERE_SECTION).assertDoesNotExist()
    }

    /** 行タップがチェックに繋がっていること。進捗の分子で見る。 */
    @Test
    fun 行タップでチェックが付き進捗が進む() = shopping(withAnywhere) {
        onNodeWithTag(TestTags.shoppingRow(itemId = 1)).performClick()
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("1 / 3")

        // もう一度タップすると外れる
        onNodeWithTag(TestTags.shoppingRow(itemId = 1)).performClick()
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("0 / 3")
    }

    /** 終了でプールへ戻り、チェック済みだけがカゴから抜ける（画面 04）。 */
    @Test
    fun 終了でプールへ戻りチェック済みだけカゴから抜ける() = shopping({
        seed(
            items = listOf(
                item(1, status = checked, destinationId = 1),
                item(2, status = inBasket, destinationId = 1)
            ),
            destinations = listOf(destination(1))
        )
    }) {
        onNodeWithTag(TestTags.SHOPPING_FINISH_BUTTON).performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextContains("Re-Buy")
        // 未チェックの 1 件がカゴに残るので、CTA のバッジは 1
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertTextContains("1")
    }

    /** チェックが 1 件も無くても終了できる（画面 04。何も買わずに帰る道）。 */
    @Test
    fun チェックが無くても終了できる() = shopping(withAnywhere) {
        onNodeWithTag(TestTags.SHOPPING_FINISH_BUTTON).assertIsEnabled()

        onNodeWithTag(TestTags.SHOPPING_FINISH_BUTTON).performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextContains("Re-Buy")
        // 何も戻していないので、カゴの 3 件はそのまま
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertTextContains("3")
    }

    /** ← の離脱確認で「続ける」を選ぶと 04 に留まる（画面 04）。 */
    @Test
    fun 離脱確認で続けると買い物に留まる() = shopping(withAnywhere) {
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithText("買い物を途中でやめますか？").assertExists()

        onNodeWithTag(TestTags.SHOPPING_LEAVE_CANCEL).performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("行き先1で買い物中")
    }

    /** 「やめる」で 01 へ戻る。**チェックは残る**ので、03 から入り直せば続きから。 */
    @Test
    fun 離脱してもチェックは残る() = shopping({
        seed(
            items = listOf(item(1, status = checked, destinationId = 1)),
            destinations = listOf(destination(1))
        )
    }) {
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("1 / 1")

        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).performClick()

        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("1 / 1")
    }
}
