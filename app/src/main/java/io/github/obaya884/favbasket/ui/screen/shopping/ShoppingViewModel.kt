package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)

    private val _inShoppingListItems = MutableStateFlow<List<Item>>(listOf())
    private val _scheduledBoughtItemIds = MutableStateFlow<List<Int>>(listOf())

    private val _isShowNavigateBackAlertDialog = MutableStateFlow(false)
    private val _isShowFinishShoppingAlertDialog = MutableStateFlow(false)

    val uiState: StateFlow<ShoppingScreenUiState> =
        combine(
            _isLoading,
            _inShoppingListItems,
            _scheduledBoughtItemIds,
            _isShowNavigateBackAlertDialog,
            _isShowFinishShoppingAlertDialog
        ) { isLoading, inShoppingListItems, boughtItemIds, isShowNavigateBackAlertDialog, isShowFinishShoppingAlertDialog ->
            ShoppingScreenUiState(
                isLoading = isLoading,
                inShoppingListItems = inShoppingListItems,
                scheduledBoughtItemIds = boughtItemIds,
                isShowNavigateBackAlertDialog = isShowNavigateBackAlertDialog,
                isShowFinishShoppingAlertDialog = isShowFinishShoppingAlertDialog
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ShoppingScreenUiState(
                isLoading = false,
                inShoppingListItems = listOf(),
                scheduledBoughtItemIds = listOf(),
                isShowNavigateBackAlertDialog = false,
                isShowFinishShoppingAlertDialog = false
            )
        )

    init {
        viewModelScope.launch {
            itemRepository.getAll()
                .collect { items ->
                    _inShoppingListItems.update { items.filter { it.status == ItemStatus.IN_SHOPPING_LIST } }
                }
        }
    }

    fun markScheduledBought(itemId: Int) {
        viewModelScope.launch {
            _scheduledBoughtItemIds.update { it + itemId }
        }
    }

    fun unMarkScheduledBought(itemId: Int) {
        viewModelScope.launch {
            _scheduledBoughtItemIds.update { it - itemId }
        }
    }

    fun changeBoughtConfirm(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isLoading.emit(true)
            val jobs = _scheduledBoughtItemIds.value.map { id ->
                launch {
                    itemRepository.updateStatusAsBought(id)
                }
            }
            delay(500)
            jobs.joinAll()
            _isLoading.emit(false)
            onFinished()
        }
    }

    fun showNavigateBackAlertDialog() {
        viewModelScope.launch {
            _isShowNavigateBackAlertDialog.emit(true)
        }
    }

    fun hideNavigateBackAlertDialog() {
        viewModelScope.launch {
            _isShowNavigateBackAlertDialog.emit(false)
        }
    }

    fun showFinishShoppingAlertDialog() {
        viewModelScope.launch {
            _isShowFinishShoppingAlertDialog.emit(true)
        }
    }

    fun hideFinishShoppingAlertDialog() {
        viewModelScope.launch {
            _isShowFinishShoppingAlertDialog.emit(false)
        }
    }
}
