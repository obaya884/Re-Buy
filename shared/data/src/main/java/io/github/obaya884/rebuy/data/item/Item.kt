package io.github.obaya884.rebuy.data.item

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.obaya884.rebuy.data.category.Category
import java.time.Instant

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: ItemStatus = ItemStatus.NO_DEAL,
    val categoryId: Int? = null,
    val lastBoughtAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class ItemStatus(val value: Int) {
    NO_DEAL(0),
    IN_SHOPPING_LIST(1),
    CHECKED_IN_SHOPPING_LIST(2)
}
