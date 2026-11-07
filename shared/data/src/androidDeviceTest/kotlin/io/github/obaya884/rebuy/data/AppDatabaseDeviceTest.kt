package io.github.obaya884.rebuy.data

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android の本物の SQLite で、日時の保存形式と外部キーを見る。iOS 側は `AppDatabaseIosTest`。
 *
 * **保存形式は Room を通さずに読む。** 変換を通すと、形式が変わっても往復は揃うので緑のままになる。
 * スキーマ JSON はビルドのたびに生成し直されるので、こちらも形式の変更を止めない。
 *
 * **外部キーは接続の設定に依存する**ので、本番と同じ `applyAppDatabaseOptions()` で開く。
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseDeviceTest {

    private val database = Room
        .inMemoryDatabaseBuilder<AppDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        .applyAppDatabaseOptions(queryContext = Dispatchers.IO)
        .build()

    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun category() = Category(
        name = "カテゴリー1",
        sortOrder = 1,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun destination() = Destination(
        name = "行き先1",
        sortOrder = 1,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun item(categoryId: Int? = null, destinationId: Int? = null) = Item(
        name = "アイテム1",
        categoryId = categoryId,
        destinationId = destinationId,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    /** `lastBoughtAt` の `typeof()` と、整数として読んだ値。NULL の列は 0 になる。 */
    private suspend fun storedLastBoughtAt(): Pair<String, Long> =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT typeof(lastBoughtAt), lastBoughtAt FROM items") { row ->
                assertTrue(row.step())
                row.getText(0) to row.getLong(1)
            }
        }

    @After
    fun tearDown() {
        database.close()
    }

    /** 本番が非 null の最終購入日を書くのは、買い物の終了の `UPDATE` だけ。 */
    @Test
    fun 日時はエポックミリ秒の整数で保存され値のまま戻る() = runBlocking {
        val itemId = database.itemDao().insertItem(item()).toInt()
        val boughtAt = Instant.parse("2026-01-02T03:04:05.678Z")

        database.itemDao().updateItemStatusWithLastBoughtAt(
            itemId = itemId,
            newStatus = ItemStatus.NO_DEAL,
            updatedAt = boughtAt,
        )

        assertEquals("integer" to boughtAt.toEpochMilliseconds(), storedLastBoughtAt())
        assertEquals(boughtAt, database.itemDao().getAllItems().first().single().lastBoughtAt)
    }

    @Test
    fun 最終購入日が無い品目はNULLで保存され値のまま戻る() = runBlocking {
        database.itemDao().insertItem(item())

        assertEquals("null", storedLastBoughtAt().first)
        assertNull(database.itemDao().getAllItems().first().single().lastBoughtAt)
    }

    @Test
    fun 存在しないカテゴリーの品目は入らない() = runBlocking {
        assertThrows(SQLiteException::class.java) {
            runBlocking { database.itemDao().insertItem(item(categoryId = 999)) }
        }

        assertTrue(database.itemDao().getAllItems().first().isEmpty())
    }

    @Test
    fun 存在しない行き先の品目は入らない() = runBlocking {
        assertThrows(SQLiteException::class.java) {
            runBlocking { database.itemDao().insertItem(item(destinationId = 999)) }
        }

        assertTrue(database.itemDao().getAllItems().first().isEmpty())
    }

    @Test
    fun カテゴリーを消しても品目は残りカテゴリーなしになる() = runBlocking {
        val categoryId = database.categoryDao().insert(category()).toInt()
        database.itemDao().insertItem(item(categoryId = categoryId))

        database.categoryDao().deleteById(categoryId)

        assertNull(database.itemDao().getAllItems().first().single().categoryId)
    }

    @Test
    fun 行き先を消しても品目は残り行き先なしになる() = runBlocking {
        val destinationId = database.destinationDao().insert(destination()).toInt()
        database.itemDao().insertItem(item(destinationId = destinationId))

        database.destinationDao().deleteById(destinationId)

        assertNull(database.itemDao().getAllItems().first().single().destinationId)
    }
}
