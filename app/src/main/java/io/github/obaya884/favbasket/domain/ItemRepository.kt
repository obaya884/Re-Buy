package io.github.obaya884.favbasket.domain

import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemDao
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

    suspend fun addToBasket(item: Item) {
        if (item.isInBasket) return

        itemDao.updateItemIsInBasket(
            itemId = item.id,
            newIsInBasket = true,
            updatedAt = Date()
        )
    }

    suspend fun removeFromBasket(item: Item) {
        if (!item.isInBasket) return

        itemDao.updateItemIsInBasket(
            itemId = item.id,
            newIsInBasket = false,
            updatedAt = Date()
        )
    }
}
