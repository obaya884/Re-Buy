package io.github.obaya884.rebuy.ui.screen.item_edit

import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import io.github.obaya884.rebuy.ui.screen.NewNameTarget
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 品目編集シート（画面 06）の ViewModel。
 *
 * 見るのは 4 つ。**開いた品目の値が入ること**、**保存が名前・カテゴリ・行き先を
 * まとめて反映すること**（重複判定から自分自身は除く）、**削除**、
 * **「なし」で選択を外せること**。
 */
class ItemEditViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel() = ItemEditViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = CategoryRepository(db.categoryDao),
        destinationRepository = DestinationRepository(db.destinationDao)
    )

    private fun seedOneItem() = db.seed(
        items = listOf(item(id = 1, categoryId = 1, destinationId = 1)),
        categories = listOf(category(id = 1), category(id = 2)),
        destinations = listOf(destination(id = 1), destination(id = 2))
    )

    // ---- 開く ----

    @Test
    fun 開いた品目の値が入る() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.start(db.storedItem(1))
        advanceUntilIdle()

        val editing = assertNotNull(viewModel.uiState.value.editing)
        assertEquals("アイテム1", editing.name)
        assertEquals("アイテム1", editing.originalName)
        assertEquals(1, editing.categoryId)
        assertEquals(1, editing.destinationId)
    }

    /**
     * 開き直したときに**前回の編集もエラーもダイアログも残らない**
     * （シートより ViewModel が長生きする）。
     */
    @Test
    fun 別の品目を開くと前の状態は残らない() = runTest {
        db.seed(items = listOf(item(1), item(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))
        // 重複で弾かれた状態と、開いた 02b を作る
        viewModel.changeName("アイテム2")
        viewModel.save()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        advanceUntilIdle()

        viewModel.start(db.storedItem(2))
        advanceUntilIdle()

        assertEquals("アイテム2", assertNotNull(viewModel.uiState.value.editing).name)
        assertNull(viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.newNameDialog)
    }

    // ---- 保存 ----

    @Test
    fun 名前とカテゴリと行き先をまとめて保存する() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.changeName("アイテムA")
        viewModel.selectCategory(2)
        viewModel.selectDestination(2)
        viewModel.save()
        advanceUntilIdle()

        val stored = db.storedItem(1)
        assertEquals("アイテムA", stored.name)
        assertEquals(2, stored.categoryId)
        assertEquals(2, stored.destinationId)
        assertEquals(1, viewModel.closeRequests.value)
    }

    /** **重複判定から自分自身は除く**（画面 06）。名前を変えずに保存できる。 */
    @Test
    fun 名前を変えずに保存できる() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.selectCategory(2)
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.nameError)
        assertEquals(2, db.storedItem(1).categoryId)
    }

    @Test
    fun 他の品目と同じ名前には変えられない() = runTest {
        db.seed(items = listOf(item(1), item(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.changeName("アイテム2")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.nameError)
        assertEquals("アイテム1", db.storedItem(1).name)
        assertEquals(0, viewModel.closeRequests.value)
    }

    /** 名前が弾かれたら**カテゴリと行き先も書かない**（まとめて反映するので中途半端にしない）。 */
    @Test
    fun 名前が弾かれたらカテゴリも書かれない() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.changeName("   ")
        viewModel.selectCategory(2)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(NameError.BLANK, viewModel.uiState.value.nameError)
        assertEquals(1, db.storedItem(1).categoryId)
    }

    // ---- 「なし」 ----

    /** 「なし」チップで選択を外せる（画面 06）。 */
    @Test
    fun なしでカテゴリと行き先を外せる() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.clearCategory()
        viewModel.clearDestination()
        viewModel.save()
        advanceUntilIdle()

        assertNull(db.storedItem(1).categoryId)
        assertNull(db.storedItem(1).destinationId)
    }

    /** 同じチップをもう一度押しても外れる（§2）。 */
    @Test
    fun 同じチップをもう一度押すと外れる() = runTest {
        seedOneItem()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.selectCategory(1)
        viewModel.selectDestination(1)
        advanceUntilIdle()

        val editing = assertNotNull(viewModel.uiState.value.editing)
        assertNull(editing.categoryId)
        assertNull(editing.destinationId)
    }

    // ---- 削除 ----

    /** **物理削除で戻せない**（データモデル定義書 §7）。 */
    @Test
    fun 削除すると品目が消えて閉じる() = runTest {
        db.seed(items = listOf(item(1), item(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.delete()
        advanceUntilIdle()

        assertEquals(listOf(2), db.storedItems.map { it.id })
        assertEquals(1, viewModel.closeRequests.value)
    }

    /** 名前を打ちかけていても、削除するのは**開いた品目**。 */
    @Test
    fun 打ちかけの名前でも開いた品目を削除する() = runTest {
        db.seed(items = listOf(item(1)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))

        viewModel.changeName("書きかけ")
        viewModel.delete()
        advanceUntilIdle()

        assertEquals(emptyList(), db.storedItems)
    }

    // ---- 02b ----

    @Test
    fun 作ったカテゴリが選択済みになる() = runTest {
        db.seed(items = listOf(item(1)), categories = listOf(category(1, sortOrder = 5)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        viewModel.changeNewName("カテゴリA")

        viewModel.createNewName()
        advanceUntilIdle()

        val created = db.storedCategories.single { it.name == "カテゴリA" }
        assertEquals(2, created.id)
        assertEquals(6, created.sortOrder)
        assertEquals(2, assertNotNull(viewModel.uiState.value.editing).categoryId)
    }

    @Test
    fun 閉じると編集もエラーもダイアログも捨てる() = runTest {
        db.seed(items = listOf(item(1), item(2)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.start(db.storedItem(1))
        viewModel.changeName("アイテム2")
        viewModel.save()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        advanceUntilIdle()

        viewModel.reset()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNull(uiState.editing)
        assertNull(uiState.nameError)
        assertNull(uiState.newNameDialog)
        assertEquals(0, viewModel.closeRequests.value)
    }
}
