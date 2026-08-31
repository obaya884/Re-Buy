package io.github.obaya884.rebuy.ui.screen.category_edit

import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** カテゴリー編集画面の ViewModel。③ の移植で挙動が変わっていないことを確かめる網。 */
class CategoryEditViewModelTest : ViewModelTestBase() {

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
    // ---- 名前の検証（F-003）----

    /** 保存できたらダイアログは ViewModel が閉じる。画面側は閉じる呼び出しを持たない。 */
    @Test
    fun 追加できたらダイアログが閉じてエラーも出ない() = runTest {
        val viewModel = viewModel()
        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        viewModel.addCategory("カテゴリーA")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isShowCategoryAddDialog)
        assertNull(viewModel.nameError.value)
    }

    /** 弾かれたらダイアログは開いたまま（画面定義書 §2）。 */
    @Test
    fun 同じ名前で追加するとダイアログが開いたままエラーが出る() = runTest {
        db.seed(categories = listOf(category(1, name = "カテゴリーA")))
        val viewModel = viewModel()
        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        viewModel.addCategory("カテゴリーA")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryAddDialog)
        assertEquals(NameError.DUPLICATE, viewModel.nameError.value)
        assertEquals(1, db.storedCategories.size)
    }

    @Test
    fun 空の名前で追加するとダイアログが開いたままエラーが出る() = runTest {
        val viewModel = viewModel()
        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        viewModel.addCategory("   ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryAddDialog)
        assertEquals(NameError.BLANK, viewModel.nameError.value)
    }

    /** 弾かれた後に直して確定し直す導線。**保存できた時点でエラーが消える。** */
    @Test
    fun 直して確定し直すとエラーが消えてダイアログが閉じる() = runTest {
        db.seed(categories = listOf(category(1, name = "カテゴリーA")))
        val viewModel = viewModel()
        viewModel.showCategoryAddDialog()
        viewModel.addCategory("カテゴリーA")
        advanceUntilIdle()

        viewModel.addCategory("カテゴリーB")
        advanceUntilIdle()

        assertNull(viewModel.nameError.value)
        assertFalse(viewModel.uiState.value.isShowCategoryAddDialog)
        assertEquals(2, db.storedCategories.size)
    }

    /** 消えるのは開き直したときも。**追加で弾かれた理由が編集ダイアログに残らない。** */
    @Test
    fun 編集ダイアログを開くと追加で出たエラーが消える() = runTest {
        val viewModel = viewModel()
        viewModel.addCategory("")
        advanceUntilIdle()

        viewModel.showCategoryEditDialog()
        advanceUntilIdle()

        assertNull(viewModel.nameError.value)
    }

    /** 開き直したら前回のエラーは消える。 */
    @Test
    fun ダイアログを開き直すとエラーが消える() = runTest {
        val viewModel = viewModel()
        viewModel.addCategory("")
        advanceUntilIdle()

        viewModel.showCategoryAddDialog()
        advanceUntilIdle()

        assertNull(viewModel.nameError.value)
    }

    @Test
    fun 同じ名前へ改名するとダイアログが開いたままエラーが出る() = runTest {
        db.seed(categories = listOf(category(1, name = "カテゴリーA"), category(2)))
        val viewModel = viewModel()
        viewModel.showCategoryEditDialog()
        advanceUntilIdle()

        viewModel.editCategoryName(2, "カテゴリーA")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShowCategoryEditDialog)
        assertEquals(NameError.DUPLICATE, viewModel.nameError.value)
        assertEquals("カテゴリー2", db.storedCategories.single { it.id == 2 }.name)
    }
}
