package io.github.obaya884.rebuy.ui.screen.pool

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.isInBasket

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
    val basketCount: Int = items.count { it.item.isInBasket }

    val isEmpty: Boolean = isLoaded && items.isEmpty()

    /** 品目はあるが、絞り込んだ結果が空。空状態とは文言が違う（画面 01）。 */
    val isFilteredEmpty: Boolean = items.isNotEmpty() && visibleItems.isEmpty()

    val canStartShopping: Boolean = basketCount > 0

    /**
     * **これから始まる買い物が全件モード**（行き先を選ばない買い物）になるか。真なら CTA は
     * 開始シート（03）を開かず買い物モードへ直行する（画面 01・FB-04）。
     *
     * **他の画面の `isAllMode` と時制が違う**——あちらは「今いるモード」で、こちらは予測。
     * カゴが空なら false になるが、それは「全件モードでない」ではなく「そもそも始まらない」。
     *
     * **数えるのは [visibleItems] ではなく [items]。** 絞り込みは見せ方の話なので、
     * 「🏬 どこでも」を選んだだけで行き先付きが消えて直行するようになってはいけない。
     *
     * **行き先は id ではなく実体で見る。** DB 上は SET_NULL で孤児が出ないので同値だが、
     * 品目と行き先は別々の Flow で届くので、**行き先を消した直後に「品目はまだ古い id を持ち、
     * 行き先はもう無い」一瞬**がある。id で見るとそこで 03 が開き、内訳は行き先を突き合わせて
     * 作るので**行が 1 つも無いシート**になる（14 §4）。
     */
    val startsInAllMode: Boolean = canStartShopping &&
        items.none { it.isInBasket && it.destination != null }

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
    val isInBasket: Boolean get() = item.isInBasket
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
