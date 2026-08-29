package io.github.obaya884.rebuy.ui.screen.home

import io.github.obaya884.rebuy.MainDispatcherRule
import io.github.obaya884.rebuy.category
import io.github.obaya884.rebuy.data.FakeDatabase
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.item
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * ホーム画面の ViewModel。③ の KMP 移植で挙動が変わっていないことを確かめる網。
 *
 * Repository は本物を使い、その下の DAO だけを [FakeDatabase] に差し替える。
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val db = FakeDatabase()

    private fun viewModel() = HomeViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = CategoryRepository(db.categoryDao)
    )

    // ---- Repository からの流れ込み ----

    @Test
    fun 初期状態は空() = runTest {
        val viewModel = viewModel()

        assertEquals(HomeScreenUiState(categories = listOf(), items = listOf()), viewModel.uiState.value)
    }

    @Test
    fun 品目がuiStateに流れ込む() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("アイテム1"), viewModel.uiState.value.items.map { it.item.name })
    }

    @Test
    fun カテゴリーがuiStateに流れ込む() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(category(1)), viewModel.uiState.value.categories)
    }

    @Test
    fun 品目にカテゴリーが結びつく() = runTest {
        db.seed(items = listOf(item(1, categoryId = 1)), categories = listOf(category(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(category(1), viewModel.uiState.value.items.single().category)
    }

    @Test
    fun カテゴリーが無い品目のcategoryはnull() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.items.single().category)
    }

    @Test
    fun 後から追加された品目も流れ込む() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        db.seed(items = listOf(item(1)))
        advanceUntilIdle()

        assertEquals(listOf("アイテム1"), viewModel.uiState.value.items.map { it.item.name })
    }

    // ---- カゴへの出し入れ ----

    @Test
    fun カゴに入れると買い物リストの状態になる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.item(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.item(1).status)
    }

    @Test
    fun カゴに入れた結果がuiStateに返ってくる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.item(1))
        advanceUntilIdle()

        assertEquals(
            ItemStatus.IN_SHOPPING_LIST,
            viewModel.uiState.value.items.single().item.status
        )
    }

    @Test
    fun カゴに入れる操作はすぐには効かない() = runTest {
        // Ripple effect を見せるための遅延。すぐ反映すると波紋が出る前に行が動く
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.item(1))
        advanceTimeBy(199)

        assertEquals(ItemStatus.NO_DEAL, db.item(1).status)
    }

    @Test
    fun カゴから出すと取引なしの状態に戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.removeFromBasket(db.item(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.item(1).status)
    }

    @Test
    fun 既にカゴにある品目をカゴに入れても更新しない() = runTest {
        val original = item(1, status = ItemStatus.IN_SHOPPING_LIST)
        db.seed(items = listOf(original))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.item(1))
        advanceUntilIdle()

        // Repository が早期 return するので updatedAt も書き換わらない
        assertEquals(original, db.item(1))
    }

    // ---- 派生値 ----

    @Test
    fun inBasketItemsは取引なし以外を集める() = runTest {
        db.seed(
            items = listOf(
                item(1),
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2, 3), viewModel.uiState.value.inBasketItems.map { it.item.id })
    }

    @Test
    fun inShoppingListItemsは買い物リストの状態だけを集める() = runTest {
        db.seed(
            items = listOf(
                item(1),
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.inShoppingListItems.map { it.item.id })
    }
}
