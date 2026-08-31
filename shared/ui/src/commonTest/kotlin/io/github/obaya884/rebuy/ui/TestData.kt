package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 生成時刻。`Clock.System.now()` を既定値にすると同じ引数で作った 2 つが等しくならないので固定する。
 *
 * DAO の更新系は `Clock.System.now()` で `updatedAt` を書き換えるため、この値のままかどうかで
 * 「更新が走ったか」を判別できる。
 */
val CREATED_AT: Instant = Instant.parse("2026-01-01T00:00:00Z")

/**
 * テスト用の品目・カテゴリー・行き先。
 *
 * 名前は連番だけの無味なものにする。このリポジトリは public なので、
 * 実在の買い物リストを思わせる語を例示に使わない（要求定義書 §11）。
 */
fun item(
    id: Int,
    status: ItemStatus = ItemStatus.NO_DEAL,
    categoryId: Int? = null,
    destinationId: Int? = null,
    lastBoughtAt: Instant? = null,
    name: String = "アイテム$id"
): Item = Item(
    id = id,
    name = name,
    status = status,
    categoryId = categoryId,
    destinationId = destinationId,
    lastBoughtAt = lastBoughtAt,
    createdAt = CREATED_AT,
    updatedAt = CREATED_AT
)

fun category(id: Int, name: String = "カテゴリー$id", sortOrder: Int = id): Category =
    Category(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT
    )

fun destination(id: Int, name: String = "行き先$id", sortOrder: Int = id): Destination =
    Destination(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT
    )
