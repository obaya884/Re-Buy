package io.github.obaya884.rebuy.data

import androidx.room.Room
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    private fun destination(name: String, sortOrder: Int = 1) = Destination(
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun category(name: String, sortOrder: Int = 1) = Category(
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun item(
        name: String,
        status: ItemStatus = ItemStatus.NO_DEAL,
        categoryId: Int? = null,
        destinationId: Int? = null,
        lastBoughtAt: Instant? = null
    ) = Item(
        name = name,
        status = status,
        categoryId = categoryId,
        destinationId = destinationId,
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

        val categoryId = categoryDao.insert(category(name = "カテゴリー1")).toInt()
        itemDao.insertItem(item(name = "アイテム1", categoryId = categoryId))
        itemDao.insertItem(item(name = "アイテム2"))

        val stored = itemDao.getAllItemsWithCategory().first().associateBy { it.item.name }
        assertEquals(2, stored.size)
        assertEquals("カテゴリー1", stored.getValue("アイテム1").category?.name)
        // カテゴリーが無い側は null で返る。@Relation の非マッチ経路
        assertNull(stored.getValue("アイテム2").category)
    }

    /**
     * 並び順が本物の SQL で効くこと（`ORDER BY sortOrder, id` と
     * `COALESCE(MAX(sortOrder), 0)`）。1 件も無いときに 0 を返すのは、
     * Repository が末尾の採番に使う（データモデル定義書 §6）。
     *
     * **同値のタイブレーク（`, id`）はここでは守れない。** 落として実測しても素通りする——
     * SQLite が返す順が rowid 昇順＝ id 昇順とたまたま一致するため。**観測できる網は
     * `FakeDatabaseTest` の「並び順が同値なら登録順で割る」だけ**で、SQL 側の `, id` は
     * 順序を実装依存にしないための明示的な契約として書いてある。
     */
    @Test
    fun 行き先は並び順で返る() = runBlocking {
        val dao = database.destinationDao()
        assertEquals(0, dao.maxSortOrder())

        dao.insert(destination(name = "行き先1", sortOrder = 2))
        dao.insert(destination(name = "行き先2", sortOrder = 1))
        dao.insert(destination(name = "行き先3", sortOrder = 1))

        assertEquals(
            listOf("行き先2", "行き先3", "行き先1"),
            dao.getAllDestinations().first().map { it.name }
        )
        assertEquals(2, dao.maxSortOrder())
    }

    /** カテゴリーは行き先と同型だが、`ORDER BY` は DAO ごとに書くので別に見る。 */
    @Test
    fun カテゴリーは並び順で返る() = runBlocking {
        val dao = database.categoryDao()
        assertEquals(0, dao.maxSortOrder())

        dao.insert(category(name = "カテゴリー1", sortOrder = 2))
        dao.insert(category(name = "カテゴリー2", sortOrder = 1))

        assertEquals(
            listOf("カテゴリー2", "カテゴリー1"),
            dao.getAllCategories().first().map { it.name }
        )
        assertEquals(2, dao.maxSortOrder())
    }

    /**
     * 名前の UNIQUE インデックスと `ABORT`（理由は `ItemDao.insertItem`）が本物の SQL で
     * 効くこと。`onConflict` は生成される DAO のコードにしか出ず、**スキーマ JSON にも
     * `RoomMigrationTest` にも現れない**ので、3 テーブルとも実物で見る。
     */
    @Test
    fun 同じ名前の行き先は入らない() = runBlocking {
        val dao = database.destinationDao()
        dao.insert(destination(name = "行き先1"))

        assertFailsWith<SQLiteException> {
            dao.insert(destination(name = "行き先1"))
        }
        assertEquals(1, dao.getAllDestinations().first().size)
    }

    @Test
    fun 同じ名前のカテゴリーは入らない() = runBlocking {
        val dao = database.categoryDao()
        dao.insert(category(name = "カテゴリー1"))

        assertFailsWith<SQLiteException> {
            dao.insert(category(name = "カテゴリー1"))
        }
        assertEquals(1, dao.getAllCategories().first().size)
    }

    /**
     * 品目では最終購入日まで見る。**`REPLACE` だと 2 回目の登録が既存の行を消して
     * 最終購入日が失われる**——常駐と最終購入日がこのアプリの芯なので、ここがいちばん痛い。
     */
    @Test
    fun 同じ名前の品目は入らず最終購入日も残る() = runBlocking {
        val dao = database.itemDao()
        val boughtAt = Instant.parse("2026-01-02T03:04:05Z")
        dao.insertItem(item(name = "アイテム1", lastBoughtAt = boughtAt))

        assertFailsWith<SQLiteException> {
            dao.insertItem(item(name = "アイテム1"))
        }

        val stored = dao.getAllItems().first().single()
        assertEquals(boughtAt, stored.lastBoughtAt)
    }

    /**
     * 同名の存在確認が本物の SQL で効くこと。**`AND id != :exceptId` を落とすと
     * 「自分自身と同じ名前への改名」が重複として弾かれる**（画面では保存が通らなくなる）。
     */
    @Test
    fun 品目の同名の存在確認は自分自身を除く() = runBlocking {
        val dao = database.itemDao()
        val id = dao.insertItem(item(name = "アイテム1")).toInt()
        dao.insertItem(item(name = "アイテム2"))

        assertFalse(dao.existsName("アイテム1", exceptId = id))
        assertTrue(dao.existsName("アイテム2", exceptId = id))
        // 新規（exceptId = 0）はどの行も除かない
        assertTrue(dao.existsName("アイテム1", exceptId = 0))
    }

    /** 3 つの DAO が同じ SQL を別々に持つので、1 か所落ちても他は緑になる。 */
    @Test
    fun カテゴリーの同名の存在確認は自分自身を除く() = runBlocking {
        val dao = database.categoryDao()
        val id = dao.insert(category(name = "カテゴリー1")).toInt()

        assertFalse(dao.existsName("カテゴリー1", exceptId = id))
        assertTrue(dao.existsName("カテゴリー1", exceptId = 0))
    }

    @Test
    fun 行き先の同名の存在確認は自分自身を除く() = runBlocking {
        val dao = database.destinationDao()
        val id = dao.insert(destination(name = "行き先1")).toInt()

        assertFalse(dao.existsName("行き先1", exceptId = id))
        assertTrue(dao.existsName("行き先1", exceptId = 0))
    }

    /**
     * 名前・カテゴリー・行き先をまとめて書けること（画面 06 の「保存」）。
     *
     * **`FakeDatabase` は既定実装をそのまま継ぐので、Room が生成する `@Transaction` の
     * override はここでしか通らない。**
     */
    @Test
    fun まとめて書くと3つとも変わる() = runBlocking {
        val itemDao = database.itemDao()
        val categoryId = database.categoryDao().insert(category(name = "カテゴリー1")).toInt()
        val destinationId = database.destinationDao().insert(destination(name = "行き先1")).toInt()
        val id = itemDao.insertItem(item(name = "アイテム1")).toInt()

        itemDao.updateItemNameAndRelations(id, "アイテムA", categoryId, destinationId)

        val stored = itemDao.getAllItems().first().single()
        assertEquals("アイテムA", stored.name)
        assertEquals(categoryId, stored.categoryId)
        assertEquals(destinationId, stored.destinationId)
    }

    /**
     * **途中で失敗したら名前も巻き戻る。** `@Transaction` を外すと、名前だけ変わって
     * カテゴリーが変わらない中途半端な状態が残る（外して実測）。
     */
    @Test
    fun まとめ書きの途中で失敗すると名前も巻き戻る() = runBlocking {
        val itemDao = database.itemDao()
        val id = itemDao.insertItem(item(name = "アイテム1")).toInt()

        assertFailsWith<SQLiteException> {
            // 存在しないカテゴリー。外部キーで弾かれる
            itemDao.updateItemNameAndRelations(id, "アイテムA", newCategoryId = 999, null)
        }

        assertEquals("アイテム1", itemDao.getAllItems().first().single().name)
    }

    @Test
    fun 品目をidで消せる() = runBlocking {
        val dao = database.itemDao()
        val id = dao.insertItem(item(name = "アイテム1")).toInt()
        dao.insertItem(item(name = "アイテム2"))

        dao.deleteItemById(id)

        assertEquals(listOf("アイテム2"), dao.getAllItems().first().map { it.name })
    }

    /**
     * 行き先を消すと、それを指していた品目が「どこでも買えるもの」に戻ること
     * （外部キーの `SET_NULL`。データモデル定義書 §7）。
     *
     * **native の driver で外部キー制約が効いているかを見る唯一のテスト**でもある（T-49）。
     * 効いていなければ消えた行き先の id が品目に残り、どの買い物モードにも出てこなくなる。
     */
    @Test
    fun 行き先を消しても品目は残る() = runBlocking {
        val destinationDao = database.destinationDao()
        val itemDao = database.itemDao()
        val destinationId = destinationDao.insert(destination(name = "行き先1")).toInt()
        itemDao.insertItem(item(name = "アイテム1", destinationId = destinationId))

        destinationDao.delete(destinationDao.getAllDestinations().first().single())

        val stored = itemDao.getAllItems().first().single()
        assertEquals("アイテム1", stored.name)
        assertNull(stored.destinationId)
    }
}
