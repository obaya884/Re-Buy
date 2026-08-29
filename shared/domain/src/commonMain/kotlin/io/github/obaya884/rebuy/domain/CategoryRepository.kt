package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun updateName(id: Int, newName: String) {
        categoryDao.updateCategoryName(id, newName)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

}
