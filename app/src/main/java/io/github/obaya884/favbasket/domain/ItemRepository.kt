package io.github.obaya884.favbasket.domain

import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemDao
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    fun getAll(): Flow<List<Item>> = itemDao.getAllItems()

    fun getAllWithCategory(): Flow<List<ItemWithCategory>> = itemDao.getAllItemsWithCategory()

    suspend fun insert(item: Item) {
        itemDao.insertItem(item)
    }

    suspend fun delete(item: Item) {
        itemDao.deleteItem(item)
    }

    suspend fun updateName(id: Int, newName: String) {
        itemDao.updateItemName(
            itemId = id,
            newName = newName
        )
    }

    suspend fun updateCategory(id: Int, newCategoryId: Int?) {
        itemDao.updateItemCategoryId(
            itemId = id,
            newCategoryId = newCategoryId
        )
    }

    suspend fun updateStatusAsNoDeal(item: Item) {
        if (item.status == ItemStatus.NO_DEAL) return

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.NO_DEAL
        )
    }

    suspend fun updateStatusAsInBasket(item: Item) {
        if (item.status == ItemStatus.IN_SHOPPING_LIST) return

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.IN_SHOPPING_LIST
        )
    }

    suspend fun updateStatusAsCheckedInBasket(item: Item) {
        if (item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST) return

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.CHECKED_IN_SHOPPING_LIST
        )
    }

    suspend fun updateStatusAsBought(id: Int) {
        val item = itemDao.getItemById(id)

        itemDao.updateItemStatusWithLastBoughtAt(
            itemId = item.id,
            newStatus = ItemStatus.NO_DEAL
        )
    }
}
