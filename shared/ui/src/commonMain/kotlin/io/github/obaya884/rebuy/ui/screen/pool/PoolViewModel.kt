package io.github.obaya884.rebuy.ui.screen.pool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * プール画面（画面 01）。
 *
 * **一覧は 3 つの Flow を突き合わせて作る。** 品目・カテゴリー・行き先を別々に読み、
 * 行に出す名前をここで結ぶ。Room の `@Relation` を 2 つ重ねる手もあるが、
 * 一覧は 1 画面ぶんの件数しか出ないので、クエリを増やさず素直に組む。
 */
class PoolViewModel(
    private val itemRepository: ItemRepository,
    categoryRepository: CategoryRepository,
    destinationRepository: DestinationRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Int?>(null)
    private val destinationFilter = MutableStateFlow<DestinationFilter>(DestinationFilter.All)

    val uiState: StateFlow<PoolScreenUiState> = combine(
        itemRepository.getAll(),
        categoryRepository.getAll(),
        destinationRepository.getAll(),
        selectedCategoryId,
        destinationFilter
    ) { items, categories, destinations, categoryId, destination ->
        PoolScreenUiState(
            items = items.map { item ->
                PoolItem(
                    item = item,
                    category = categories.firstOrNull { it.id == item.categoryId },
                    destination = destinations.firstOrNull { it.id == item.destinationId }
                )
            },
            categories = categories,
            destinations = destinations,
            selectedCategoryId = categoryId,
            destinationFilter = destination,
            isLoaded = true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PoolScreenUiState(items = emptyList(), categories = emptyList(), destinations = emptyList())
    )

    /**
     * 行タップ＝カゴの出し入れ（データモデル定義書 §3）。
     * **チェック済み（状態 2）から出すとチェックは失われる**——戻すのは常に状態 0。
     */
    fun toggleBasket(item: Item) {
        viewModelScope.launch {
            if (item.status == ItemStatus.NO_DEAL) {
                itemRepository.updateStatusAsInBasket(item)
            } else {
                itemRepository.updateStatusAsNoDeal(item)
            }
        }
    }

    /** カテゴリーの絞り込み。同じチップをもう一度押すと解除する。 */
    fun selectCategory(categoryId: Int) {
        selectedCategoryId.value = if (categoryId == selectedCategoryId.value) null else categoryId
    }

    /** 行き先の絞り込み。同じチップをもう一度押すと解除する。 */
    fun selectDestination(filter: DestinationFilter) {
        destinationFilter.value =
            if (filter == destinationFilter.value) DestinationFilter.All else filter
    }

    /** 「すべて」＝カテゴリーと行き先の両方を解除する（画面 01）。 */
    fun clearFilters() {
        selectedCategoryId.value = null
        destinationFilter.value = DestinationFilter.All
    }
}
