package io.github.obaya884.rebuy.ui.screen.shopping_start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.isInBasket
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 買い物開始シート（画面 03）。**出発前にカゴの中身を行き先別に確認する**。
 *
 * ここは品目と行き先を突き合わせるだけで、**内訳の作り方は UiState が持つ**
 * （アーキテクチャ定義書の「派生値は UiState 側で計算する」）。
 */
class ShoppingStartViewModel(
    itemRepository: ItemRepository,
    destinationRepository: DestinationRepository
) : ViewModel() {

    val uiState: StateFlow<ShoppingStartSheetUiState> = combine(
        itemRepository.getAll(),
        destinationRepository.getAll()
    ) { items, destinations ->
        ShoppingStartSheetUiState(items = items, destinations = destinations)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShoppingStartSheetUiState())
}

/**
 * 内訳の導出（データモデル定義書 §4）。
 *
 * **どこでも買えるものは独立した行にしない。** どの店へ行っても持っていくものなので、
 * 行き先ごとの見出しの下に重複させず、[anywhereCount] として各行の件数に足す（画面 03）。
 *
 * **カゴが空のときは開かれない**——プールの CTA が押せないため（画面 01）。
 */
data class ShoppingStartSheetUiState(
    val items: List<Item> = emptyList(),
    val destinations: List<Destination> = emptyList()
) {
    private val inBasket: List<Item> = items.filter { it.isInBasket }

    val basketCount: Int = inBasket.size

    /** どこでも買えるもの（行き先なし）のカゴ内件数。**シート全体で 1 つの事実**。 */
    val anywhereCount: Int = inBasket.count { it.destinationId == null }

    /** カゴに品目を持つ行き先だけを、行き先の並び順で。 */
    val rows: List<DestinationSummary> = inBasket.groupBy { it.destinationId }.let { byDestination ->
        destinations.mapNotNull { destination ->
            val ofDestination = byDestination[destination.id] ?: return@mapNotNull null
            DestinationSummary(
                destinationId = destination.id,
                name = destination.name,
                // プレビューは先頭 2 件固定（画面 03。「など」は付けない）
                preview = ofDestination.take(PREVIEW_COUNT).map(Item::name),
                count = ofDestination.size
            )
        }
    }

    /** カゴに行き先付きの品目が 1 件も無いとき。内訳の代わりに「n 件で開始」の 1 行だけ。 */
    val isAllMode: Boolean = basketCount > 0 && rows.isEmpty()

    private companion object {
        const val PREVIEW_COUNT = 2
    }
}

/** 内訳の 1 行ぶん。 */
data class DestinationSummary(
    val destinationId: Int,
    val name: String,
    val preview: List<String>,
    val count: Int
)
