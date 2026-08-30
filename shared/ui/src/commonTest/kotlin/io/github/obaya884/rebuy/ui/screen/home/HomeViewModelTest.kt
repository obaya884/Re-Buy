package io.github.obaya884.rebuy.ui.screen.home

import io.github.obaya884.rebuy.ui.CREATED_AT
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.ui.item
import io.github.obaya884.rebuy.ui.screen.home.HomeViewModel.Companion.RIPPLE_DELAY_MS
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.Test

/** ホーム画面の ViewModel。③ の移植で挙動が変わっていないことを確かめる網。 */
class HomeViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private val categoryRepository = CategoryRepository(db.categoryDao)

    private fun viewModel() = HomeViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = categoryRepository
    )

    // ---- Repository からの流れ込み ----

    @Test
    fun 初期値は空で始まる() = runTest {
        // 収集が走る前を観測する。DB が空だと初期値と収集後が同じで区別が付かない
        db.seed(items = listOf(item(1)), categories = listOf(category(1)))

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

        assertNull(viewModel.uiState.value.items.single().category)
    }

    @Test
    fun 後から追加された品目も流れ込む() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        db.add(item(1))
        advanceUntilIdle()

        assertEquals(listOf("アイテム1"), viewModel.uiState.value.items.map { it.item.name })
    }

    @Test
    fun カテゴリーを消すと品目に結びついたカテゴリーも消える() = runTest {
        // items と categories の 2 つの Flow を combine しているので、
        // カテゴリー側の変化でも品目が流れ直す必要がある
        db.seed(items = listOf(item(1, categoryId = 1)), categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        categoryRepository.delete(category(1))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.items.single().category)
    }

    // ---- カゴへの出し入れ ----

    @Test
    fun カゴに入れると買い物リストの状態になる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
    }

    @Test
    fun カゴに入れた結果がuiStateに返ってくる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(
            ItemStatus.IN_SHOPPING_LIST,
            viewModel.uiState.value.items.single().item.status
        )
    }

    @Test
    fun カゴに入れると更新日時が書き換わる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceUntilIdle()

        assertNotEquals(CREATED_AT, db.storedItem(1).updatedAt)
    }

    @Test
    fun チェック済みの品目もカゴに入れ直せる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
    }

    @Test
    fun カゴから出すと取引なしの状態に戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.removeFromBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    @Test
    fun チェック済みの品目もカゴから出せる() = runTest {
        // ホームのカゴ出しボタンは取引なし以外に出るので、チェック済みからも到達する
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.removeFromBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    // ---- 同じ状態への更新は握りつぶす（Repository の早期 return） ----

    @Test
    fun 既にカゴにある品目をカゴに入れても更新しない() = runTest {
        val original = item(1, status = ItemStatus.IN_SHOPPING_LIST)
        db.seed(items = listOf(original))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceUntilIdle()

        // 早期 return するので updatedAt も書き換わらない
        assertEquals(original, db.storedItem(1))
    }

    @Test
    fun 取引なしの品目をカゴから出しても更新しない() = runTest {
        val original = item(1)
        db.seed(items = listOf(original))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.removeFromBasket(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(original, db.storedItem(1))
    }

    // ---- Ripple のための遅延 ----

    @Test
    fun カゴに入れる操作は遅延の直前では効いていない() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        // advanceTimeBy はちょうどその時刻に積まれたタスクを実行しない
        advanceTimeBy(RIPPLE_DELAY_MS)

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    @Test
    fun カゴに入れる操作は遅延を過ぎると効く() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToBasket(db.storedItem(1))
        advanceTimeBy(RIPPLE_DELAY_MS + 1)

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
    }

    @Test
    fun カゴから出す操作も遅延の直前では効いていない() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.removeFromBasket(db.storedItem(1))
        advanceTimeBy(RIPPLE_DELAY_MS)

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
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
