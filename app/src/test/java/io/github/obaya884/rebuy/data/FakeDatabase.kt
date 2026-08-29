package io.github.obaya884.rebuy.data

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Room の代わりに使うインメモリの実体。2 つの DAO が同じものを見る。
 *
 * ViewModel のテストは Repository を本物のまま使い、その下の DAO だけを差し替える。
 * ステータス遷移の早期 return など Repository のルールも一緒に網に入れるため。
 */
class FakeDatabase {
    val items = MutableStateFlow<List<Item>>(emptyList())
    val categories = MutableStateFlow<List<Category>>(emptyList())

    val itemDao: ItemDao = FakeItemDao()
    val categoryDao: CategoryDao = FakeCategoryDao()

    fun seed(items: List<Item> = emptyList(), categories: List<Category> = emptyList()) {
        this.items.value = items
        this.categories.value = categories
    }

    /** id で 1 件引く。テストの assert 用。 */
    fun item(id: Int): Item = items.value.first { it.id == id }

    private inner class FakeItemDao : ItemDao {
        override fun getAllItems(): Flow<List<Item>> = items

        override fun getAllItemsWithCategory(): Flow<List<ItemWithCategory>> =
            combine(items, categories) { items, categories ->
                items.map { item ->
                    ItemWithCategory(item, categories.find { it.id == item.categoryId })
                }
            }

        override suspend fun getItemById(itemId: Int): Item = item(itemId)

        override suspend fun insertItem(item: Item): Long {
            val id = if (item.id != 0) item.id else nextId(items.value.map { it.id })
            items.update { it + item.copy(id = id) }
            return id.toLong()
        }

        override suspend fun deleteItem(item: Item) {
            items.update { list -> list.filterNot { it.id == item.id } }
        }

        override suspend fun updateItemName(itemId: Int, newName: String, updatedAt: Instant) =
            updateItem(itemId) { it.copy(name = newName, updatedAt = updatedAt) }

        override suspend fun updateItemStatus(
            itemId: Int,
            newStatus: ItemStatus,
            updatedAt: Instant
        ) = updateItem(itemId) { it.copy(status = newStatus, updatedAt = updatedAt) }

        override suspend fun updateItemStatusWithLastBoughtAt(
            itemId: Int,
            newStatus: ItemStatus,
            updatedAt: Instant
        ) = updateItem(itemId) {
            it.copy(status = newStatus, updatedAt = updatedAt, lastBoughtAt = updatedAt)
        }

        override suspend fun updateItemCategoryId(
            itemId: Int,
            newCategoryId: Int?,
            updatedAt: Instant
        ) = updateItem(itemId) { it.copy(categoryId = newCategoryId, updatedAt = updatedAt) }

        /** 存在しない id への UPDATE が何もしないのは SQL と同じ。 */
        private fun updateItem(itemId: Int, transform: (Item) -> Item) {
            items.update { list -> list.map { if (it.id == itemId) transform(it) else it } }
        }
    }

    private inner class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<Category>> = categories

        override suspend fun insert(category: Category): Long {
            val id =
                if (category.id != 0) category.id else nextId(categories.value.map { it.id })
            categories.update { it + category.copy(id = id) }
            return id.toLong()
        }

        override suspend fun delete(category: Category) {
            categories.update { list -> list.filterNot { it.id == category.id } }
            // Item.categoryId の外部キーは onDelete = SET_NULL
            items.update { list ->
                list.map { if (it.categoryId == category.id) it.copy(categoryId = null) else it }
            }
        }

        override suspend fun updateCategoryName(id: Int, newName: String, updatedAt: Instant) {
            categories.update { list ->
                list.map { if (it.id == id) it.copy(name = newName, updatedAt = updatedAt) else it }
            }
        }
    }

    private fun nextId(existing: List<Int>): Int = (existing.maxOrNull() ?: 0) + 1
}
