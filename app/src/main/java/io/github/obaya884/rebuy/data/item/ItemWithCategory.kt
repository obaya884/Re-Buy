package io.github.obaya884.favbasket.data.item

import androidx.room.Embedded
import androidx.room.Relation
import io.github.obaya884.favbasket.data.category.Category

/**
 * Item と Category の 多 対 1 の関係を表すデータクラス
 */
data class ItemWithCategory(
    @Embedded val item: Item,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)
