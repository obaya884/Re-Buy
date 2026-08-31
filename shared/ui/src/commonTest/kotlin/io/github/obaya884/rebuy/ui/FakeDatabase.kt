package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/**
 * Room の代わりに使うインメモリの実体。3 つの DAO が同じものを見る。
 *
 * ViewModel のテストは Repository を本物のまま使い、その下の DAO だけを差し替える。
 * ステータス遷移の早期 return など Repository のルールも一緒に網へ入れるため。
 *
 * Room に寄せてある挙動は、外部キーの `SET_NULL` と存在しない親 id の拒否、
 * 名前の UNIQUE インデックスと `OnConflictStrategy.ABORT`、
 * 存在しない id への UPDATE が no-op であること、
 * `AUTOINCREMENT` が削除した id を再利用しないこと、
 * 一覧の並び順（品目は id 昇順、カテゴリーと行き先は sortOrder 昇順）。
 *
 * **再現していない差分**は 3 つ。
 * - 制約違反で投げる例外が `SQLiteConstraintException` ではない（JVM 段に android.database が無い）
 * - Flow が `StateFlow` なので同じ値の再 emit が落ちる（Room は無効化のたびに流す）。
 *   emit 回数に意味を持たせるテストは書かないこと
 * - [seed] は制約を通らない。Room では作れない状態（同名 2 件・宙に浮いた `destinationId`）も
 *   置けてしまうので、**初期状態は満たすべき制約を自分で守って書くこと**
 */
class FakeDatabase {
    private val items = MutableStateFlow<List<Item>>(emptyList())
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val destinations = MutableStateFlow<List<Destination>>(emptyList())

    // AUTOINCREMENT は「これまでに使った最大値 + 1」を返し、削除しても戻らない
    private var lastItemId = 0
    private var lastCategoryId = 0
    private var lastDestinationId = 0

    val itemDao: ItemDao = FakeItemDao()
    val categoryDao: CategoryDao = FakeCategoryDao()
    val destinationDao: DestinationDao = FakeDestinationDao()

    /** 初期状態を置く。書き込みはここからだけ行う。 */
    fun seed(
        items: List<Item> = emptyList(),
        categories: List<Category> = emptyList(),
        destinations: List<Destination> = emptyList()
    ) {
        this.items.value = items.sortedBy { it.id }
        this.categories.value = categories.sortedWith(categoryOrder)
        this.destinations.value = destinations.sortedWith(destinationOrder)
        lastItemId = items.maxOfOrNull { it.id } ?: 0
        lastCategoryId = categories.maxOfOrNull { it.id } ?: 0
        lastDestinationId = destinations.maxOfOrNull { it.id } ?: 0
    }

    /** すでに入っているものへ 1 件足す。「後から増えた」ことを表したいときに使う。 */
    fun add(item: Item) {
        items.update { (it + item).sortedBy { stored -> stored.id } }
    }

    val storedItems: List<Item> get() = items.value

    val storedCategories: List<Category> get() = categories.value

    val storedDestinations: List<Destination> get() = destinations.value

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
            requireDestinationExists(item.destinationId)
            requireNameIsFree(item.name, exceptId = null)
            val id = if (item.id != 0) item.id else ++lastItemId
            requireIdIsFree(id)
            items.update { list -> (list + item.copy(id = id)).sortedBy { it.id } }
            return id.toLong()
        }

        override suspend fun deleteItem(item: Item) {
            items.update { list -> list.filterNot { it.id == item.id } }
        }

        override suspend fun updateItemName(itemId: Int, newName: String, updatedAt: Instant) {
            requireNameIsFree(newName, exceptId = itemId)
            updateItem(itemId) { it.copy(name = newName, updatedAt = updatedAt) }
        }

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

        override suspend fun updateItemDestinationId(
            itemId: Int,
            newDestinationId: Int?,
            updatedAt: Instant
        ) {
            requireDestinationExists(newDestinationId)
            updateItem(itemId) { it.copy(destinationId = newDestinationId, updatedAt = updatedAt) }
        }

        /** 存在しない id への UPDATE が何もしないのは SQL と同じ。 */
        private fun updateItem(itemId: Int, transform: (Item) -> Item) {
            items.update { list -> list.map { if (it.id == itemId) transform(it) else it } }
        }

        private fun requireNameIsFree(name: String, exceptId: Int?) =
            requireNameIsUnique("品目", items.value.map { it.id to it.name }, name, exceptId)

        private fun requireIdIsFree(id: Int) =
            requireIdIsUnique("品目", items.value.map { it.id }, id)
    }

    private inner class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<Category>> = categories

        override suspend fun insert(category: Category): Long {
            requireNameIsFree(category.name, exceptId = null)
            val id = if (category.id != 0) category.id else ++lastCategoryId
            requireIdIsFree(id)
            categories.update { list ->
                (list + category.copy(id = id)).sortedWith(categoryOrder)
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
            requireNameIsFree(newName, exceptId = id)
            updateCategory(id) { it.copy(name = newName, updatedAt = updatedAt) }
        }

        override suspend fun updateCategorySortOrder(
            id: Int,
            newSortOrder: Int,
            updatedAt: Instant
        ) = updateCategory(id) { it.copy(sortOrder = newSortOrder, updatedAt = updatedAt) }

        override suspend fun maxSortOrder(): Int = categories.value.maxOfOrNull { it.sortOrder } ?: 0

        private fun updateCategory(id: Int, transform: (Category) -> Category) {
            categories.update { list ->
                list.map { if (it.id == id) transform(it) else it }.sortedWith(categoryOrder)
            }
        }

        private fun requireNameIsFree(name: String, exceptId: Int?) =
            requireNameIsUnique(
                "カテゴリー",
                categories.value.map { it.id to it.name },
                name,
                exceptId
            )

        private fun requireIdIsFree(id: Int) =
            requireIdIsUnique("カテゴリー", categories.value.map { it.id }, id)
    }

    private inner class FakeDestinationDao : DestinationDao {
        override fun getAllDestinations(): Flow<List<Destination>> = destinations

        override suspend fun insert(destination: Destination): Long {
            requireNameIsFree(destination.name, exceptId = null)
            val id = if (destination.id != 0) destination.id else ++lastDestinationId
            requireIdIsFree(id)
            destinations.update { list ->
                (list + destination.copy(id = id)).sortedWith(destinationOrder)
            }
            return id.toLong()
        }

        override suspend fun delete(destination: Destination) {
            destinations.update { list -> list.filterNot { it.id == destination.id } }
            // Item.destinationId の外部キーは onDelete = SET_NULL
            items.update { list ->
                list.map {
                    if (it.destinationId == destination.id) it.copy(destinationId = null) else it
                }
            }
        }

        override suspend fun updateDestinationName(id: Int, newName: String, updatedAt: Instant) {
            requireNameIsFree(newName, exceptId = id)
            updateDestination(id) { it.copy(name = newName, updatedAt = updatedAt) }
        }

        override suspend fun updateDestinationSortOrder(
            id: Int,
            newSortOrder: Int,
            updatedAt: Instant
        ) = updateDestination(id) { it.copy(sortOrder = newSortOrder, updatedAt = updatedAt) }

        override suspend fun maxSortOrder(): Int =
            destinations.value.maxOfOrNull { it.sortOrder } ?: 0

        private fun updateDestination(id: Int, transform: (Destination) -> Destination) {
            destinations.update { list ->
                list.map { if (it.id == id) transform(it) else it }.sortedWith(destinationOrder)
            }
        }

        private fun requireNameIsFree(name: String, exceptId: Int?) =
            requireNameIsUnique(
                "行き先",
                destinations.value.map { it.id to it.name },
                name,
                exceptId
            )

        private fun requireIdIsFree(id: Int) =
            requireIdIsUnique("行き先", destinations.value.map { it.id }, id)
    }

    /** Room は外部キー制約を有効にするので、存在しないカテゴリーは参照できない。 */
    private fun requireCategoryExists(categoryId: Int?) {
        if (categoryId != null && categories.value.none { it.id == categoryId }) {
            error("id = $categoryId のカテゴリーが無い（本番では SQLiteConstraintException）")
        }
    }

    /** 行き先も同じ。null は「どこでも買えるもの」なので常に通す。 */
    private fun requireDestinationExists(destinationId: Int?) {
        if (destinationId != null && destinations.value.none { it.id == destinationId }) {
            error("id = $destinationId の行き先が無い（本番では SQLiteConstraintException）")
        }
    }
}

/** name の UNIQUE インデックス。更新では自分自身を除いて見る。 */
private fun requireNameIsUnique(
    label: String,
    stored: List<Pair<Int, String>>,
    name: String,
    exceptId: Int?
) {
    if (stored.any { (id, storedName) -> storedName == name && id != exceptId }) {
        error("$label の名前「$name」はすでにある（本番では SQLiteConstraintException）")
    }
}

/** `OnConflictStrategy.ABORT` なので、id がぶつかる挿入は置き換えずに落ちる。 */
private fun requireIdIsUnique(label: String, stored: List<Int>, id: Int) {
    if (stored.contains(id)) {
        error("$label の id = $id はすでにある（本番では SQLiteConstraintException）")
    }
}

/** sortOrder 昇順、同値は登録順（id 昇順）で割る。 */
private val categoryOrder = compareBy<Category>({ it.sortOrder }, { it.id })
private val destinationOrder = compareBy<Destination>({ it.sortOrder }, { it.id })
