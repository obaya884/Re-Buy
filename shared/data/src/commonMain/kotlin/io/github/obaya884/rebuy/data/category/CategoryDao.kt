package io.github.obaya884.rebuy.data.category

import androidx.room.*
import io.github.obaya884.rebuy.data.SortOrderRow
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

    /** id で消す。**開いている行を消す**ときは、打ちかけの名前を持ち回らずに済む。 */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE categories SET name = :newName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategoryName(id: Int, newName: String, updatedAt: Instant = Clock.System.now())

    @Query("UPDATE categories SET sortOrder = :newSortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategorySortOrder(
        id: Int,
        newSortOrder: Int,
        updatedAt: Instant = Clock.System.now()
    )

    /**
     * 並び替えの保存。**渡された順に 1..n を振り直す**（画面 09 の「離した時点で保存」）。
     *
     * 1 件ずつ書くと、途中で `sortOrder` が重なった一覧が Flow に流れて**別の順で一瞬描かれる**
     * （`ItemDao.updateItemNameAndRelations` と同じ理由）。`@Transaction` で 1 回の変更にする。
     *
     * 値が変わらない行は書かない——`updatedAt` を動かさないため（データモデル定義書 §3 の
     * 「同じ状態への更新は no-op」と同じ流儀）。
     *
     * **`FakeDatabase` はこの既定実装をそのまま継ぐ**ので、`@Transaction` が本当に効くことは
     * `:shared:data` の iosTest でしか見られない。
     */
    @Transaction
    suspend fun updateSortOrders(orderedIds: List<Int>) {
        val current = currentSortOrders().associate { it.id to it.sortOrder }
        orderedIds.forEachIndexed { index, id ->
            val newSortOrder = index + 1
            if (current[id] != newSortOrder) {
                updateCategorySortOrder(id = id, newSortOrder = newSortOrder)
            }
        }
    }

    /** いまの並び順。**書かなくてよい行を見分ける**ために読む。 */
    @Query("SELECT id, sortOrder FROM categories")
    suspend fun currentSortOrders(): List<SortOrderRow>

    /** 末尾に足すための現在の最大値。1 件も無ければ 0。 */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxSortOrder(): Int
}
