package io.github.obaya884.rebuy.ui.screen.shopping

import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.isInBasket

/**
 * 買い物モード（画面 04）の一覧（データモデル定義書 §4）。
 *
 * **一覧は「選んだ行き先の品目」と「どこでも買えるもの」の 2 群**で、どちらも登録順。
 * 全件モード（[destinationId] が null）はこの区別を持たず、カゴの中身をそのまま 1 群で出す。
 *
 * 画面が見せる派生値はここに集める（アーキテクチャ定義書の「派生値は UiState 側で計算する」）。
 */
data class ShoppingScreenUiState(
    val destinationId: Int? = null,
    val items: List<Item> = emptyList(),
    val destinations: List<Destination> = emptyList()
) {
    /** 03 の「n 件で開始」から入った状態。行き先で絞らず、区切りも出さない。 */
    val isAllMode: Boolean = destinationId == null

    /**
     * アプリバーに出す行き先名。**行き先を読み込むまでは null**——全件モードと区別が付かない
     * ので、呼び出し側はここが埋まるまでタイトルを出さない。
     */
    val destinationName: String? = destinations.firstOrNull { it.id == destinationId }?.name

    private val inBasket: List<Item> = items.filter { it.isInBasket }

    /** 選んだ行き先の品目。全件モードではカゴの中身すべて。 */
    val ofDestination: List<Item> =
        if (isAllMode) inBasket else inBasket.filter { it.destinationId == destinationId }

    /** どこでも買えるもの。全件モードでは [ofDestination] に含まれるので空。 */
    val anywhere: List<Item> =
        if (isAllMode) emptyList() else inBasket.filter { it.destinationId == null }

    /**
     * 一覧に出ている品目。**「終了」で戻すのはこの中のチェック済みだけ**で、
     * 他の行き先で付けたチェックは残る（データモデル定義書 §3）。
     */
    val visibleItems: List<Item> = ofDestination + anywhere

    /** アプリバーの進捗「x / n」。n は一覧の総数（画面 04）。 */
    val checkedCount: Int = visibleItems.count { it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
    val totalCount: Int = visibleItems.size
}
