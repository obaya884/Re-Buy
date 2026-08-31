package io.github.obaya884.rebuy.ui.screen.pool

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus

/**
 * プール画面（画面 01）の状態。
 *
 * **絞り込みは導出で表す。** 選んでいるカテゴリー・行き先を持ち、一覧はそこから
 * 毎回作る（データモデル定義書 §4）。絞り込んだ結果を状態として持つと、品目が増えたときに
 * 更新し忘れる面が増える。
 */
data class PoolScreenUiState(
    val items: List<PoolItem>,
    val categories: List<Category>,
    val destinations: List<Destination>,
    val selectedCategoryId: Int? = null,
    val destinationFilter: DestinationFilter = DestinationFilter.All,
    /**
     * DB から最初の値が届いたか。**空状態は「読み込み済みで 0 件」のときだけ出す**——
     * 区別しないと、起動のたびに「まだ何も登録されていません」が一瞬見えてから一覧に変わる。
     */
    val isLoaded: Boolean = false
) {
    /** 一覧に出す品目。登録順（id 昇順）は DAO が保証する。 */
    val visibleItems: List<PoolItem> = items.filter { poolItem ->
        val categoryMatches = selectedCategoryId == null || poolItem.item.categoryId == selectedCategoryId
        categoryMatches && destinationFilter.matches(poolItem.item.destinationId)
    }

    /** アプリバーに出す総数。**絞り込みに関わらず全件**（画面 01 の「全 n 件」）。 */
    val totalCount: Int = items.size

    /** カゴに入っている件数。CTA のバッジと有効・無効に使う。 */
    val basketCount: Int = items.count { it.item.status != ItemStatus.NO_DEAL }

    val isEmpty: Boolean = isLoaded && items.isEmpty()

    /** 品目はあるが、絞り込んだ結果が空。空状態とは文言が違う（画面 01）。 */
    val isFilteredEmpty: Boolean = items.isNotEmpty() && visibleItems.isEmpty()

    val canStartShopping: Boolean = basketCount > 0

    /** 「すべて」が選ばれている状態＝カテゴリーも行き先も絞っていない。 */
    val isNoFilter: Boolean =
        selectedCategoryId == null && destinationFilter == DestinationFilter.All
}

/** 一覧の 1 行ぶん。カテゴリーと行き先は名前を出すので実体で持つ。 */
data class PoolItem(
    val item: Item,
    val category: Category?,
    val destination: Destination?
) {
    val isInBasket: Boolean = item.status != ItemStatus.NO_DEAL
}

/**
 * 行き先の絞り込み。**「どこでも」は「行き先なし」だけを指す**（画面 01）。
 * 行き先を選んだときも厳密で、その行き先の品目だけを出す。
 */
sealed interface DestinationFilter {
    data object All : DestinationFilter
    data object Anywhere : DestinationFilter
    data class Only(val destinationId: Int) : DestinationFilter

    fun matches(destinationId: Int?): Boolean = when (this) {
        All -> true
        Anywhere -> destinationId == null
        is Only -> destinationId == this.destinationId
    }
}
