package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAllCategories()

    /** 新しいカテゴリーは並びの末尾に置く（データモデル定義書 §6）。 */
    suspend fun insert(name: String) {
        categoryDao.insert(Category(name = name, sortOrder = categoryDao.maxSortOrder() + 1))
    }

    suspend fun updateName(id: Int, newName: String) {
        categoryDao.updateCategoryName(id, newName)
    }

    suspend fun updateSortOrder(id: Int, newSortOrder: Int) {
        categoryDao.updateCategorySortOrder(id, newSortOrder)
    }

    /** 紐づく品目は消えず、外部キーの `SET_NULL` で「なし」に戻る。 */
    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }
}
