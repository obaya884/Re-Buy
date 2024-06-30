package io.github.obaya884.favbasket.data.item

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.obaya884.favbasket.data.category.Category
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
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

// TODO: DBでの実体はIntにしたい（RoomはデフォルトだとenumをTEXTに扱う）
enum class ItemStatus {
    NO_DEAL,
    IN_BASKET,
    BOUGHT
}
