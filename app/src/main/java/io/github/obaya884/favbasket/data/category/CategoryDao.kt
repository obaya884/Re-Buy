package io.github.obaya884.favbasket.data.category

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Delete
    suspend fun delete(category: Category)


    @Query("UPDATE categories SET name = :newName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategoryName(id: Int, newName: String, updatedAt: Instant = Instant.now())
}
