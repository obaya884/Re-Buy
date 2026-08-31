package io.github.obaya884.rebuy.data.item

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * プールに常駐する品目。
 *
 * カテゴリーと行き先はそれぞれ 0 または 1 個持つ。**[destinationId] が null のものが
 * 「どこでも買えるもの」**で、どの行き先の買い物モードにも並ぶ（データモデル定義書 §1）。
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Destination::class,
            parentColumns = ["id"],
            childColumns = ["destinationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("destinationId"),
        Index(value = ["name"], unique = true)
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: ItemStatus = ItemStatus.NO_DEAL,
    val categoryId: Int? = null,
    val destinationId: Int? = null,
    val lastBoughtAt: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

enum class ItemStatus(val value: Int) {
    NO_DEAL(0),
    IN_SHOPPING_LIST(1),
    CHECKED_IN_SHOPPING_LIST(2)
}
