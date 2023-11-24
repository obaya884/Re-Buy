package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    private val _inBasketItems = MutableStateFlow<List<Item>>(listOf())
    private val _scheduledBoughtItemIds = MutableStateFlow<List<Int>>(listOf())

    val uiState: StateFlow<ShoppingUiState> =
        combine(
            _isLoading,
            _inBasketItems,
            _scheduledBoughtItemIds
        ) { isLoading, inBasketItems, boughtItemIds ->
            ShoppingUiState(
                isLoading = isLoading,
                inBasketItems = inBasketItems,
                scheduledBoughtItemIds = boughtItemIds
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ShoppingUiState(false, listOf(), listOf())
        )

    init {
        viewModelScope.launch {
            itemRepository.getAll()
                .collect { items ->
                    _inBasketItems.value = items.filter { it.status == ItemStatus.IN_BASKET }
                }
        }
    }

    fun markScheduledBought(itemId: Int) {
        _scheduledBoughtItemIds.value = _scheduledBoughtItemIds.value + itemId
    }

    fun unMarkScheduledBought(itemId: Int) {
        _scheduledBoughtItemIds.value = _scheduledBoughtItemIds.value - itemId
    }

    fun changeBoughtConfirm(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val jobs = _scheduledBoughtItemIds.value.map { id ->
                launch {
                    itemRepository.updateStatusAsBought(id)
                }
            }
            jobs.joinAll()
            delay(1000)
            _isLoading.value = false
            onFinished()
        }
    }
}
