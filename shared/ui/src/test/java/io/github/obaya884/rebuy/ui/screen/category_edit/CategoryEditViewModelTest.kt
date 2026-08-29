package io.github.obaya884.rebuy.ui.screen.category_edit

import io.github.obaya884.rebuy.ui.MainDispatcherRule
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** カテゴリー編集画面の ViewModel。③ の移植で挙動が変わっていないことを確かめる網。 */
class CategoryEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val db = FakeDatabase()

    private fun viewModel() =
        CategoryEditViewModel(categoryRepository = CategoryRepository(db.categoryDao))

    // ---- 一覧 ----

    @Test
    fun 初期値は空で始まる() = runTest {
        // 収集が走る前を観測する。DB が空だと初期値と収集後が同じで区別が付かない
        db.seed(categories = listOf(category(1)))

        val viewModel = viewModel()

        assertEquals(emptyList<Category>(), viewModel.uiState.value.categories)
    }

    @Test
    fun カテゴリーがuiStateに流れ込む() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(category(1)), viewModel.uiState.value.categories)
    }

    // ---- 追加・変更・削除 ----

    @Test
    fun カテゴリーを追加できる() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addCategory("追加したカテゴリー")
        advanceUntilIdle()

        assertEquals(listOf("追加したカテゴリー"), db.storedCategories.map { it.name })
    }

    @Test
    fun 追加したカテゴリーがuiStateに返ってくる() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addCategory("追加したカテゴリー")
        advanceUntilIdle()

        assertEquals(
            listOf("追加したカテゴリー"),
            viewModel.uiState.value.categories.map { it.name }
        )
    }

    @Test
    fun カテゴリー名を変更できる() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editCategoryName(1, "変更後の名前")
        advanceUntilIdle()

        assertEquals("変更後の名前", db.storedCategories.single().name)
    }

    @Test
    fun 編集対象のカテゴリーを削除できる() = runTest {
        db.seed(categories = listOf(category(1), category(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setEditingCategory(category(1))
        advanceUntilIdle()

        viewModel.deleteCategory()
        advanceUntilIdle()

        assertEquals(listOf(2), db.storedCategories.map { it.id })
    }

    @Test
    fun 編集対象が無ければ削除しても何も起きない() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.deleteCategory()
        advanceUntilIdle()

        assertEquals(listOf(1), db.storedCategories.map { it.id })
    }

    @Test
    fun カテゴリーを削除するとその品目のカテゴリーが外れる() = runTest {
        db.seed(items = listOf(item(1, categoryId = 1)), categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setEditingCategory(category(1))
        advanceUntilIdle()

        viewModel.deleteCategory()
        advanceUntilIdle()

        assertNull(db.storedItem(1).categoryId)
    }

    @Test
    fun 存在しないカテゴリーの名前を変えても何も起きない() = runTest {
        val original = listOf(category(1))
        db.seed(categories = original)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.editCategoryName(999, "変更後の名前")
        advanceUntilIdle()

        assertEquals(original, db.storedCategories)
    }

    // ---- 編集対象 ----

    @Test
    fun 編集対象は初期状態ではnull() = runTest {
        val viewModel = viewModel()

        assertNull(viewModel.uiState.value.editingCategory)
    }

    @Test
    fun 編集対象を選べる() = runTest {
        val viewModel = viewModel()

        viewModel.setEditingCategory(category(1))
        advanceUntilIdle()

        assertEquals(category(1), viewModel.uiState.value.editingCategory)
    }

    // ---- ダイアログ ----

    @Test
    fun 追加ダイアログは初期状態では閉じている() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.isShowCategoryAddDialog)
    }

    @Test
    fun 追加ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryAddDialog)
    }

    @Test
    fun 追加ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        viewModel.hideCategoryAddDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowCategoryAddDialog)
    }

    @Test
    fun 編集ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showCategoryEditDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryEditDialog)
    }

    @Test
    fun 編集ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showCategoryEditDialog()
        advanceUntilIdle()

        viewModel.hideCategoryEditDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowCategoryEditDialog)
    }

    @Test
    fun 削除ダイアログを開ける() = runTest {
        val viewModel = viewModel()

        viewModel.showCategoryDeleteDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryDeleteDialog)
    }

    @Test
    fun 削除ダイアログを閉じられる() = runTest {
        val viewModel = viewModel()
        viewModel.showCategoryDeleteDialog()
        advanceUntilIdle()

        viewModel.hideCategoryDeleteDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowCategoryDeleteDialog)
    }

    @Test
    fun 追加ダイアログを開いても編集ダイアログは開かない() = runTest {
        val viewModel = viewModel()

        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowCategoryEditDialog)
    }
}
