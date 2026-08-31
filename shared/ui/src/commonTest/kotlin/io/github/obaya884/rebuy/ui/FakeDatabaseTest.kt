package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * [FakeDatabase] が Room に寄せている挙動を固定する。
 *
 * この fake は ViewModel テスト 90 件の土台なので、**寄せ方が崩れると 90 件が
 * 一斉に嘘をつく**。それでいて崩れたことは 90 件のどれからも見えない
 * （`++lastItemId` を `items.size + 1` に変えても全部緑のままだった）。
 * ここで直接押さえる。
 */
class FakeDatabaseTest {

    private val db = FakeDatabase()

    @Test
    fun 削除した品目のidは再利用されない() = runTest {
        db.itemDao.insertItem(item(id = 0, name = "1 番目"))
        db.itemDao.insertItem(item(id = 0, name = "2 番目"))
        val second = db.itemDao.getAllItems().first().single { it.name == "2 番目" }

        db.itemDao.deleteItem(second)
        db.itemDao.insertItem(item(id = 0, name = "3 番目"))

        val third = db.itemDao.getAllItems().first().single { it.name == "3 番目" }
        assertEquals(3, third.id)
    }

    @Test
    fun 削除したカテゴリーのidは再利用されない() = runTest {
        db.categoryDao.insert(category(id = 0, name = "1 番目"))
        db.categoryDao.insert(category(id = 0, name = "2 番目"))
        val second = db.categoryDao.getAllCategories().first().single { it.name == "2 番目" }

        db.categoryDao.delete(second)
        db.categoryDao.insert(category(id = 0, name = "3 番目"))

        val third = db.categoryDao.getAllCategories().first().single { it.name == "3 番目" }
        assertEquals(3, third.id)
    }

    /** 本番では外部キー制約が `SQLiteConstraintException` を投げる経路。 */
    @Test
    fun 存在しないカテゴリーを指す品目は入らない() = runTest {
        assertFailsWith<IllegalStateException> {
            db.itemDao.insertItem(item(id = 0, categoryId = 999))
        }
    }

    @Test
    fun 存在しないカテゴリーへ付け替えられない() = runTest {
        db.itemDao.insertItem(item(id = 0))
        val inserted = db.itemDao.getAllItems().first().single()

        assertFailsWith<IllegalStateException> {
            db.itemDao.updateItemCategoryId(inserted.id, 999)
        }
    }

    /**
     * `OnConflictStrategy.ABORT`（理由は `ItemDao.insertItem`）。
     * ViewModel は id = 0 でしか挿入しないので、ここでしか通らない。
     */
    @Test
    fun 同じidで挿入すると失敗する() = runTest {
        db.seed(items = listOf(item(id = 1)))

        assertFailsWith<IllegalStateException> {
            db.itemDao.insertItem(item(id = 1, name = "別の名前"))
        }

        assertEquals("アイテム1", db.itemDao.getAllItems().first().single().name)
    }

    @Test
    fun 同じ名前の品目は入らない() = runTest {
        db.itemDao.insertItem(item(id = 0, name = "アイテム1"))

        assertFailsWith<IllegalStateException> {
            db.itemDao.insertItem(item(id = 0, name = "アイテム1"))
        }
    }

    @Test
    fun 同じ名前へは改名できない() = runTest {
        db.seed(items = listOf(item(id = 1), item(id = 2)))

        assertFailsWith<IllegalStateException> {
            db.itemDao.updateItemName(2, "アイテム1")
        }
        // 自分自身と同じ名前は通る（UNIQUE インデックスは自分の行とはぶつからない）。
        // 名前は変わらないので、更新が走ったことは updatedAt で見る
        db.itemDao.updateItemName(2, "アイテム2")
        assertEquals("アイテム2", db.storedItem(2).name)
        assertNotEquals(CREATED_AT, db.storedItem(2).updatedAt)
    }

    @Test
    fun 同じ名前の行き先は入らない() = runTest {
        db.destinationDao.insert(destination(id = 0, name = "1 番目"))

        assertFailsWith<IllegalStateException> {
            db.destinationDao.insert(destination(id = 0, name = "1 番目"))
        }
    }

    /** 行き先を消しても品目は残り、「どこでも買えるもの」に戻る。 */
    @Test
    fun 行き先を消すと品目の行き先がnullになる() = runTest {
        db.seed(
            items = listOf(item(id = 1, destinationId = 1)),
            destinations = listOf(destination(id = 1))
        )

        db.destinationDao.delete(destination(id = 1))

        assertEquals(1, db.storedItems.size)
        assertNull(db.storedItem(1).destinationId)
    }

    @Test
    fun 存在しない行き先を指す品目は入らない() = runTest {
        assertFailsWith<IllegalStateException> {
            db.itemDao.insertItem(item(id = 0, destinationId = 999))
        }
    }

    @Test
    fun 存在しない行き先へ付け替えられない() = runTest {
        db.seed(items = listOf(item(id = 1)))

        assertFailsWith<IllegalStateException> {
            db.itemDao.updateItemDestinationId(1, 999)
        }
    }

    /** 一覧の並びは品目が id 昇順、カテゴリーと行き先が sortOrder 昇順。 */
    @Test
    fun 一覧は並び順で返る() = runTest {
        db.seed(
            items = listOf(item(id = 3), item(id = 1)),
            categories = listOf(
                category(id = 1, sortOrder = 3),
                category(id = 2, sortOrder = 1),
                category(id = 3, sortOrder = 2)
            ),
            destinations = listOf(
                destination(id = 1, sortOrder = 2),
                destination(id = 2, sortOrder = 1)
            )
        )

        // 後から入れたものも id の位置に割り込む（seed だけでなく insert 経路も見る）
        db.itemDao.insertItem(item(id = 2))

        assertEquals(listOf(1, 2, 3), db.itemDao.getAllItems().first().map { it.id })
        assertEquals(listOf(2, 3, 1), db.categoryDao.getAllCategories().first().map { it.id })
        assertEquals(listOf(2, 1), db.destinationDao.getAllDestinations().first().map { it.id })
    }

    /** 並び順が同値なら登録順（id 昇順）で割る。 */
    @Test
    fun 並び順が同値なら登録順で割る() = runTest {
        db.seed(
            categories = listOf(
                category(id = 2, sortOrder = 1),
                category(id = 1, sortOrder = 1)
            )
        )

        assertEquals(listOf(1, 2), db.categoryDao.getAllCategories().first().map { it.id })
    }

    @Test
    fun 並び順の最大値を返す() = runTest {
        assertEquals(0, db.categoryDao.maxSortOrder())
        assertEquals(0, db.destinationDao.maxSortOrder())

        db.seed(categories = listOf(category(id = 1, sortOrder = 7)))

        assertEquals(7, db.categoryDao.maxSortOrder())
    }
}
