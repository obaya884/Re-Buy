package io.github.obaya884.favbasket.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _preparedItems = MutableStateFlow<List<Item>>(listOf())
    private val _inBasketItems = MutableStateFlow<List<Item>>(listOf())
    val uiState: StateFlow<MainUiState> =
        combine(_preparedItems, _inBasketItems) { preparedItems, inBasketItems ->
            MainUiState(
                preparedItems = preparedItems,
                inBasketItems = inBasketItems
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState(listOf(), listOf()))

    init {
        viewModelScope.launch {
            itemRepository.getAll()
                .collect { items ->
                    _preparedItems.value = items
                    _inBasketItems.value = items.filter { it.isInBasket }
                }
        }
    }

    fun addToBasket(item: Item) = viewModelScope.launch {
        itemRepository.addToBasket(item)
    }

    fun removeFromBasket(item: Item) = viewModelScope.launch {
        itemRepository.removeFromBasket(item)
    }
}
