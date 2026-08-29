package io.github.obaya884.rebuy.data

import androidx.room.TypeConverter
import io.github.obaya884.rebuy.data.item.ItemStatus

object ItemStatusConverter {
    @TypeConverter
    fun toItemStatus(value: Int): ItemStatus {
        return ItemStatus.entries.first { it.value == value }
    }

    @TypeConverter
    fun fromItemStatus(itemStatus: ItemStatus): Int {
        return itemStatus.value
    }
}
