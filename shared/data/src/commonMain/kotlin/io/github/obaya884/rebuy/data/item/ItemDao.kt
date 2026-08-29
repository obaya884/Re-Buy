package io.github.obaya884.rebuy.data.item

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): Item

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Transaction
    @Query("SELECT * FROM items")
    fun getAllItemsWithCategory(): Flow<List<ItemWithCategory>>
}

