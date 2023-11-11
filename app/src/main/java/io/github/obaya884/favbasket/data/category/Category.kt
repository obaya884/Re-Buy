package io.github.obaya884.favbasket.data.category

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val createdAt: Date,
    val updatedAt: Date
)
