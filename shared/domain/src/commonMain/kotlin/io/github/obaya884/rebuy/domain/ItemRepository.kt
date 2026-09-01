package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    fun getAll(): Flow<List<Item>> = itemDao.getAllItems()

    fun getAllWithCategory(): Flow<List<ItemWithCategory>> = itemDao.getAllItemsWithCategory()

    /**
     * 名前を検証してから入れる（データモデル定義書 §5）。保存するのはトリム後の名前で、
     * カテゴリーと行き先は渡されたまま。
     */
    suspend fun insert(item: Item): SaveResult =
        saveWithValidatedName(item.name, exceptId = NEW_RECORD_ID, itemDao::existsName) { normalized ->
            itemDao.insertItem(item.copy(name = normalized)).toInt()
        }

    suspend fun delete(item: Item) {
        itemDao.deleteItem(item)
    }

    /** id で消す。**開いている品目を消す**ときは、打ちかけの名前を持ち回らずに済む。 */
    suspend fun delete(id: Int) {
        itemDao.deleteItemById(id)
    }

    /**
     * 名前・カテゴリー・行き先をまとめて書く（画面 06 の「保存」）。
     * **名前が弾かれたら何も書かない**——中途半端な反映を残さない。
     */
    suspend fun update(
        id: Int,
        name: String,
        categoryId: Int?,
        destinationId: Int?
    ): SaveResult = saveWithValidatedName(name, exceptId = id, itemDao::existsName) { normalized ->
        itemDao.updateItemNameAndRelations(id, normalized, categoryId, destinationId)
        id
    }

    suspend fun updateName(id: Int, newName: String): SaveResult =
        saveWithValidatedName(newName, exceptId = id, itemDao::existsName) { normalized ->
            itemDao.updateItemName(itemId = id, newName = normalized)
            id
        }

    suspend fun updateCategory(id: Int, newCategoryId: Int?) {
        itemDao.updateItemCategoryId(
            itemId = id,
            newCategoryId = newCategoryId
        )
    }

    /** null は「どこでも買えるもの」。 */
    suspend fun updateDestination(id: Int, newDestinationId: Int?) {
        itemDao.updateItemDestinationId(
            itemId = id,
            newDestinationId = newDestinationId
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
