package io.github.obaya884.favbasket.data.item

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.obaya884.favbasket.data.category.Category
import java.util.Date

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: ItemStatus = ItemStatus.NO_DEAL,
    val categoryId: Int? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class ItemStatus {
    NO_DEAL,
    IN_BASKET,
    BOUGHT
}
