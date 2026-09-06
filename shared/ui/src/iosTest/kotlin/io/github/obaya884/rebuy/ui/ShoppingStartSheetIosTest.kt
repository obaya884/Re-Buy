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
import kotlin.test.assertTrue

/**
 * 買い物開始シート（画面 03）の**画面段**。
 *
 * `ShoppingStartViewModelTest` が見るのは内訳の中身までで、**行の文言の組み立て
 * （プレビューの区切り・「n＋m 件」）と脚注の文言、行タップで買い物へ入ること**は
 * ここでしか見られない。**シートを出すかどうか**（FB-04）も同じで、判定は 01 が持つが
 * 「CTA を踏んだ結果どちらへ行くか」の結線はここまで来ないと見えない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ShoppingStartSheetIosTest {

    private val inBasket = ItemStatus.IN_SHOPPING_LIST

    /**
     * CTA を踏むところまで。**シートが出たことは見ない**ので、出ないほうの経路（FB-04）にも使う。
     */
    private fun startShopping(
        prepare: FakeDatabase.() -> Unit,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        block()
    }

    /**
     * 行き先 1 に 3 件・行き先 2 に 1 件・どこでも買えるものが 2 件。
     *
     * **m を 2 にしてあるのは、行数（2）とも n（3・1）とも違う数にするため**——1 だと
     * 「＋m」の m を行数や別の件数に取り違える変異が素通りする。
     */
    private fun withDestinations(): FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1, name = "アイテムA"),
                item(2, status = inBasket, destinationId = 1, name = "アイテムB"),
                item(3, status = inBasket, destinationId = 1, name = "アイテムC"),
                item(4, status = inBasket, destinationId = 2, name = "アイテムD"),
                item(5, status = inBasket, name = "どこでも品X"),
                item(6, status = inBasket, name = "どこでも品Y")
            ),
            destinations = listOf(destination(1), destination(2))
        )
    }

    /** 行き先付き 1 件だけ・どこでも買えるものは無し。「＋m」も脚注も出ない側。 */
    private fun withoutAnywhere(): FakeDatabase.() -> Unit = {
        seed(
            items = listOf(item(1, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
    }

    @Test
    fun 行に行き先とプレビューと件数が出る() = startShopping(withDestinations()) {
        onNodeWithText("今日はどこへ？").assertIsDisplayed()
        onNodeWithText("行き先を選ぶと、買い物が始まります").assertIsDisplayed()

        val row = onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1))
        row.assertTextContains("行き先1")
        // プレビューは先頭 2 件を中黒でつなぐ。「など」は付けない
        row.assertTextContains("アイテムA・アイテムB")
        // 3 件＋どこでも 2 件
        row.assertTextContains("3＋2 件")
    }

    /** **m はどの行にも同じだけ足す**（画面 03）。行ごとに違う数を足す実装だとここが落ちる。 */
    @Test
    fun どこでも買えるものは行にならず全部の行の件数に足される() =
        startShopping(withDestinations()) {
            onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertTextContains("3＋2 件")
            onNodeWithTag(TestTags.shoppingStartRow(destinationId = 2)).assertTextContains("1＋2 件")
            // 行き先は 2 件しか無いので、独立した行が増えていれば 3 件目の行が出る
            onNodeWithTag(TestTags.shoppingStartRow(destinationId = 3)).assertDoesNotExist()
        }

    /** どこでも買えるものが無ければ「n 件」だけ。 */
    @Test
    fun どこでも買えるものが無ければ件数はnだけ() = startShopping(withoutAnywhere()) {
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertTextContains("1 件")
    }

    /**
     * 「＋m」が何なのかは脚注で言う（画面 03・FB-09）。
     *
     * **位置まで見る**——存在だけ見ると、内訳の上へ動かしても緑のままになる（17 §3）。
     */
    @Test
    fun どこでも買えるものがあれば内訳の下に脚注が出る() = startShopping(withDestinations()) {
        val note = onNodeWithText("＋2 件はどこでも買えるもの。どの行き先にも一緒に入ります")
        note.assertIsDisplayed()

        val lastRow = topOf(TestTags.shoppingStartRow(destinationId = 2))
        assertTrue(note.fetchSemanticsNode().positionInRoot.y > lastRow, "脚注は最後の行より下")
    }

    /** m が 0 なら脚注も出さない——件数側にも「＋m」が出ていない。 */
    @Test
    fun どこでも買えるものが無ければ脚注も出ない() = startShopping(withoutAnywhere()) {
        onNodeWithText("どの行き先にも一緒に入ります", substring = true).assertDoesNotExist()
    }

    /** **行き先付きが 1 件も無ければシートを出さず 04 へ直行する**（画面 01・FB-04）。 */
    @Test
    fun 行き先付きが無ければシートを出さず買い物へ直行する() = startShopping({
        seed(items = listOf(item(1, status = inBasket), item(2, status = inBasket)))
    }) {
        onNodeWithText("今日はどこへ？").assertDoesNotExist()
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextContains("買い物中")
    }

    /** **行き先付きが 1 件でもあればシートは出す。** 選択肢 1 つでも「どこへ？」の答えになる。 */
    @Test
    fun 行き先付きが1件あればシートを出す() = startShopping(withoutAnywhere()) {
        onNodeWithText("今日はどこへ？").assertIsDisplayed()
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertExists()
    }

    /** 行タップで買い物へ入り、シートは閉じる（画面 03）。 */
    @Test
    fun 行タップで買い物に入りシートは閉じる() = startShopping(withDestinations()) {
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextContains("行き先1で買い物中")
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertDoesNotExist()
    }

    /** 画面上の縦位置。並びの assert に使う（`ShoppingIosTest` と同じ手）。 */
    private fun ComposeUiTest.topOf(tag: String): Float =
        onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
}
