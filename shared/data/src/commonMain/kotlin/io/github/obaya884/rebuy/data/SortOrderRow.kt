package io.github.obaya884.rebuy.data

/**
 * 並び順だけを引くときの行。カテゴリーと行き先で同じ形なので 1 つで足りる。
 *
 * **並び替えの保存で「書かなくてよい行」を見分ける**ために使う（`CategoryDao.updateSortOrders`）。
 */
data class SortOrderRow(val id: Int, val sortOrder: Int)

/**
 * 並び替えの保存の中身（画面 09）。**渡された順に 1..n を振り直す**。
 *
 * 値が変わらない行は書かない——`updatedAt` を動かさないため（データモデル定義書 §3 の
 * 「同じ状態への更新は no-op」と同じ流儀）。
 *
 * カテゴリーと行き先で同じなので 1 か所に置く。呼ぶ側（DAO）が `@Transaction` を持つ:
 * 1 件ずつ書くと、途中で `sortOrder` が重なった一覧が Flow に流れて**別の順で一瞬描かれる**
 * （`ItemDao.updateItemNameAndRelations` と同じ理由）。
 */
suspend fun applySortOrders(
    orderedIds: List<Int>,
    current: List<SortOrderRow>,
    update: suspend (id: Int, newSortOrder: Int) -> Unit
) {
    val currentById = current.associate { it.id to it.sortOrder }
    orderedIds.forEachIndexed { index, id ->
        val newSortOrder = index + 1
        if (currentById[id] != newSortOrder) {
            update(id, newSortOrder)
        }
    }
}
