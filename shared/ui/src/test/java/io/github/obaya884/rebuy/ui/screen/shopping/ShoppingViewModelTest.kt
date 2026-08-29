package io.github.obaya884.rebuy.ui.screen.shopping

import io.github.obaya884.rebuy.CREATED_AT
import io.github.obaya884.rebuy.MainDispatcherRule
import io.github.obaya884.rebuy.data.FakeDatabase
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.item
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingViewModel.Companion.FINISH_SHOPPING_DELAY_MS
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 買い物画面の ViewModel。③ の移植で挙動が変わっていないことを確かめる網。 */
class ShoppingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val db = FakeDatabase()

    private fun viewModel() = ShoppingViewModel(itemRepository = ItemRepository(db.itemDao))

    // ---- 流れ込み ----

    @Test
    fun 初期値は空で始まる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))

        val viewModel = viewModel()

        assertEquals(emptyList<Item>(), viewModel.uiState.value.shoppingListItems)
    }

    // ---- チェックの付け外し ----

    @Test
    fun チェックを付けると確認済みの状態になる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.CHECKED_IN_SHOPPING_LIST, db.storedItem(1).status)
    }

    @Test
    fun チェックを外すと買い物リストの状態に戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.unMarkScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
    }

    @Test
    fun 既にチェック済みの品目にチェックを付けても更新しない() = runTest {
        val original = item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
        db.seed(items = listOf(original))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        // 早期 return するので updatedAt も書き換わらない
        assertEquals(original, db.storedItem(1))
    }

    @Test
    fun チェックの付いていない品目のチェックを外しても更新しない() = runTest {
        val original = item(1, status = ItemStatus.IN_SHOPPING_LIST)
        db.seed(items = listOf(original))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.unMarkScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(original, db.storedItem(1))
    }

    @Test
    fun チェックを付けても最終購入日は変わらない() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        assertNull(db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun チェックを付けても作成日時は変わらない() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.markScheduledBought(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(CREATED_AT, db.storedItem(1).createdAt)
    }

    // ---- 買い物の終了 ----

    @Test
    fun 買い物を終えるとチェック済みの品目が取引なしに戻る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, db.storedItem(1).status)
    }

    @Test
    fun 買い物を終えるとチェック済みの品目に最終購入日が入る() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertNotNull(db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun 最終購入日は更新日時と同じ値になる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(db.storedItem(1).updatedAt, db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun 前回の最終購入日は上書きされる() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST, lastBoughtAt = CREATED_AT)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertNotEquals(CREATED_AT, db.storedItem(1).lastBoughtAt)
    }

    @Test
    fun チェック済みが複数あればすべて取引なしに戻る() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST),
                item(2, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(
            listOf(ItemStatus.NO_DEAL, ItemStatus.NO_DEAL),
            db.storedItems.map { it.status }
        )
    }

    @Test
    fun チェック済みが複数あればすべてに最終購入日が入る() = runTest {
        db.seed(
            items = listOf(
                item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST),
                item(2, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceUntilIdle()

        assertEquals(emptyList<Item>(), db.storedItems.filter { it.lastBoughtAt == null })
    }

    @Test
    fun 完了が通知される時点で更新は終わっている() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()
        var statusOnFinished: ItemStatus? = null

        viewModel.changeBoughtConfirm { statusOnFinished = db.storedItem(1).status }
        advanceUntilIdle()

        assertEquals(ItemStatus.NO_DEAL, statusOnFinished)
    }

    @Test
    fun チェック済みが1件も無くても完了が通知される() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()
        var finished = false

        viewModel.changeBoughtConfirm { finished = true }
        advanceUntilIdle()

        assertTrue(finished)
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

        assertEquals(ItemStatus.IN_SHOPPING_LIST, db.storedItem(1).status)
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

        assertNull(db.storedItem(1).lastBoughtAt)
    }

    // ---- 終了処理中の読み込み表示 ----

    @Test
    fun 買い物を終える処理の途中は読み込み中になる() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        // advanceTimeBy はちょうどその時刻に積まれたタスクを実行しない
        advanceTimeBy(FINISH_SHOPPING_DELAY_MS)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun 遅延を過ぎると読み込み中が解除される() = runTest {
        db.seed(items = listOf(item(1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeBoughtConfirm {}
        advanceTimeBy(FINISH_SHOPPING_DELAY_MS + 1)

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun 初期状態では読み込み中ではない() = runTest {
        val viewModel = viewModel()

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
}
