package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * F-002 で足したデータモデルの規則を Repository の段で固定する
 * ——並び順の採番（データモデル定義書 §6）と、品目の行き先の付け替え。
 *
 * 画面はまだ無いので ViewModel を通せない。Repository を本物のまま使い、その下の DAO を
 * [FakeDatabase] に差し替える形は ViewModel テストと同じ。**この fake が
 * `:shared:ui` の `commonTest` にあるので、Repository のテストもここに置く**（T-52）。
 *
 * **カテゴリーと行き先は同型なので、規則ごとに片方で代表する。** 両方に同じ網を張らない
 * かわり、代表を消したときに気づけるよう、どちらを見ているかをテスト名に出す。
 */
class DataModelRepositoryTest {

    private val db = FakeDatabase()
    private val categoryRepository = CategoryRepository(db.categoryDao)
    private val destinationRepository = DestinationRepository(db.destinationDao)
    private val itemRepository = ItemRepository(db.itemDao)

    @Test
    fun 最初のカテゴリーの並び順は1() = runTest {
        categoryRepository.insert("カテゴリーA")

        assertEquals(1, db.storedCategories.single().sortOrder)
    }

    @Test
    fun 最初の行き先の並び順は1() = runTest {
        destinationRepository.insert("行き先A")

        assertEquals(1, db.storedDestinations.single().sortOrder)
    }

    @Test
    fun 新しいカテゴリーは末尾に付く() = runTest {
        db.seed(categories = listOf(category(id = 1, sortOrder = 7)))

        categoryRepository.insert("カテゴリーB")

        val added = db.storedCategories.single { it.name == "カテゴリーB" }
        assertEquals(8, added.sortOrder)
        assertEquals(listOf(1, added.id), categoryRepository.getAll().first().map { it.id })
    }

    @Test
    fun 新しい行き先は末尾に付く() = runTest {
        db.seed(destinations = listOf(destination(id = 1, sortOrder = 2)))

        destinationRepository.insert("行き先B")

        val added = db.storedDestinations.single { it.name == "行き先B" }
        assertEquals(3, added.sortOrder)
        assertEquals(listOf(1, added.id), destinationRepository.getAll().first().map { it.id })
    }

    /** 並び替えは sortOrder の書き換えで表す。一覧の順もそれに従う。 */
    @Test
    fun 行き先の並び順を変えると一覧の順も変わる() = runTest {
        db.seed(
            destinations = listOf(
                destination(id = 1, sortOrder = 1),
                destination(id = 2, sortOrder = 2)
            )
        )

        destinationRepository.updateSortOrder(id = 2, newSortOrder = 0)

        assertEquals(listOf(2, 1), destinationRepository.getAll().first().map { it.id })
    }

    @Test
    fun カテゴリーの並び順を変えると一覧の順も変わる() = runTest {
        db.seed(
            categories = listOf(
                category(id = 1, sortOrder = 1),
                category(id = 2, sortOrder = 2)
            )
        )

        categoryRepository.updateSortOrder(id = 2, newSortOrder = 0)

        assertEquals(listOf(2, 1), categoryRepository.getAll().first().map { it.id })
    }

    /**
     * **同じ状態への更新は no-op**（テスト戦略定義書 §3）。`PoolViewModel` は状態で分岐するので
     * この早期 return に到達せず、ここでしか見られない。`updatedAt` が動かないことで見る。
     */
    @Test
    fun 常駐の品目をカゴから出しても更新しない() = runTest {
        db.seed(items = listOf(item(id = 1, status = ItemStatus.NO_DEAL)))

        itemRepository.updateStatusAsNoDeal(db.storedItem(1))

        assertEquals(CREATED_AT, db.storedItem(1).updatedAt)
    }

    /** 04 の行タップは状態で分岐するので、この早期 return には UI から到達しない。 */
    @Test
    fun チェック済みの品目にチェックを付けても更新しない() = runTest {
        db.seed(items = listOf(item(id = 1, status = ItemStatus.CHECKED_IN_SHOPPING_LIST)))

        itemRepository.updateStatusAsCheckedInBasket(db.storedItem(1))

        assertEquals(CREATED_AT, db.storedItem(1).updatedAt)
    }

    @Test
    fun チェックの付いていない品目のチェックを外しても更新しない() = runTest {
        db.seed(items = listOf(item(id = 1, status = ItemStatus.IN_SHOPPING_LIST)))

        itemRepository.updateStatusAsInBasket(db.storedItem(1))

        assertEquals(CREATED_AT, db.storedItem(1).updatedAt)
    }

    @Test
    fun 品目の行き先を付け替えられる() = runTest {
        db.seed(items = listOf(item(id = 1)), destinations = listOf(destination(id = 1)))

        itemRepository.updateDestination(id = 1, newDestinationId = 1)
        assertEquals(1, db.storedItem(1).destinationId)

        // null は「どこでも買えるもの」へ戻すこと
        itemRepository.updateDestination(id = 1, newDestinationId = null)
        assertNull(db.storedItem(1).destinationId)
    }
}
