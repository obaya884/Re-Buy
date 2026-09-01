package io.github.obaya884.rebuy.ui.screen.manage

import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.CREATED_AT
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.category
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import io.github.obaya884.rebuy.ui.screen.NameTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * カテゴリの管理／行き先の管理（画面 09）と編集シート（09b）。
 *
 * **2 つは同型**なので、向きで変わるところ（一覧の出どころ・削除の影響先）だけ
 * 両方で見て、残りはカテゴリ側で見る。
 *
 * 並び替えで見るのは 3 つ。**ドラッグ中は入れ替わって見えること**、
 * **ドラッグ中は DB に書かないこと**、**離した時点で保存されること**（画面 09）。
 */
class ManageViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel(target: NameTarget = NameTarget.CATEGORY) = ManageViewModel(
        categoryRepository = CategoryRepository(db.categoryDao),
        destinationRepository = DestinationRepository(db.destinationDao),
        itemRepository = ItemRepository(db.itemDao),
        target = target
    )

    private fun seedThree() = db.seed(
        categories = listOf(
            category(id = 1, sortOrder = 1),
            category(id = 2, sortOrder = 2),
            category(id = 3, sortOrder = 3)
        )
    )

    // ---- 一覧 ----

    @Test
    fun カテゴリ向きならカテゴリが並び順で出る() = runTest {
        db.seed(
            categories = listOf(category(id = 1, sortOrder = 2), category(id = 2, sortOrder = 1)),
            destinations = listOf(destination(id = 9))
        )
        val viewModel = viewModel(NameTarget.CATEGORY)

        advanceUntilIdle()

        assertEquals(listOf(2, 1), viewModel.uiState.value.rows.map { it.id })
    }

    @Test
    fun 行き先向きなら行き先が出る() = runTest {
        db.seed(
            categories = listOf(category(id = 1)),
            destinations = listOf(destination(id = 9, name = "行き先X"))
        )
        val viewModel = viewModel(NameTarget.DESTINATION)

        advanceUntilIdle()

        assertEquals(listOf("行き先X"), viewModel.uiState.value.rows.map { it.name })
    }

    /** 空状態は破線行だけを出す（画面 09）。 */
    @Test
    fun 一件も無ければ空状態() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    // ---- 並び替え ----

    @Test
    fun ドラッグ中は落とし先を当てた並びが見える() = runTest {
        seedThree()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startDrag(index = 2)
        viewModel.dragTo(index = 0)
        advanceUntilIdle()

        assertEquals(listOf(3, 1, 2), viewModel.uiState.value.rows.map { it.id })
        // 掴んでいる行は持ち上げて描く
        assertEquals(3, viewModel.uiState.value.draggingId)
    }

    /** **離すまで書かない**（画面 09）。指を動かすたびに DB を叩かない。 */
    @Test
    fun ドラッグ中はDBに書かない() = runTest {
        seedThree()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startDrag(index = 2)
        viewModel.dragTo(index = 0)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), db.categoryDao.getAllCategories().first().map { it.id })
    }

    @Test
    fun 離すと並びが保存される() = runTest {
        seedThree()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startDrag(index = 2)
        viewModel.dragTo(index = 0)
        viewModel.endDrag()
        advanceUntilIdle()

        assertEquals(listOf(3, 1, 2), db.categoryDao.getAllCategories().first().map { it.id })
        assertNull(viewModel.uiState.value.drag)
    }

    /** 掴んで元の位置で離したら何も書かない。 */
    @Test
    fun 同じ位置で離せば書かない() = runTest {
        seedThree()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startDrag(index = 1)
        viewModel.endDrag()
        advanceUntilIdle()

        val stored = db.categoryDao.getAllCategories().first()
        assertEquals(listOf(CREATED_AT, CREATED_AT, CREATED_AT), stored.map { it.updatedAt })
    }

    // ---- 09b 編集シート ----

    @Test
    fun 長押しで開くと元の名前を持つ() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startEditing(viewModel.uiState.value.rows.single())
        advanceUntilIdle()

        assertEquals("カテゴリA", viewModel.uiState.value.editing?.originalName)
        assertEquals("カテゴリA", viewModel.uiState.value.editing?.name)
    }

    @Test
    fun 保存すると名前が変わって閉じる() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.startEditing(viewModel.uiState.value.rows.single())

        viewModel.changeName("カテゴリB")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("カテゴリB", viewModel.uiState.value.rows.single().name)
        assertNull(viewModel.uiState.value.editing)
    }

    /** 弾かれたらシートは閉じない（画面定義書 §2）。 */
    @Test
    fun 同じ名前があると弾かれて閉じない() = runTest {
        db.seed(
            categories = listOf(category(id = 1, name = "カテゴリA"), category(id = 2, name = "カテゴリB"))
        )
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.startEditing(viewModel.uiState.value.rows.first())

        viewModel.changeName("カテゴリB")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.nameError)
        assertEquals("カテゴリA", viewModel.uiState.value.rows.first().name)
        assertEquals("カテゴリA", viewModel.uiState.value.editing?.originalName)
    }

    /** **見出しは編集前の名前**。打ちかけの名前で「◯◯を削除しますか？」と聞かない。 */
    @Test
    fun 打ちかけの名前は見出しに映らない() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.startEditing(viewModel.uiState.value.rows.single())

        viewModel.changeName("打ちかけ")
        advanceUntilIdle()

        assertEquals("カテゴリA", viewModel.uiState.value.editing?.originalName)
    }

    @Test
    fun 閉じると打ちかけの名前もエラーも残らない() = runTest {
        db.seed(
            categories = listOf(category(id = 1, name = "カテゴリA"), category(id = 2, name = "カテゴリB"))
        )
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.startEditing(viewModel.uiState.value.rows.first())
        viewModel.changeName("カテゴリB")
        viewModel.save()
        advanceUntilIdle()

        viewModel.dismissEditing()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editing)
        assertNull(viewModel.uiState.value.nameError)
    }

    /** **品目は消えない**。カテゴリなしに戻るだけ（データモデル定義書 §7）。 */
    @Test
    fun 削除しても紐づく品目は残る() = runTest {
        db.seed(
            items = listOf(item(id = 1, categoryId = 1)),
            categories = listOf(category(id = 1))
        )
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.startEditing(viewModel.uiState.value.rows.single())

        viewModel.delete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(1, db.storedItems.size)
        assertNull(db.storedItem(1).categoryId)
        assertNull(viewModel.uiState.value.editing)
    }

    // ---- 削除の影響件数 ----

    @Test
    fun 削除の影響件数はカテゴリに紐づく品目を数える() = runTest {
        db.seed(
            items = listOf(item(id = 1, categoryId = 1), item(id = 2, categoryId = 1), item(id = 3)),
            categories = listOf(category(id = 1))
        )
        val viewModel = viewModel(NameTarget.CATEGORY)
        advanceUntilIdle()

        viewModel.startEditing(viewModel.uiState.value.rows.single())
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.affectedItemCount)
    }

    @Test
    fun 行き先向きなら行き先に紐づく品目を数える() = runTest {
        db.seed(
            items = listOf(item(id = 1, destinationId = 1), item(id = 2, categoryId = 1)),
            categories = listOf(category(id = 1)),
            destinations = listOf(destination(id = 1))
        )
        val viewModel = viewModel(NameTarget.DESTINATION)
        advanceUntilIdle()

        viewModel.startEditing(viewModel.uiState.value.rows.single())
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.affectedItemCount)
    }

    @Test
    fun 紐づく品目が無ければゼロ件() = runTest {
        db.seed(items = listOf(item(id = 1)), categories = listOf(category(id = 1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startEditing(viewModel.uiState.value.rows.single())
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.affectedItemCount)
    }

    // ---- 02b 追加ダイアログ ----

    /** 作った行は末尾に現れる（画面 09。採番は Repository が持つ）。 */
    @Test
    fun 追加した行は末尾に現れる() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showAddDialog()
        viewModel.changeNewName("カテゴリZ")
        viewModel.createNewName()
        advanceUntilIdle()

        assertEquals(listOf("カテゴリA", "カテゴリZ"), viewModel.uiState.value.rows.map { it.name })
        assertNull(viewModel.uiState.value.addDialog)
    }

    @Test
    fun 追加が弾かれたらダイアログは閉じない() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showAddDialog()
        viewModel.changeNewName("カテゴリA")
        viewModel.createNewName()
        advanceUntilIdle()

        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.addDialog?.error)
        assertEquals(1, viewModel.uiState.value.rows.size)
    }

    /** 行き先向きなら行き先が増える（ダイアログの向きも [NameTarget] で決まる）。 */
    @Test
    fun 行き先向きの追加は行き先が増える() = runTest {
        val viewModel = viewModel(NameTarget.DESTINATION)
        advanceUntilIdle()

        viewModel.showAddDialog()
        viewModel.changeNewName("行き先Z")
        viewModel.createNewName()
        advanceUntilIdle()

        assertEquals(listOf("行き先Z"), viewModel.uiState.value.rows.map { it.name })
        assertEquals(emptyList(), db.categoryDao.getAllCategories().first())
    }
}
