package io.github.obaya884.rebuy.ui.screen.register

import io.github.obaya884.rebuy.data.item.ItemStatus
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
 * 登録シート（画面 02）と新規作成ダイアログ（02b）の ViewModel。
 *
 * 見るのは 4 つ。**名前だけで登録できること**、**登録直後はカゴに入れないこと**、
 * **「続けて登録」が名前だけ消してチップの選択を残すこと**、
 * **02b で作ったものが選択済みで現れること**。
 */
class RegisterViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel() = RegisterViewModel(
        itemRepository = ItemRepository(db.itemDao),
        categoryRepository = CategoryRepository(db.categoryDao),
        destinationRepository = DestinationRepository(db.destinationDao)
    )

    // ---- 登録 ----

    /** **登録直後の品目はカゴに入れない**（画面 02）。 */
    @Test
    fun 名前だけで登録できてカゴには入らない() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("アイテムA")

        viewModel.register()
        advanceUntilIdle()

        val stored = db.storedItems.single()
        assertEquals("アイテムA", stored.name)
        assertEquals(ItemStatus.NO_DEAL, stored.status)
        assertNull(stored.categoryId)
        assertNull(stored.destinationId)
    }

    @Test
    fun 選んだカテゴリと行き先が付く() = runTest {
        db.seed(categories = listOf(category(1)), destinations = listOf(destination(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeName("アイテムA")
        viewModel.selectCategory(1)
        viewModel.selectDestination(1)
        viewModel.register()
        advanceUntilIdle()

        val stored = db.storedItems.single()
        assertEquals(1, stored.categoryId)
        assertEquals(1, stored.destinationId)
    }

    /**
     * 同じチップをもう一度押すと外れる（画面定義書 §2）。
     * **カテゴリと行き先は別のコードなので両方見る。**
     */
    @Test
    fun 同じチップをもう一度押すと外れる() = runTest {
        db.seed(categories = listOf(category(1)), destinations = listOf(destination(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectCategory(1)
        viewModel.selectDestination(1)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedCategoryId)
        assertEquals(1, viewModel.uiState.value.selectedDestinationId)

        viewModel.selectCategory(1)
        viewModel.selectDestination(1)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertNull(viewModel.uiState.value.selectedDestinationId)
    }

    /** 保存できたときだけシートを閉じる合図を出す。 */
    @Test
    fun 登録できたら閉じる合図が出る() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("アイテムA")
        assertEquals(0, viewModel.closeRequests.value)

        viewModel.register()
        advanceUntilIdle()

        assertEquals(1, viewModel.closeRequests.value)
    }

    @Test
    fun 弾かれたら閉じずにエラーを出す() = runTest {
        db.seed(items = listOf(item(1, name = "アイテムA")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeName("アイテムA")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(0, viewModel.closeRequests.value)
        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.nameError)
        assertEquals(1, db.storedItems.size)
    }

    @Test
    fun 空の名前は弾かれる() = runTest {
        val viewModel = viewModel()

        viewModel.changeName("   ")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(NameError.BLANK, viewModel.uiState.value.nameError)
        assertEquals(0, db.storedItems.size)
    }

    // ---- 続けて登録 ----

    /** **名前だけ消して、チップの選択は残す**（画面 02）。閉じる合図も出さない。 */
    @Test
    fun 続けて登録は名前だけ消してチップを残す() = runTest {
        db.seed(categories = listOf(category(1)), destinations = listOf(destination(1)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.selectCategory(1)
        viewModel.selectDestination(1)

        viewModel.changeName("アイテムA")
        viewModel.registerAndContinue()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.name)
        assertEquals(1, viewModel.uiState.value.selectedCategoryId)
        assertEquals(1, viewModel.uiState.value.selectedDestinationId)
        assertEquals(0, viewModel.closeRequests.value)
        assertEquals(1, db.storedItems.size)
    }

    @Test
    fun 続けて登録を繰り返すと同じ組で増える() = runTest {
        db.seed(categories = listOf(category(1)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.selectCategory(1)

        viewModel.changeName("アイテムA")
        viewModel.registerAndContinue()
        advanceUntilIdle()
        viewModel.changeName("アイテムB")
        viewModel.registerAndContinue()
        advanceUntilIdle()

        assertEquals(listOf("アイテムA", "アイテムB"), db.storedItems.map { it.name })
        assertEquals(listOf(1, 1), db.storedItems.map { it.categoryId })
    }

    /**
     * **閉じるときに捨てるもの**（画面定義書 §2「保存されていない入力は破棄」）。
     *
     * ViewModel はシートより長生きするので、捨て漏らすと次に開いたときに残る。
     * 捨てる先が 6 つあるので、**1 つずつ落とす変異が捕まるように 6 つとも見る**。
     */
    @Test
    fun 閉じると入力もエラーもダイアログも捨てる() = runTest {
        db.seed(
            items = listOf(item(1, name = "アイテムA")),
            categories = listOf(category(1)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel()
        advanceUntilIdle()
        // 重複で弾かれた状態を作り、チップも 02b も開いておく
        viewModel.changeName("アイテムA")
        viewModel.selectCategory(1)
        viewModel.selectDestination(1)
        viewModel.register()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        advanceUntilIdle()

        viewModel.reset()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("", uiState.name)
        assertNull(uiState.nameError)
        assertNull(uiState.selectedCategoryId)
        assertNull(uiState.selectedDestinationId)
        assertNull(uiState.newNameDialog)
        assertEquals(0, viewModel.closeRequests.value)
    }

    /** 続けて登録でも、弾かれたら**名前を消さない**（直して押し直せるように）。 */
    @Test
    fun 続けて登録で弾かれたら名前は残る() = runTest {
        db.seed(items = listOf(item(1, name = "アイテムA")))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeName("アイテムA")
        viewModel.registerAndContinue()
        advanceUntilIdle()

        assertEquals("アイテムA", viewModel.uiState.value.name)
        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.nameError)
        assertEquals(1, db.storedItems.size)
    }

    // ---- 02b 新しいカテゴリ／行き先 ----

    /** **作ったものは選択済みで現れる**（画面 02b）。 */
    @Test
    fun 作ったカテゴリが選択済みになる() = runTest {
        val viewModel = viewModel()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        viewModel.changeNewName("カテゴリA")

        viewModel.createNewName()
        advanceUntilIdle()

        val created = db.storedCategories.single()
        assertEquals("カテゴリA", created.name)
        assertEquals(created.id, viewModel.uiState.value.selectedCategoryId)
        // 作れたらダイアログは閉じる
        assertNull(viewModel.uiState.value.newNameDialog)
    }

    @Test
    fun 作った行き先が選択済みになる() = runTest {
        val viewModel = viewModel()
        viewModel.showNewNameDialog(NewNameTarget.DESTINATION)
        viewModel.changeNewName("行き先A")

        viewModel.createNewName()
        advanceUntilIdle()

        val created = db.storedDestinations.single()
        assertEquals(created.id, viewModel.uiState.value.selectedDestinationId)
    }

    /** 並び順は末尾（データモデル定義書 §6）。 */
    @Test
    fun 作ったカテゴリは並びの末尾に付く() = runTest {
        db.seed(categories = listOf(category(1, sortOrder = 5)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        viewModel.changeNewName("カテゴリA")

        viewModel.createNewName()
        advanceUntilIdle()

        val created = db.storedCategories.single { it.name == "カテゴリA" }
        assertEquals(6, created.sortOrder)
        // id・sortOrder・既存 id がすべて別値なので、取り違えるとここで落ちる
        assertEquals(2, created.id)
        assertEquals(2, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun 作った行き先も並びの末尾に付いて選ばれる() = runTest {
        db.seed(destinations = listOf(destination(1, sortOrder = 5)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.DESTINATION)
        viewModel.changeNewName("行き先A")

        viewModel.createNewName()
        advanceUntilIdle()

        val created = db.storedDestinations.single { it.name == "行き先A" }
        assertEquals(6, created.sortOrder)
        assertEquals(2, created.id)
        assertEquals(2, viewModel.uiState.value.selectedDestinationId)
    }

    @Test
    fun 同じ名前のカテゴリは作れずダイアログが開いたまま() = runTest {
        db.seed(categories = listOf(category(1, name = "カテゴリA")))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        viewModel.changeNewName("カテゴリA")

        viewModel.createNewName()
        advanceUntilIdle()

        val dialog = assertNotNull(viewModel.uiState.value.newNameDialog)
        assertEquals(NameError.DUPLICATE, dialog.error)
        assertEquals(1, db.storedCategories.size)
        assertNull(viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun ダイアログを閉じると入力は捨てられる() = runTest {
        val viewModel = viewModel()
        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        viewModel.changeNewName("カテゴリA")

        viewModel.dismissNewNameDialog()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.newNameDialog)

        viewModel.showNewNameDialog(NewNameTarget.CATEGORY)
        advanceUntilIdle()

        assertEquals("", assertNotNull(viewModel.uiState.value.newNameDialog).name)
        assertEquals(0, db.storedCategories.size)
    }
}
