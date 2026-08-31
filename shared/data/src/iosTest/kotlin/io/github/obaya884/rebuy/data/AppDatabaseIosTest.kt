package io.github.obaya884.rebuy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
 * iOS で Room が本当に動くことを見る（T-35）。
 *
 * ここまで iOS 側は Converter の純粋関数しか通っておらず、**次の 3 つはシミュレータで
 * 起動するまで分からなかった**。
 *
 * 1. `AppDatabaseConstructor` の `actual` が KSP でこのターゲット向けに生成されていること
 * 2. `BundledSQLiteDriver` が Kotlin/Native で動くこと
 * 3. `@TypeConverters`（`Instant` と `ItemStatus`）が本物の SQLite を往復すること
 *
 * **`commonTest` には置けない。** `Room.inMemoryDatabaseBuilder` は native にはあるが
 * common には無い。Android 側の同じ守りは instrumented の `RoomMigrationTest` が持つ。
 *
 * **本番のパス（`NSDocumentDirectory`）は通らない。** in-memory なのは、テストバイナリが
 * アプリのサンドボックスではなくシミュレータ共有のデータ領域で動き、新品のシミュレータには
 * `data/Documents` が無いため（経緯は T-48）。ファイルを開く経路は Android の
 * `RoomMigrationTest` 側の話（T-34）。
 */
class AppDatabaseIosTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun 品目を入れて取り出せる() = runBlocking {
        val dao = database.itemDao()
        val boughtAt = Instant.parse("2026-01-02T03:04:05Z")

        dao.insertItem(
            Item(
                name = "アイテム1",
                status = ItemStatus.IN_SHOPPING_LIST,
                lastBoughtAt = boughtAt
            )
        )

        val stored = dao.getAllItems().first().single()
        assertEquals("アイテム1", stored.name)
        // ここが本題。ItemStatus と Instant のコンバータが SQLite を往復している
        assertEquals(ItemStatus.IN_SHOPPING_LIST, stored.status)
        assertEquals(boughtAt, stored.lastBoughtAt)
    }

    /**
     * `lastBoughtAt` が null のまま往復すること。
     *
     * Room が生成する null 分岐はコンバータの非 null シグネチャでは表現できないので、
     * 値がある場合とは別の経路を通る（T-25 の (b) と同じ観点の iOS 版）。
     */
    @Test
    fun 最終購入日が無い品目も往復する() = runBlocking {
        val dao = database.itemDao()

        dao.insertItem(Item(name = "アイテム2"))

        val stored = dao.getAllItems().first().single()
        assertEquals(ItemStatus.NO_DEAL, stored.status)
        assertNull(stored.lastBoughtAt)
    }
}
