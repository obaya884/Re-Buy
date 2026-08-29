package io.github.obaya884.rebuy.ui

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
 * ステータス遷移の早期 return など Repository のルールも一緒に網へ入れるため。
 *
 * Room に寄せてある挙動は、外部キーの `SET_NULL` と存在しない親 id の拒否、
 * `OnConflictStrategy.REPLACE`、存在しない id への UPDATE が no-op であること、
 * `AUTOINCREMENT` が削除した id を再利用しないこと。
 *
 * **再現していない差分**は 2 つ。
 * - 外部キー違反で投げる例外が `SQLiteConstraintException` ではない（JVM 段に android.database が無い）
 * - Flow が `StateFlow` なので同じ値の再 emit が落ちる（Room は無効化のたびに流す）。
 *   emit 回数に意味を持たせるテストは書かないこと
 */
class FakeDatabase {
    private val items = MutableStateFlow<List<Item>>(emptyList())
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    // AUTOINCREMENT は「これまでに使った最大値 + 1」を返し、削除しても戻らない
    private var lastItemId = 0
    private var lastCategoryId = 0

    val itemDao: ItemDao = FakeItemDao()
    val categoryDao: CategoryDao = FakeCategoryDao()

    /** 初期状態を置く。書き込みはここからだけ行う。 */
    fun seed(items: List<Item> = emptyList(), categories: List<Category> = emptyList()) {
        this.items.value = items
        this.categories.value = categories
        lastItemId = items.maxOfOrNull { it.id } ?: 0
        lastCategoryId = categories.maxOfOrNull { it.id } ?: 0
    }

    /** すでに入っているものへ 1 件足す。「後から増えた」ことを表したいときに使う。 */
    fun add(item: Item) {
        items.update { it + item }
    }

    val storedItems: List<Item> get() = items.value

    val storedCategories: List<Category> get() = categories.value

    /** id で 1 件引く。テストの assert 用。 */
    fun storedItem(id: Int): Item =
        items.value.firstOrNull { it.id == id } ?: error("id = $id の品目が無い")

    private inner class FakeItemDao : ItemDao {
        override fun getAllItems(): Flow<List<Item>> = items

        override fun getAllItemsWithCategory(): Flow<List<ItemWithCategory>> =
            combine(items, categories) { itemList, categoryList ->
                itemList.map { item ->
                    ItemWithCategory(item, categoryList.find { it.id == item.categoryId })
                }
            }

        override suspend fun getItemById(itemId: Int): Item = storedItem(itemId)

        override suspend fun insertItem(item: Item): Long {
            requireCategoryExists(item.categoryId)
            val id = if (item.id != 0) item.id else ++lastItemId
            items.update { list -> list.filterNot { it.id == id } + item.copy(id = id) }
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
        ) {
            requireCategoryExists(newCategoryId)
            updateItem(itemId) { it.copy(categoryId = newCategoryId, updatedAt = updatedAt) }
        }

        /** 存在しない id への UPDATE が何もしないのは SQL と同じ。 */
        private fun updateItem(itemId: Int, transform: (Item) -> Item) {
            items.update { list -> list.map { if (it.id == itemId) transform(it) else it } }
        }
    }

    private inner class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<Category>> = categories

        override suspend fun insert(category: Category): Long {
            val id = if (category.id != 0) category.id else ++lastCategoryId
            categories.update { list ->
                list.filterNot { it.id == id } + category.copy(id = id)
            }
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

    /** Room は外部キー制約を有効にするので、存在しないカテゴリーは参照できない。 */
    private fun requireCategoryExists(categoryId: Int?) {
        if (categoryId != null && categories.value.none { it.id == categoryId }) {
            error("id = $categoryId のカテゴリーが無い（本番では SQLiteConstraintException）")
        }
    }
}
