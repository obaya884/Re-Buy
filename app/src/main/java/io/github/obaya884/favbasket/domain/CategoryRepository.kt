package io.github.obaya884.favbasket.domain

import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.category.CategoryDao
import kotlinx.coroutines.flow.Flow
import java.util.Date

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun updateName(id: Int, newName: String) {
        categoryDao.updateCategoryName(id, newName, Date())
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

}
