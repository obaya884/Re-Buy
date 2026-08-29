package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
            db.itemDao.insertItem(item(id = 0, name = "みかん", categoryId = 999))
        }
    }

    @Test
    fun 存在しないカテゴリーへ付け替えられない() = runTest {
        db.itemDao.insertItem(item(id = 0, name = "みかん"))
        val inserted = db.itemDao.getAllItems().first().single()

        assertFailsWith<IllegalStateException> {
            db.itemDao.updateItemCategoryId(inserted.id, 999)
        }
    }

    /** `OnConflictStrategy.REPLACE`。ViewModel は id = 0 でしか挿入しないので、ここでしか通らない。 */
    @Test
    fun 同じidで挿入すると置き換わる() = runTest {
        db.seed(items = listOf(item(id = 1, name = "みかん")))

        db.itemDao.insertItem(item(id = 1, name = "りんご"))

        val items = db.itemDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("りんご", items.single().name)
    }
}
