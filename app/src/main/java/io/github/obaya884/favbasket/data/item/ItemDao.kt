package io.github.obaya884.favbasket.data.item

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

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
    suspend fun updateItemName(itemId: Int, newName: String, updatedAt: Date)

    @Query("UPDATE items SET status = :newStatus, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: Int, newStatus: ItemStatus, updatedAt: Date)

    @Query("UPDATE items SET categoryId = :newCategoryId, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemCategoryId(itemId: Int, newCategoryId: Int?, updatedAt: Date)

}

