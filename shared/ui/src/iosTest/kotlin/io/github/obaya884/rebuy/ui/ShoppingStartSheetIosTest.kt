package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.test.Test

/**
 * 買い物開始シート（画面 03）の**画面段**。
 *
 * `ShoppingStartViewModelTest` が見るのは内訳の中身までで、**行の文言の組み立て
 * （プレビューの区切り・「n＋m 件」）と、行タップで買い物へ入ること**はここでしか見られない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ShoppingStartSheetIosTest {

    private val inBasket = ItemStatus.IN_SHOPPING_LIST

    private fun sheet(
        prepare: FakeDatabase.() -> Unit,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        block()
    }

    /** 行き先付きが 2 件・どこでも買えるものが 1 件。件数は「n＋m 件」になる。 */
    private fun withDestinations(): FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1, name = "アイテムA"),
                item(2, status = inBasket, destinationId = 1, name = "アイテムB"),
                item(3, status = inBasket, destinationId = 1, name = "アイテムC"),
                item(4, status = inBasket, name = "どこでも品")
            ),
            destinations = listOf(destination(1))
        )
    }

    @Test
    fun 行に行き先とプレビューと件数が出る() = sheet(withDestinations()) {
        onNodeWithText("今日はどこへ？").assertIsDisplayed()
        onNodeWithText("行き先を選ぶと、買い物が始まります").assertIsDisplayed()

        val row = onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1))
        row.assertTextContains("行き先1")
        // プレビューは先頭 2 件を中黒でつなぐ。「など」は付けない
        row.assertTextContains("アイテムA・アイテムB")
        // 3 件＋どこでも 1 件
        row.assertTextContains("3＋1 件")
    }

    /** どこでも買えるものが無ければ「n 件」だけ。 */
    @Test
    fun どこでも買えるものが無ければ件数はnだけ() = sheet({
        seed(
            items = listOf(item(1, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
    }) {
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertTextContains("1 件")
    }

    /**
     * どこでも買えるものは**独立した行にしない**（画面 03）。件数の「＋m」でのみ表現する。
     *
     * 品目名で見ないのは、**シートの背後にプールの一覧が残っている**ため（実測）。
     * 行き先の行が 1 つだけで、全件モードの行が無いことで見る。
     */
    @Test
    fun どこでも買えるものの行は出ない() = sheet(withDestinations()) {
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertExists()
        onNodeWithTag(TestTags.SHOPPING_START_ALL_ROW).assertDoesNotExist()
        // 行き先は 1 件しか無いので、2 件目の行も出ない
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 2)).assertDoesNotExist()
    }

    /** 行き先付きが 1 件も無いときは「n 件で開始」の 1 行だけ。 */
    @Test
    fun 行き先付きが無ければ全件モードの1行() = sheet({
        seed(items = listOf(item(1, status = inBasket), item(2, status = inBasket)))
    }) {
        onNodeWithTag(TestTags.SHOPPING_START_ALL_ROW).assertTextContains("2 件で開始")
    }

    /** 行タップで買い物へ入り、シートは閉じる（画面 03）。 */
    @Test
    fun 行タップで買い物に入りシートは閉じる() = sheet(withDestinations()) {
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextContains("行き先1で買い物中")
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertDoesNotExist()
    }
}
