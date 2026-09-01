package io.github.obaya884.rebuy.ui.screen.add_noticed

import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.FakeDatabase
import io.github.obaya884.rebuy.ui.ViewModelTestBase
import io.github.obaya884.rebuy.ui.destination
import io.github.obaya884.rebuy.ui.item
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 気づいたものを足すシート（画面 05）の中身と、足したときの書き込み。
 *
 * 見るのは 4 つ。**初期表示は未追加だけ**、**検索は単純な部分一致で追加済みも当たりに含む**、
 * **「他の行き先から」は検索中だけ**、そして**どの経路も 1 件で閉じる**こと。
 */
class AddNoticedViewModelTest : ViewModelTestBase() {

    private val db = FakeDatabase()

    private fun viewModel(destinationId: Int?) = AddNoticedViewModel(
        itemRepository = ItemRepository(db.itemDao),
        destinationRepository = DestinationRepository(db.destinationDao),
        destinationId = destinationId
    )

    private val inBasket = ItemStatus.IN_SHOPPING_LIST

    /** 行き先 1（今の店）・行き先 2（他の店）・どこでも買えるもの、それぞれ 1 件ずつ。 */
    private fun seedThreePlaces() = db.seed(
        items = listOf(
            item(1, destinationId = 1, name = "こめ"),
            item(2, destinationId = 2, name = "こむぎ"),
            item(3, name = "こおり")
        ),
        destinations = listOf(destination(1), destination(2, name = "行き先B"))
    )

    // ---- 初期表示（入力が空）----

    @Test
    fun 初期表示は今の行き先とどこでも買えるものだけ() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.hereItems.map { it.id })
        assertEquals(listOf(3), viewModel.uiState.value.anywhereItems.map { it.id })
    }

    /** 「他の行き先から」は検索中だけのセクション（画面 05）。 */
    @Test
    fun 初期表示に他の行き先の枠は出ない() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(emptyList(), viewModel.uiState.value.elsewhereRows)
    }

    /** 「未追加のものから選ぶ」なので、カゴに入っているものは出ない。 */
    @Test
    fun 初期表示に追加済みは出ない() = runTest {
        db.seed(
            items = listOf(
                item(1, destinationId = 1),
                item(2, status = inBasket, destinationId = 1),
                item(3, status = ItemStatus.CHECKED_IN_SHOPPING_LIST, destinationId = 1)
            ),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.hereItems.map { it.id })
    }

    /** 空のセクションは見出しごと出さない（画面 05）。 */
    @Test
    fun 未追加が1件も無ければ見出しも出さない() = runTest {
        db.seed(
            items = listOf(item(1, status = inBasket, destinationId = 1)),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnaddedSectionVisible)
    }

    /** 全件モードは行き先で仕分けない。区切りも使わない（画面 05）。 */
    @Test
    fun 全件モードは未追加の全品目が1群に出る() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = null)

        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.hereItems.map { it.id })
        assertEquals(emptyList(), viewModel.uiState.value.anywhereItems)
        assertEquals(emptyList(), viewModel.uiState.value.elsewhereRows)
    }

    // ---- 検索 ----

    @Test
    fun 検索は部分一致で当たりを行き先ごとに仕分ける() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("こ")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(listOf(1), uiState.hereItems.map { it.id })
        assertEquals(listOf(3), uiState.anywhereItems.map { it.id })
        assertEquals(listOf(2), uiState.elsewhereRows.map { it.item.id })
        // どの店のものかを添える（画面 05）
        assertEquals(listOf("行き先B"), uiState.elsewhereRows.map { it.destinationName })
    }

    @Test
    fun 当たらない語では何も出ない() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("さ")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnaddedSectionVisible)
        assertEquals(emptyList(), viewModel.uiState.value.elsewhereRows)
    }

    /** **追加済みも当たりに含める**（画面 05）。探して見つからないと入れ忘れと区別が付かない。 */
    @Test
    fun 検索では追加済みも当たりに含む() = runTest {
        db.seed(
            items = listOf(item(1, status = inBasket, destinationId = 1, name = "こめ")),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("こめ")
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.hereItems.map { it.id })
    }

    /** ひらがな・カタカナの同一視はしない（画面 05）。 */
    @Test
    fun かなとカナは同一視しない() = runTest {
        db.seed(
            items = listOf(item(1, destinationId = 1, name = "こめ")),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("コメ")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnaddedSectionVisible)
    }

    @Test
    fun 前後の空白は落として探す() = runTest {
        db.seed(
            items = listOf(item(1, destinationId = 1, name = "こめ")),
            destinations = listOf(destination(1))
        )
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("  こめ  ")
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.uiState.value.hereItems.map { it.id })
    }

    /** 「＋ この名前で登録する」は入力が空白のみでない間だけ出る（画面 05）。 */
    @Test
    fun 空白だけの入力では登録ボタンを出さない() = runTest {
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("   ")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canRegisterQuery)
    }

    @Test
    fun 何か打てば登録ボタンが出る() = runTest {
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("こめ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canRegisterQuery)
    }

    // ---- 足す ----

    @Test
    fun 今の行き先のものを足すとカゴに入って閉じる() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.add(db.storedItem(1))
        advanceUntilIdle()

        assertEquals(inBasket, db.storedItem(1).status)
        assertEquals(1, viewModel.closeRequest.value.count)
        // 今の一覧に現れるので通知は要らない
        assertNull(viewModel.closeRequest.value.addedElsewhere)
    }

    @Test
    fun どこでも買えるものを足しても通知は出さない() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.add(db.storedItem(3))
        advanceUntilIdle()

        assertEquals(inBasket, db.storedItem(3).status)
        assertNull(viewModel.closeRequest.value.addedElsewhere)
    }

    /** 他の行き先のものは**今の一覧に現れない**ので、足した先を文言で知らせる（画面 05）。 */
    @Test
    fun 他の行き先のものを足すと行き先名で知らせる() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.add(db.storedItem(2))
        advanceUntilIdle()

        assertEquals(inBasket, db.storedItem(2).status)
        assertEquals("行き先B", viewModel.closeRequest.value.addedElsewhere)
    }

    /** 全件モードには「他の行き先」が無いので、どれを足しても通知は出ない。 */
    @Test
    fun 全件モードでは通知を出さない() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.add(db.storedItem(2))
        advanceUntilIdle()

        assertNull(viewModel.closeRequest.value.addedElsewhere)
    }

    // ---- この名前で登録する ----

    @Test
    fun 検索語で登録すると今の行き先でカゴに入って閉じる() = runTest {
        db.seed(destinations = listOf(destination(1)))
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()

        viewModel.changeQuery("こめ")
        viewModel.registerQuery()
        advanceUntilIdle()

        val stored = db.storedItems.single()
        assertEquals("こめ", stored.name)
        assertEquals(inBasket, stored.status)
        assertEquals(1, stored.destinationId)
        // カテゴリは付けない（画面 05）
        assertNull(stored.categoryId)
        assertEquals(1, viewModel.closeRequest.value.count)
    }

    /** 全件モードは今の店を持たないので行き先なしで登録する（画面 05）。 */
    @Test
    fun 全件モードでは行き先なしで登録する() = runTest {
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.changeQuery("こめ")
        viewModel.registerQuery()
        advanceUntilIdle()

        assertNull(db.storedItems.single().destinationId)
    }

    /** 検証は §2 と同じ。**弾かれたら閉じない**。 */
    @Test
    fun 同じ名前があると弾かれて閉じない() = runTest {
        db.seed(items = listOf(item(1, name = "こめ")))
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.changeQuery("こめ")
        viewModel.registerQuery()
        advanceUntilIdle()

        assertEquals(NameError.DUPLICATE, viewModel.uiState.value.nameError)
        assertEquals(0, viewModel.closeRequest.value.count)
        assertEquals(1, db.storedItems.size)
    }

    @Test
    fun 前後の空白は落として登録する() = runTest {
        val viewModel = viewModel(destinationId = null)
        advanceUntilIdle()

        viewModel.changeQuery("  こめ  ")
        viewModel.registerQuery()
        advanceUntilIdle()

        assertEquals("こめ", db.storedItems.single().name)
    }

    // ---- 閉じたあと ----

    /**
     * **ViewModel はシートより長生きする**（`AddNoticedViewModel` の KDoc）。
     * 捨てないと 2 回目に開いた瞬間に閉じる合図の残りで閉じ、二度と開けなくなる。
     */
    @Test
    fun resetで打ちかけの検索語と閉じる合図を捨てる() = runTest {
        seedThreePlaces()
        val viewModel = viewModel(destinationId = 1)
        advanceUntilIdle()
        viewModel.changeQuery("こ")
        viewModel.add(db.storedItem(1))
        advanceUntilIdle()

        viewModel.reset()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.query)
        assertNull(viewModel.uiState.value.nameError)
        assertEquals(CloseRequest(), viewModel.closeRequest.value)
    }
}
