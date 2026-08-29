package io.github.obaya884.rebuy.ui.screen.shopping

import io.github.obaya884.rebuy.CREATED_AT
import io.github.obaya884.rebuy.MainDispatcherRule
import io.github.obaya884.rebuy.data.FakeDatabase
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.item
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 買い物画面の ViewModel。③ の KMP 移植で挙動が変わっていないことを確かめる網。
 *
 * Repository は本物を使い、その下の DAO だけを [FakeDatabase] に差し替える。
 */
class ShoppingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val db = FakeDatabase()

    private fun viewModel() = ShoppingViewModel(itemRepository = ItemRepository(db.itemDao))

    // ---- チェックの付け外し ----

    @Test
    fun チェックを付けると確認済みの状態になる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.item(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.CHECKED_IN_SHOPPING_LIST, db.item(1).status)
    }

    @Test
    fun チェックを外すと買い物リストの状態に戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.unMarkScheduledBought(db.item(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.item(1).status)
    }

    // ---- 買い物の終了 ----

    @Test
    fun 買い物を終えるとチェック済みの品目が取引なしに戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.item(1).status)
    }

    @Test
    fun 買い物を終えるとチェック済みの品目に最終購入日が入る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertNotEquals(null, db.item(1).lastBoughtAt)
    }

    @Test
    fun 買い物を終えてもチェックしていない品目は買い物リストに残る() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.IN_SHOPPING_LIST),
                item(2, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.item(1).status)
    }

    @Test
    fun 買い物を終えてもチェックしていない品目に最終購入日は入らない() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.IN_SHOPPING_LIST),
                item(2, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertNull(db.item(1).lastBoughtAt)
    }

    @Test
    fun 買い物を終えると完了が通知される() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()
        var finished = false

        viewModel.changeBoughtConfirm { finished = true }
        advanceUntilIdle()

        assertTrue(finished)
    }

    @Test
    fun 買い物を終える処理の途中は読み込み中になる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        // 処理中に 500 ミリ秒の待ちがあるので、その手前で観測する
        advanceTimeBy(499)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun 買い物を終えると読み込み中が解除される() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ---- 終了確認ダイアログ ----

    @Test
    fun 終了確認ダイアログは初期状態では閉じている() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.isShowFinishShoppingAlertDialog)
    }

    @Test
    fun 終了確認ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showFinishShoppingAlertDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowFinishShoppingAlertDialog)
    }

    @Test
    fun 終了確認ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showFinishShoppingAlertDialog()
        advanceUntilIdle()

        viewModel.hideFinishShoppingAlertDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowFinishShoppingAlertDialog)
    }

    // ---- 派生値 ----

    @Test
    fun shoppingListItemsは買い物リストと確認済みを集める() = runTest {
        db.seed(
            items = listOf(
                item(1),
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2, 3), viewModel.uiState.value.shoppingListItems.map { it.id })
    }

    @Test
    fun checkedInShoppingListItemsは確認済みだけを集める() = runTest {
        db.seed(
            items = listOf(
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(3), viewModel.uiState.value.checkedInShoppingListItems.map { it.id })
    }

    @Test
    fun 確認済みが1件でもあればisExistCheckedInShoppingListItemsが立つ() = runTest {
        db.seed(
            items = listOf(
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isExistCheckedInShoppingListItems)
    }

    @Test
    fun 確認済みが無ければisExistCheckedInShoppingListItemsは立たない() = runTest {
        db.seed(items = listOf(item(2, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isExistCheckedInShoppingListItems)
    }

    @Test
    fun inShoppingListItemsは買い物リストの状態だけを集める() = runTest {
        db.seed(
            items = listOf(
                item(2, status = ItemStatus.IN_SHOPPING_LIST),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(2), viewModel.uiState.value.inShoppingListItems.map { it.item.id })
    }

    @Test
    fun 最終購入日は買い物を終えるまで変わらない() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.item(1))
        advanceUntilIdle()

        assertNull(db.item(1).lastBoughtAt)
    }

    @Test
    fun チェックを付けてもcreatedAtは変わらない() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.item(1))
        advanceUntilIdle()

        assertEquals(CREATED_AT, db.item(1).createdAt)
    }
}
