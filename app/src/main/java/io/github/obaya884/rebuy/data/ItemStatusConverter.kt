package io.github.obaya884.favbasket.data

import androidx.room.TypeConverter
import io.github.obaya884.favbasket.data.item.ItemStatus

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
