package io.github.obaya884.rebuy.data

/**
 * 並び順だけを引くときの行。カテゴリーと行き先で同じ形なので 1 つで足りる。
 *
 * **並び替えの保存で「書かなくてよい行」を見分ける**ために使う（`CategoryDao.updateSortOrders`）。
 */
data class SortOrderRow(val id: Int, val sortOrder: Int)
