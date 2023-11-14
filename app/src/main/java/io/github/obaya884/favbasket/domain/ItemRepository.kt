package io.github.obaya884.favbasket.domain

import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemDao
import io.github.obaya884.favbasket.data.item.ItemStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ItemRepository(private val itemDao: ItemDao) {
    fun getAll(): Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insert(item: Item) {
        itemDao.insertItem(item)
    }

    suspend fun delete(item: Item) {
        itemDao.deleteItem(item)
    }

    suspend fun updateStatusAsNoDeal(item: Item) {
        if (item.status == ItemStatus.NO_DEAL) return

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.NO_DEAL,
            updatedAt = Date()
        )
    }

    suspend fun updateStatusAsInBasket(item: Item) {
        if (item.status == ItemStatus.IN_BASKET) return

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.IN_BASKET,
            updatedAt = Date()
        )
    }

    suspend fun updateStatusAsBought(id: Int) {
        val item = itemDao.getItemById(id)

        itemDao.updateItemStatus(
            itemId = item.id,
            newStatus = ItemStatus.BOUGHT,
            updatedAt = Date()
        )
    }
}
