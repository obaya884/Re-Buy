package io.github.obaya884.rebuy.ui.screen.item_edit

import io.github.obaya884.rebuy.ui.ViewModelTest
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** 品目編集画面の ViewModel。③ の移植で挙動が変わっていないことを確かめる網。 */
class ItemEditViewModelTest : ViewModelTest() {

    private val db = FakeDatabase()

    private fun viewModel() = ItemEditViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = CategoryRepository(db.categoryDao)
    )

    // ---- 一覧 ----

    @Test
    fun 品目がuiStateに流れ込む() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("アイテム1"), viewModel.uiState.value.items.map { it.item.name })
    }

    @Test
    fun カテゴリーの先頭に未選択を表すnullが入る() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(null, category(1)), viewModel.uiState.value.categories)
    }

    @Test
    fun カテゴリーが1件も無くてもnullだけは入る() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(null), viewModel.uiState.value.categories)
    }

    // ---- 追加・変更・削除 ----

    @Test
    fun 品目を追加できる() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addItem("追加した品目")
        advanceUntilIdle()

        assertEquals(listOf("追加した品目"), db.storedItems.map { it.name })
    }

    @Test
    fun 品目をカテゴリー付きで追加できる() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addItem("追加した品目", categoryId = 1)
        advanceUntilIdle()

        assertEquals(1, db.storedItems.single().categoryId)
    }

    @Test
    fun 追加した品目のカテゴリーは既定でnull() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addItem("追加した品目")
        advanceUntilIdle()

        assertNull(db.storedItems.single().categoryId)
    }

    @Test
    fun 追加した品目がuiStateに返ってくる() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addItem("追加した品目")
        advanceUntilIdle()

        assertEquals(
            listOf("追加した品目"),
            viewModel.uiState.value.items.map { it.item.name }
        )
    }

    @Test
    fun 存在しない品目の名前を変えても何も起きない() = runTest {
        val original = listOf(item(1))
        db.seed(items = original)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editItemName(999, "変更後の名前")
        advanceUntilIdle()

        assertEquals(original, db.storedItems)
    }

    @Test
    fun 品目名を変更できる() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editItemName(1, "変更後の名前")
        advanceUntilIdle()

        assertEquals("変更後の名前", db.storedItem(1).name)
    }

    @Test
    fun 品目のカテゴリーを変更できる() = runTest {
        db.seed(items = listOf(item(1)), categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editItemCategory(1, 1)
        advanceUntilIdle()

        assertEquals(1, db.storedItem(1).categoryId)
    }

    @Test
    fun 品目のカテゴリーを未選択に戻せる() = runTest {
        db.seed(items = listOf(item(1, categoryId = 1)), categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editItemCategory(1, null)
        advanceUntilIdle()

        assertNull(db.storedItem(1).categoryId)
    }

    @Test
    fun 編集対象の品目を削除できる() = runTest {
        db.seed(items = listOf(item(1), item(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setEditingItem(item(1))
        advanceUntilIdle()

        viewModel.deleteItem()
        advanceUntilIdle()

        assertEquals(listOf(2), db.storedItems.map { it.id })
    }

    @Test
    fun 編集対象が無ければ削除しても何も起きない() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.deleteItem()
        advanceUntilIdle()

        assertEquals(listOf(1), db.storedItems.map { it.id })
    }

    // ---- 編集対象 ----

    @Test
    fun 編集対象は初期状態ではnull() = runTest {
        val viewModel = viewModel()

        assertNull(viewModel.uiState.value.editingItem)
    }

    @Test
    fun 編集対象を選べる() = runTest {
        val viewModel = viewModel()

        viewModel.setEditingItem(item(1))
        advanceUntilIdle()

        assertEquals(item(1), viewModel.uiState.value.editingItem)
    }

    // ---- ダイアログ ----

    @Test
    fun 追加ダイアログは初期状態では閉じている() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.isShowItemAddDialog)
    }

    @Test
    fun 追加ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showItemAddDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowItemAddDialog)
    }

    @Test
    fun 追加ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showItemAddDialog()
        advanceUntilIdle()

        viewModel.hideItemAddDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowItemAddDialog)
    }

    @Test
    fun 編集ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showItemEditDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowItemEditDialog)
    }

    @Test
    fun 編集ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showItemEditDialog()
        advanceUntilIdle()

        viewModel.hideItemEditDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowItemEditDialog)
    }

    @Test
    fun 削除ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showItemDeleteDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowItemDeleteDialog)
    }

    @Test
    fun 削除ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showItemDeleteDialog()
        advanceUntilIdle()

        viewModel.hideItemDeleteDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowItemDeleteDialog)
    }

    @Test
    fun 追加ダイアログを開いても編集ダイアログは開かない() = runTest {
        val viewModel = viewModel()

        viewModel.showItemAddDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowItemEditDialog)
    }
}
