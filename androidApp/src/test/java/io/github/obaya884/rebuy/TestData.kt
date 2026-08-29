package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import java.time.Instant

/**
 * 生成時刻。`Instant.now()` を既定値にすると同じ引数で作った 2 つが等しくならないので固定する。
 *
 * DAO の更新系は `Instant.now()` で `updatedAt` を書き換えるため、この値のままかどうかで
 * 「更新が走ったか」を判別できる。
 */
val CREATED_AT: Instant = Instant.parse("2026-01-01T00:00:00Z")

/**
 * テスト用の品目・カテゴリー。
 *
 * 名前は連番だけの無味なものにする。このリポジトリは public なので、
 * 実在の買い物リストを思わせる語を例示に使わない（憲章 §10）。
 */
fun item(
    id: Int,
    status: ItemStatus = ItemStatus.NO_DEAL,
    categoryId: Int? = null,
    lastBoughtAt: Instant? = null,
    name: String = "アイテム$id"
): Item = Item(
    id = id,
    name = name,
    status = status,
    categoryId = categoryId,
    lastBoughtAt = lastBoughtAt,
    createdAt = CREATED_AT,
    updatedAt = CREATED_AT
)

fun category(id: Int, name: String = "カテゴリー$id"): Category =
    Category(id = id, name = name, createdAt = CREATED_AT, updatedAt = CREATED_AT)
