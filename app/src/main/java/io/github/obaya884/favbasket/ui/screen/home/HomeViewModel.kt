package io.github.obaya884.favbasket.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.domain.CategoryRepository
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(listOf())
    private val _preparedItems = MutableStateFlow<List<ItemWithCategory>>(listOf())
    private val _inBasketItems = MutableStateFlow<List<ItemWithCategory>>(listOf())

    val uiState: StateFlow<HomeUiState> =
        combine(
            _categories,
            _preparedItems,
            _inBasketItems
        ) { categories, preparedItems, inBasketItems ->
            HomeUiState(
                categories = categories,
                preparedItems = preparedItems,
                inBasketItems = inBasketItems
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(listOf(), listOf(), listOf()))

    // TODO: この画面の時にBOUGHT状態のアイテムが存在しないはず。というのを実装で保証したい。
    init {
        viewModelScope.launch {
            launch {
                itemRepository.getAllWithCategory()
                    .collect { items ->
                        _preparedItems.value = items
                        _inBasketItems.value =
                            items.filter { it.item.status == ItemStatus.IN_BASKET }
                    }
            }
            launch {
                categoryRepository.getAll()
                    .collect { categories ->
                        _categories.value = categories
                    }
            }
        }
    }

    fun addToBasket(item: Item) = viewModelScope.launch {
        itemRepository.updateStatusAsInBasket(item)
    }

    fun removeFromBasket(item: Item) = viewModelScope.launch {
        itemRepository.updateStatusAsNoDeal(item)
    }
}
