package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAllCategories()

    /**
     * 名前を検証してから、並びの末尾に置く（データモデル定義書 §5・§6）。
     * 保存するのはトリム後の名前。
     */
    suspend fun insert(name: String): SaveResult =
        saveWithValidatedName(name, exceptId = NEW_RECORD_ID, categoryDao::existsName) { normalized ->
            categoryDao.insert(
                Category(name = normalized, sortOrder = categoryDao.maxSortOrder() + 1)
            ).toInt()
        }

    suspend fun updateName(id: Int, newName: String): SaveResult =
        saveWithValidatedName(newName, exceptId = id, categoryDao::existsName) { normalized ->
            categoryDao.updateCategoryName(id, normalized)
            id
        }

    suspend fun updateSortOrder(id: Int, newSortOrder: Int) {
        categoryDao.updateCategorySortOrder(id, newSortOrder)
    }

    /** 紐づく品目は消えず、外部キーの `SET_NULL` で「なし」に戻る。 */
    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }
}
