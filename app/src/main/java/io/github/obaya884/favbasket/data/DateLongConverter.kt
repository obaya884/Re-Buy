package io.github.obaya884.favbasket.data

import androidx.room.TypeConverter
import java.util.Date

class DateLongConverter {
    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }
}
