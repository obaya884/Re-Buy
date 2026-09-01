package io.github.obaya884.rebuy.ui.screen.shopping_start

import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 買い物開始シート（画面 03）の内訳（データモデル定義書 §4）。
 *
 * 見るのは 4 つ。**カゴに品目を持つ行き先だけが並び順で出ること**、
 * **プレビューは登録順の先頭 2 件**、**件数は「n＋m」で m はどこでも買えるもの**、
 * **行き先付きが 1 件も無ければ全件モード**。
 */
class ShoppingStartViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel() = ShoppingStartViewModel(
        itemRepository = ItemRepository(db.itemDao),
        destinationRepository = DestinationRepository(db.destinationDao)
    )

    private val inBasket = ItemStatus.IN_SHOPPING_LIST

    @Test
    fun カゴに品目を持つ行き先だけが並び順で出る() = runTest {
        db.seed(
            items = listOf(
                // 行き先 2 のカゴ入り
                item(1, status = inBasket, destinationId = 2),
                // 行き先 1 は常駐だけ＝出ない
                item(2, destinationId = 1),
                // 行き先 3 のカゴ入り
                item(3, status = inBasket, destinationId = 3)
            ),
            destinations = listOf(
                destination(1, sortOrder = 1),
                destination(2, sortOrder = 2),
                destination(3, sortOrder = 3)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2, 3), viewModel.uiState.value.rows.map { it.destinationId })
    }

    /** 並びは行き先の手動並び順に従う（データモデル定義書 §6）。 */
    @Test
    fun 行は行き先の並び順に従う() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, status = inBasket, destinationId = 2)
            ),
            destinations = listOf(destination(1, sortOrder = 2), destination(2, sortOrder = 1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2, 1), viewModel.uiState.value.rows.map { it.destinationId })
    }

    /** プレビューは**登録順の先頭 2 件**。3 件目以降は出さず、「など」も付けない。 */
    @Test
    fun プレビューは登録順の先頭2件() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1, name = "アイテムA"),
                item(2, status = inBasket, destinationId = 1, name = "アイテムB"),
                item(3, status = inBasket, destinationId = 1, name = "アイテムC")
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val row = viewModel.uiState.value.rows.single()
        assertEquals(listOf("アイテムA", "アイテムB"), row.preview)
        assertEquals(3, row.count)
    }

    /** 件数の m は**どこでも買えるもの**（行き先なし）のカゴ内件数。 */
    @Test
    fun どこでも買えるものは各行の件数に足される() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, status = inBasket, destinationId = 2),
                item(3, status = inBasket),
                item(4, status = inBasket)
            ),
            destinations = listOf(destination(1), destination(2))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        // どこでも買えるものは独立した行にしない
        assertEquals(listOf(1, 2), rows.map { it.destinationId })
        assertEquals(listOf(1, 1), rows.map { it.count })
        assertEquals(listOf(2, 2), rows.map { it.anywhereCount })
    }

    @Test
    fun どこでも買えるものが無ければmは0() = runTest {
        db.seed(
            items = listOf(item(1, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.rows.single().anywhereCount)
    }

    /** カゴに入っていない品目は数えない（状態 0）。 */
    @Test
    fun カゴに入っていない品目は数えない() = runTest {
        db.seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1),
                item(2, destinationId = 1),
                item(3)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val row = viewModel.uiState.value.rows.single()
        assertEquals(1, row.count)
        assertEquals(0, row.anywhereCount)
        assertEquals(1, viewModel.uiState.value.basketCount)
    }

    /** チェック済み（状態 2）もカゴのうち。 */
    @Test
    fun チェック済みもカゴとして数える() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST, destinationId = 1)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.rows.single().count)
    }

    // ---- 全件モード ----

    /** カゴに行き先付きが 1 件も無ければ全件モード（データモデル定義書 §4）。 */
    @Test
    fun 行き先付きが無ければ全件モード() = runTest {
        db.seed(
            items = listOf(item(1, status = inBasket), item(2, status = inBasket)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAllMode)
        assertEquals(2, viewModel.uiState.value.basketCount)
        assertEquals(emptyList(), viewModel.uiState.value.rows)
    }

    /** 行き先付きが 1 件でもあれば内訳を出す。 */
    @Test
    fun 行き先付きが1件でもあれば内訳() = runTest {
        db.seed(
            items = listOf(item(1, status = inBasket, destinationId = 1), item(2, status = inBasket)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAllMode)
    }

    /** **カゴが空なら全件モードにもしない**（そもそも CTA が押せない）。 */
    @Test
    fun カゴが空なら全件モードにもしない() = runTest {
        db.seed(items = listOf(item(1)), destinations = listOf(destination(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAllMode)
        assertEquals(0, viewModel.uiState.value.basketCount)
    }
}
