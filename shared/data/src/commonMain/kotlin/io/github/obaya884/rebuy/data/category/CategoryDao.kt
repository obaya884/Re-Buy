package io.github.obaya884.rebuy.data.category

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant

@Dao
interface CategoryDao {
    /** 並び順は sortOrder 昇順。同値は登録順で割る（データモデル定義書 §6）。 */
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun getAllCategories(): Flow<List<Category>>

    /** 名前が重複していれば例外で止まる（`ABORT`。理由は `ItemDao.insertItem`）。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    /** 同じ名前が他の行にあるか（使い方は `NameValidation.kt`）。 */
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name AND id != :exceptId)")
    suspend fun existsName(name: String, exceptId: Int): Boolean

    @Delete
    suspend fun delete(category: Category)

    @Query("UPDATE categories SET name = :newName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategoryName(id: Int, newName: String, updatedAt: Instant = Clock.System.now())

    @Query("UPDATE categories SET sortOrder = :newSortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategorySortOrder(
        id: Int,
        newSortOrder: Int,
        updatedAt: Instant = Clock.System.now()
    )

    /** 末尾に足すための現在の最大値。1 件も無ければ 0。 */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxSortOrder(): Int
}
