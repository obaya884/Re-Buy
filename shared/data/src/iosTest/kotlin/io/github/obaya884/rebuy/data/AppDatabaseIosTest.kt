package io.github.obaya884.rebuy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * iOS で Room が動くことを見る（T-35）。
 *
 * ここまで iOS 側は Converter の純粋関数しか通っておらず、**次の 3 つはシミュレータで
 * 起動するまで分からなかった**。
 *
 * 1. `AppDatabaseConstructor` の `actual` が KSP でこのターゲット向けに生成されていること
 * 2. `BundledSQLiteDriver` が Kotlin/Native で動くこと
 * 3. `@TypeConverters`（`Instant` と `ItemStatus`）が本物の SQLite を往復すること
 *
 * **`commonTest` には置けない。** `Room.inMemoryDatabaseBuilder` は native にしか無い。
 * Android 側の同じ守りは instrumented の `RoomMigrationTest` が持つ。
 *
 * ### この網が見ないもの
 *
 * **本番の配線は見ていない。** ここで組む DB は `DataModule.ios.kt` の設定の**複製**なので、
 * 向こうの `setDriver` や `databaseFilePath()` を壊しても全件緑のまま（変異で実測）。
 * 守っているのは「native で Room と `BundledSQLiteDriver` が動くこと」まで。
 *
 * **本番のパス（`NSDocumentDirectory`）も通らない。** テストバイナリはアプリの
 * サンドボックスではなくシミュレータ共有のデータ領域で動くので、実ファイルを開く経路は
 * 実物のアプリを起動する層でしか見られない（詳細は T-46）。
 */
class AppDatabaseIosTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    /** 生成時刻を固定して、`Instant` の往復を値で確かめられるようにする。 */
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun item(
        name: String,
        status: ItemStatus = ItemStatus.NO_DEAL,
        categoryId: Int? = null,
        lastBoughtAt: Instant? = null
    ) = Item(
        name = name,
        status = status,
        categoryId = categoryId,
        lastBoughtAt = lastBoughtAt,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun 品目を入れて取り出せる() = runBlocking {
        val dao = database.itemDao()
        val boughtAt = Instant.parse("2026-01-02T03:04:05Z")

        val rowId = dao.insertItem(
            item(name = "アイテム1", status = ItemStatus.IN_SHOPPING_LIST, lastBoughtAt = boughtAt)
        )

        val stored = dao.getAllItems().first().single()
        assertEquals(rowId.toInt(), stored.id)
        assertEquals("アイテム1", stored.name)
        // ここが本題。ItemStatus と Instant のコンバータが SQLite を往復している
        assertEquals(ItemStatus.IN_SHOPPING_LIST, stored.status)
        assertEquals(boughtAt, stored.lastBoughtAt)
        assertEquals(createdAt, stored.createdAt)
        assertEquals(createdAt, stored.updatedAt)
    }

    /**
     * `lastBoughtAt` が null のまま往復すること。
     *
     * Room が生成する null 分岐はコンバータの非 null シグネチャでは表現できないので、
     * 値がある場合とは別の経路を通る（T-25 の (b) と同じ観点の iOS 版）。
     * **`createdAt` / `updatedAt` は非 null なので、この件でも `Instant` の変換自体は通る。**
     */
    @Test
    fun 最終購入日が無い品目も往復する() = runBlocking {
        val dao = database.itemDao()

        dao.insertItem(item(name = "アイテム2"))

        val stored = dao.getAllItems().first().single()
        assertEquals(ItemStatus.NO_DEAL, stored.status)
        assertNull(stored.lastBoughtAt)
        assertEquals(createdAt, stored.createdAt)
    }

    /**
     * **アプリが実際に読むのはこちら。** 3 つの ViewModel はどれも
     * `getAllItemsWithCategory()` を通り、`getAllItems()` を呼ぶ経路はどこからも使われていない。
     *
     * `@Transaction` ＋ `@Relation` は Room が別経路（親クエリと関連の一括引き）を生成するので、
     * 上の 2 件とは違うコードが動く。**`categoryDao()` の生成実装を通す唯一のテスト**でもある。
     */
    @Test
    fun カテゴリー付きの品目を関連ごと取り出せる() = runBlocking {
        val itemDao = database.itemDao()
        val categoryDao = database.categoryDao()

        val categoryId = categoryDao.insert(
            Category(name = "カテゴリー1", createdAt = createdAt, updatedAt = createdAt)
        ).toInt()
        itemDao.insertItem(item(name = "アイテム1", categoryId = categoryId))
        itemDao.insertItem(item(name = "アイテム2"))

        val stored = itemDao.getAllItemsWithCategory().first().associateBy { it.item.name }
        assertEquals(2, stored.size)
        assertEquals("カテゴリー1", stored.getValue("アイテム1").category?.name)
        // カテゴリーが無い側は null で返る。@Relation の非マッチ経路
        assertNull(stored.getValue("アイテム2").category)
    }
}
