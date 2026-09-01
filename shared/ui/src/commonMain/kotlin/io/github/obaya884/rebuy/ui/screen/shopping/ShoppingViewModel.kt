package io.github.obaya884.rebuy.ui.screen.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 買い物モード（画面 04）。店内でチェックを付け、終わりにチェック済みをプールへ戻す。
 *
 * **行き先は入場時に決まって以後変わらない**ので、状態ではなくコンストラクタで受ける
 * （03 の行タップごとに別の入場になる）。null は全件モード。
 */
class ShoppingViewModel(
    private val itemRepository: ItemRepository,
    destinationRepository: DestinationRepository,
    private val destinationId: Int?
) : ViewModel() {

    val uiState: StateFlow<ShoppingScreenUiState> = combine(
        itemRepository.getAll(),
        destinationRepository.getAll()
    ) { items, destinations ->
        ShoppingScreenUiState(destinationId, items, destinations)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShoppingScreenUiState(destinationId))

    /** 行タップ＝チェックの付け外し（状態 1 ↔ 2）。**行の位置は動かさない**ので並べ替えはしない。 */
    fun toggleCheck(item: Item) {
        viewModelScope.launch {
            if (item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST) {
                itemRepository.updateStatusAsInBasket(item)
            } else {
                itemRepository.updateStatusAsCheckedInBasket(item)
            }
        }
    }

    /**
     * 買い物を終える。**一覧のチェック済みだけ**をプールへ戻し、最終購入日を記録する
     * （データモデル定義書 §3）。未チェックはカゴに残る。
     *
     * 書き終えてから [onFinished] を呼ぶ。**先に画面を離れると ViewModel ごと
     * viewModelScope が畳まれ、書き込みが落ちる。**
     */
    fun finishShopping(onFinished: () -> Unit) {
        viewModelScope.launch {
            uiState.value.visibleItems
                .filter { it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
                .forEach { itemRepository.updateStatusAsBought(it.id) }
            onFinished()
        }
    }
}
