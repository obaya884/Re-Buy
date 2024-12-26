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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    private val _isAddingAnimationPlaying = MutableStateFlow(false)
    private val _isRemovingAnimationPlaying = MutableStateFlow(false)

    val uiState: StateFlow<HomeScreenUiState> =
        combine(
            _categories,
            _preparedItems,
            _inBasketItems,
            _isAddingAnimationPlaying,
            _isRemovingAnimationPlaying
        ) { categories, preparedItems, inBasketItems,
            isAddingAnimationPlaying, isRemovingAnimationPlaying ->
            HomeScreenUiState(
                categories = categories,
                preparedItems = preparedItems,
                inBasketItems = inBasketItems,
                isAddingAnimationPlaying = isAddingAnimationPlaying,
                isRemovingAnimationPlaying = isRemovingAnimationPlaying
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HomeScreenUiState(
                listOf(),
                listOf(),
                listOf(),
                isAddingAnimationPlaying = false,
                isRemovingAnimationPlaying = false
            )
        )

    init {
        viewModelScope.launch {
            launch {
                itemRepository.getAllWithCategory()
                    .collect { items ->
                        _preparedItems.update { items }
                        _inBasketItems.update {
                            items.filter { it.item.status != ItemStatus.NO_DEAL }
                        }
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
        // Ripple effect のために遅延を入れる
        delay(200)
        itemRepository.updateStatusAsInBasket(item)
        _isAddingAnimationPlaying.emit(true)
    }

    fun removeFromBasket(item: Item) = viewModelScope.launch {
        // Ripple effect のために遅延を入れる
        delay(200)
        itemRepository.updateStatusAsNoDeal(item)
        _isRemovingAnimationPlaying.emit(true)
    }

    fun onFinishAddingAnimation() = viewModelScope.launch {
        _isAddingAnimationPlaying.emit(false)
    }

    fun onFinishRemovingAnimation() = viewModelScope.launch {
        _isRemovingAnimationPlaying.emit(false)
    }
}
