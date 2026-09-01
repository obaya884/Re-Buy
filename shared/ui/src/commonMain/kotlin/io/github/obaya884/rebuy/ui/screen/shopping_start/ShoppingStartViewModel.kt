package io.github.obaya884.rebuy.ui.screen.shopping_start

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

/**
 * 買い物開始シート（画面 03）。**出発前にカゴの中身を行き先別に確認する**。
 *
 * 導出はデータモデル定義書 §4。カゴ（状態 1 か 2）だけを見て、
 * **どこでも買えるものは独立した行にせず、各行の件数に「＋m」として足す**——
 * どの店へ行っても持っていくものなので、行き先ごとの見出しの下に重複して出さない。
 */
class ShoppingStartViewModel(
    itemRepository: ItemRepository,
    destinationRepository: DestinationRepository
) : ViewModel() {

    val uiState: StateFlow<ShoppingStartUiState> = combine(
        itemRepository.getAll(),
        destinationRepository.getAll()
    ) { items, destinations ->
        val inBasket = items.filter { it.status != ItemStatus.NO_DEAL }
        val anywhere = inBasket.filter { it.destinationId == null }

        ShoppingStartUiState(
            rows = destinations.mapNotNull { destination ->
                val ofDestination = inBasket.filter { it.destinationId == destination.id }
                if (ofDestination.isEmpty()) {
                    null
                } else {
                    DestinationRow(
                        destinationId = destination.id,
                        name = destination.name,
                        preview = ofDestination.take(PREVIEW_COUNT).map(Item::name),
                        count = ofDestination.size,
                        anywhereCount = anywhere.size
                    )
                }
            },
            basketCount = inBasket.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShoppingStartUiState())

    private companion object {
        /** プレビューは先頭 2 件固定（画面 03。「など」は付けない）。 */
        const val PREVIEW_COUNT = 2
    }
}

data class ShoppingStartUiState(
    val rows: List<DestinationRow> = emptyList(),
    val basketCount: Int = 0
) {
    /**
     * カゴに行き先付きの品目が 1 件も無いとき（データモデル定義書 §4）。
     * 内訳の代わりに「n 件で開始」の 1 行だけを出す。
     */
    val isAllMode: Boolean = basketCount > 0 && rows.isEmpty()
}

/** 内訳の 1 行。件数は「n＋m 件」で、m は**どこでも買えるもの**の件数。 */
data class DestinationRow(
    val destinationId: Int,
    val name: String,
    val preview: List<String>,
    val count: Int,
    val anywhereCount: Int
)
