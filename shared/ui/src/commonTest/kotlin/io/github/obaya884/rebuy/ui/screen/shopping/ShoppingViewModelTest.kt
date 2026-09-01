package io.github.obaya884.rebuy.ui.screen.shopping

import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.ui.CREATED_AT
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 買い物モード（画面 04）の一覧と、チェック・終了の書き込み。
 *
 * 一覧が固定するのは 3 つ。**選んだ行き先の品目とどこでも買えるものが別の群になること**、
 * **他の行き先の品目とカゴ外は出ないこと**、**全件モードは 1 群にまとまること**。
 * 終了は **「一覧のチェック済みだけ」が戻る**ところを見る（データモデル定義書 §3）。
 */
class ShoppingViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel(destinationId: Int?) = ShoppingViewModel(
        itemRepository = ItemRepository(db.itemDao),
        destinationRepository = DestinationRepository(db.destinationDao),
        destinationId = destinationId
    )

    private val inBasket = ItemStatus.IN_SHOPPING_LIST
    private val checked = ItemStatus.CHECKED_IN_SHOPPING_LIST

    // ---- 一覧 ----

    @Test
    fun 選んだ行き先の品目とどこでも買えるものが別の群になる() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, status = inBasket),
                item(3, status = inBasket, destinationId = 1)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(1, 3), viewModel.uiState.value.destinationItems.map { it.id })
        assertEquals(listOf(2), viewModel.uiState.value.anywhereItems.map { it.id })
    }

    @Test
    fun 他の行き先の品目は一覧に出ない() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, status = inBasket, destinationId = 2)
            ),
            destinations = listOf(destination(1), destination(2))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.visibleItems.map { it.id })
    }

    /** カゴに入っていないもの（状態 0）は買い物の対象ではない。 */
    @Test
    fun カゴに入っていない品目は一覧に出ない() = runTest {
        db.seed(
            items = listOf(item(1, destinationId = 1), item(2, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.destinationItems.map { it.id })
    }

    /** チェック済み（状態 2）も一覧に残る。**行の位置は動かさない**ので順も変わらない。 */
    @Test
    fun チェック済みも一覧に残り順も変わらない() = runTest {
        db.seed(
            items = listOf(
                item(1, status = checked, destinationId = 1),
                item(2, status = inBasket, destinationId = 1)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.uiState.value.destinationItems.map { it.id })
    }

    /** 全件モードは行き先で分けない。どこでも買えるものも同じ群に入る。 */
    @Test
    fun 全件モードはカゴの中身が1群にまとまる() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, status = inBasket)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = null)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAllMode)
        assertEquals(listOf(1, 2), viewModel.uiState.value.destinationItems.map { it.id })
        assertEquals(emptyList(), viewModel.uiState.value.anywhereItems)
    }

    @Test
    fun 行き先名はアプリバーのために引かれる() = runTest {
        db.seed(destinations = listOf(destination(1, name = "行き先A"), destination(2)))
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals("行き先A", viewModel.uiState.value.destinationName)
    }

    @Test
    fun 全件モードには行き先名が無い() = runTest {
        db.seed(destinations = listOf(destination(1)))
        val viewModel = viewModel(destinationId = null)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.destinationName)
    }

    /** 進捗「x / n」。n は一覧の総数なので、どこでも買えるものも数に入る。 */
    @Test
    fun 進捗はチェック済みと一覧総数を数える() = runTest {
        db.seed(
            items = listOf(
                item(1, status = checked, destinationId = 1),
                item(2, status = inBasket, destinationId = 1),
                item(3, status = checked),
                item(4, status = inBasket, destinationId = 2)
            ),
            destinations = listOf(destination(1), destination(2))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.checkedCount)
        assertEquals(3, viewModel.uiState.value.totalCount)
    }

    // ---- チェックの付け外し ----

    @Test
    fun チェックを付けるとチェック済みになる() = runTest {
        db.seed(items = listOf(item(1, status = inBasket)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.toggleCheck(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(checked, db.storedItem(1).status)
    }

    @Test
    fun もう一度タップするとチェックが外れる() = runTest {
        db.seed(items = listOf(item(1, status = checked)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.toggleCheck(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(inBasket, db.storedItem(1).status)
    }

    /** チェックは買った印ではない。最終購入日が入るのは「終了」のときだけ。 */
    @Test
    fun チェックを付けても最終購入日は変わらない() = runTest {
        db.seed(items = listOf(item(1, status = inBasket)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.toggleCheck(db.storedItem(1))
        advanceUntilIdle()

        assertNull(db.storedItem(1).lastBoughtAt)
    }

    // ---- 買い物の終了 ----

    @Test
    fun 終了でチェック済みがプールへ戻り最終購入日が入る() = runTest {
        db.seed(items = listOf(item(1, status = checked)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.finishShopping {}
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
        assertNotNull(db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun 前回の最終購入日は上書きされる() = runTest {
        db.seed(items = listOf(item(1, status = checked, lastBoughtAt = CREATED_AT)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.finishShopping {}
        advanceUntilIdle()

        assertNotEquals(CREATED_AT, db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun 終了してもチェックしていない品目はカゴに残る() = runTest {
        db.seed(items = listOf(item(1, status = inBasket), item(2, status = checked)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.finishShopping {}
        advanceUntilIdle()

        assertEquals(inBasket, db.storedItem(1).status)
        assertNull(db.storedItem(1).lastBoughtAt)
    }

    /**
     * **一覧に無いチェック済みは戻さない**（データモデル定義書 §3）。
     * 05 で他の行き先へ足したものや、前回別の店で付けたチェックが巻き添えにならないように。
     */
    @Test
    fun 一覧に出ていないチェック済みは戻らない() = runTest {
        db.seed(
            items = listOf(
                item(1, status = checked, destinationId = 1),
                item(2, status = checked, destinationId = 2)
            ),
            destinations = listOf(destination(1), destination(2))
        )
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.finishShopping {}
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
        assertEquals(checked, db.storedItem(2).status)
    }

    @Test
    fun どこでも買えるもののチェック済みも戻る() = runTest {
        db.seed(
            items = listOf(item(1, status = checked, destinationId = 1), item(2, status = checked)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.finishShopping {}
        advanceUntilIdle()

        assertEquals(emptyList<Item>(), db.storedItems.filter { it.status != ItemStatus.NO_DEAL })
    }

    /** 画面を離れる前に書き終えていること。**先に離れると viewModelScope ごと畳まれる**。 */
    @Test
    fun 完了が通知される時点で更新は終わっている() = runTest {
        db.seed(items = listOf(item(1, status = checked)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()
        var statusOnFinished: ItemStatus? = null

        viewModel.finishShopping { statusOnFinished = db.storedItem(1).status }
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, statusOnFinished)
    }

    /** チェックが 1 件も無くても終了できる（画面 04）。 */
    @Test
    fun チェック済みが1件も無くても終了できる() = runTest {
        db.seed(items = listOf(item(1, status = inBasket)))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()
        var finished = false

        viewModel.finishShopping { finished = true }
        advanceUntilIdle()

        assertTrue(finished)
        assertEquals(inBasket, db.storedItem(1).status)
    }
}
