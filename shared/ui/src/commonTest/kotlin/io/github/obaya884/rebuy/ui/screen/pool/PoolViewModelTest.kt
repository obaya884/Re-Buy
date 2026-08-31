package io.github.obaya884.rebuy.ui.screen.pool

import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * プール画面（画面 01）の ViewModel。
 *
 * 見るのは 3 つ。**一覧が登録順で名前を結んで出ること**、**行タップのカゴ出し入れ**
 * （データモデル定義書 §3）、**絞り込みの AND 結合と解除**（画面 01）。
 */
class PoolViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel() = PoolViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = CategoryRepository(db.categoryDao),
        destinationRepository = DestinationRepository(db.destinationDao)
    )

    /** 品目 3 件。カテゴリーと行き先の組み合わせを絞り込みのテストで使い回す。 */
    private fun seedThreeItems() = db.seed(
        items = listOf(
            item(id = 1, categoryId = 1, destinationId = 1),
            item(id = 2, categoryId = 1),
            item(id = 3, destinationId = 2)
        ),
        categories = listOf(category(id = 1)),
        destinations = listOf(destination(id = 1), destination(id = 2))
    )

    // ---- 一覧 ----

    /**
     * **読み込み前を空状態と読み違えない。** DB から最初の値が届くまでは一覧も空だが、
     * ここで空状態を出すと、起動のたびに「まだ何も登録されていません」が一瞬見える。
     */
    @Test
    fun 読み込み前は空状態にしない() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()

        assertEquals(emptyList(), viewModel.uiState.value.items)
        assertFalse(viewModel.uiState.value.isEmpty)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun 品目が登録順で流れ込む() = runTest {
        db.seed(items = listOf(item(3), item(1), item(2)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.visibleItems.map { it.item.id })
    }

    /** 行に出すカテゴリー名・行き先名は、それぞれの一覧から結ぶ。 */
    @Test
    fun カテゴリーと行き先が行に結ばれる() = runTest {
        seedThreeItems()
        val viewModel = viewModel()

        advanceUntilIdle()

        val rows = viewModel.uiState.value.visibleItems.associateBy { it.item.id }
        assertEquals("カテゴリー1", rows.getValue(1).category?.name)
        assertEquals("行き先1", rows.getValue(1).destination?.name)
        // どこでも買えるもの（行き先なし）
        assertNull(rows.getValue(2).destination)
    }

    @Test
    fun 総数は絞り込んでも変わらない() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalCount)
        assertEquals(2, viewModel.uiState.value.visibleItems.size)
    }

    // ---- カゴの出し入れ ----

    @Test
    fun 行タップでカゴに入る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.NO_DEAL)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
        assertEquals(1, viewModel.uiState.value.basketCount)
    }

    @Test
    fun もう一度タップでカゴから出る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    /** チェック済み（状態 2）から出しても常駐（0）へ戻る。**チェックは失われる。** */
    @Test
    fun チェック済みでもタップでカゴから出る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    /** カゴ件数はチェック済みも数える（CTA のバッジ）。 */
    @Test
    fun カゴ件数はチェック済みも数える() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.IN_SHOPPING_LIST),
                item(2, status = ItemStatus.CHECKED_IN_SHOPPING_LIST),
                item(3, status = ItemStatus.NO_DEAL)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.basketCount)
        assertTrue(viewModel.uiState.value.canStartShopping)
    }

    @Test
    fun カゴが空なら買い物を始められない() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canStartShopping)
    }

    // ---- 絞り込み ----

    @Test
    fun カテゴリーで絞れる() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.uiState.value.visibleItems.map { it.item.id })
    }

    @Test
    fun 行き先で絞れる() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectDestination(DestinationFilter.Only(1))
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.visibleItems.map { it.item.id })
    }

    /** 「どこでも」は**行き先なしだけ**（画面 01。厳密な絞り）。 */
    @Test
    fun どこでもは行き先なしだけを出す() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectDestination(DestinationFilter.Anywhere)
        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.visibleItems.map { it.item.id })
    }

    /** カテゴリーと行き先は AND で結ぶ。 */
    @Test
    fun カテゴリーと行き先はAND() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        viewModel.selectDestination(DestinationFilter.Only(1))
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.visibleItems.map { it.item.id })
    }

    @Test
    fun 同じチップをもう一度押すと解除される() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        viewModel.selectCategory(1)
        viewModel.selectDestination(DestinationFilter.Only(1))
        viewModel.selectDestination(DestinationFilter.Only(1))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertEquals(DestinationFilter.All, viewModel.uiState.value.destinationFilter)
        assertEquals(3, viewModel.uiState.value.visibleItems.size)
    }

    /** 「すべて」は両方を一度に解除する（画面 01）。 */
    @Test
    fun すべてで両方の絞り込みが解ける() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        viewModel.selectDestination(DestinationFilter.Only(1))
        viewModel.clearFilters()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertEquals(DestinationFilter.All, viewModel.uiState.value.destinationFilter)
    }

    // ---- 空状態 ----

    @Test
    fun 品目が無ければ空状態() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isFilteredEmpty)
    }

    /** 品目はあるが絞り込みで 0 件になった場合は、空状態とは別の文言を出す（画面 01）。 */
    @Test
    fun 絞り込んで0件なら絞り込み用の空() = runTest {
        seedThreeItems()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        viewModel.selectDestination(DestinationFilter.Only(2))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEmpty)
        assertTrue(viewModel.uiState.value.isFilteredEmpty)
    }
}
