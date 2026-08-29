package io.github.obaya884.rebuy.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(listOf())
    private val _items = MutableStateFlow<List<ItemWithCategory>>(listOf())

    val uiState: StateFlow<HomeScreenUiState> =
        combine(
            _categories,
            _items
        ) { categories, items ->
            HomeScreenUiState(
                categories = categories,
                items = items
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HomeScreenUiState(
                categories = listOf(),
                items = listOf()
            )
        )

    init {
        viewModelScope.launch {
            //FIXME: zipして取得した方が良いかも。
            launch {
                itemRepository.getAllWithCategory()
                    .collect { items ->
                        _items.update { items }
                    }
            }
            launch {
                categoryRepository.getAll()
                    .collect { categories ->
                        _categories.update { categories }
                    }
            }
        }
    }

    fun addToBasket(item: Item) = viewModelScope.launch {
        delay(RIPPLE_DELAY_MS)
        itemRepository.updateStatusAsInBasket(item)
    }

    fun removeFromBasket(item: Item) = viewModelScope.launch {
        delay(RIPPLE_DELAY_MS)
        itemRepository.updateStatusAsNoDeal(item)
    }

    companion object {
        /**
         * 状態を変えるまでの待ち時間。
         *
         * すぐ反映すると波紋が出切る前に行が動く。**本来は UI 層の関心**で、
         * ここに置いているのは暫定（技術改善バックログ T-27）。
         */
        internal const val RIPPLE_DELAY_MS = 200L
    }
}
