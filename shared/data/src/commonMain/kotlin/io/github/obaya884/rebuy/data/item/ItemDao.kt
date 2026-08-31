package io.github.obaya884.rebuy.data.item

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant

@Dao
interface ItemDao {
    /** 並び順は登録順＝ id 昇順（データモデル定義書 §4・§6）。 */
    @Query("SELECT * FROM items ORDER BY id")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): Item

    /**
     * 名前が重複していれば例外で止まる（`ABORT`）。**3 テーブルとも同じ理由でこれを選ぶ。**
     *
     * `REPLACE` は衝突した行を消してから入れ直すので、同名を入れると既存の行が失われる。
     * 品目なら最終購入日ごと、カテゴリーや行き先ならそれを指していた品目が
     * 外部キーの `SET_NULL` で外れる。名前の UNIQUE インデックスは
     * 「最後の砦」（データモデル定義書 §5）なので、置き換えずに止める。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: Item): Long

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET name = :newName, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemName(itemId: Int, newName: String, updatedAt: Instant = Clock.System.now())

    @Query("UPDATE items SET status = :newStatus, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemStatus(
        itemId: Int,
        newStatus: ItemStatus,
        updatedAt: Instant = Clock.System.now()
    )

    @Query("UPDATE items SET status = :newStatus, updatedAt = :updatedAt, lastBoughtAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemStatusWithLastBoughtAt(
        itemId: Int,
        newStatus: ItemStatus,
        updatedAt: Instant = Clock.System.now()
    )

    @Query("UPDATE items SET categoryId = :newCategoryId, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemCategoryId(
        itemId: Int,
        newCategoryId: Int?,
        updatedAt: Instant = Clock.System.now()
    )

    @Query("UPDATE items SET destinationId = :newDestinationId, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemDestinationId(
        itemId: Int,
        newDestinationId: Int?,
        updatedAt: Instant = Clock.System.now()
    )

    @Transaction
    @Query("SELECT * FROM items ORDER BY id")
    fun getAllItemsWithCategory(): Flow<List<ItemWithCategory>>
}

