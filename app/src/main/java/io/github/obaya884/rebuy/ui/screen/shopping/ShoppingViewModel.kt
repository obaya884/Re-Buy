package io.github.obaya884.rebuy.ui.screen.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.domain.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<ItemWithCategory>>(listOf())
    private val _isLoading = MutableStateFlow(false)
    private val _isShowFinishShoppingAlertDialog = MutableStateFlow(false)

    val uiState: StateFlow<ShoppingScreenUiState> =
        combine(
            _isLoading,
            _items,
            _isShowFinishShoppingAlertDialog
        ) { isLoading, items, isShowFinishShoppingAlertDialog ->
            ShoppingScreenUiState(
                items = items,
                isLoading = isLoading,
                isShowFinishShoppingAlertDialog = isShowFinishShoppingAlertDialog
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ShoppingScreenUiState(
                items = listOf(),
                isLoading = false,
                isShowFinishShoppingAlertDialog = false
            )
        )

    init {
        viewModelScope.launch {
            itemRepository.getAllWithCategory()
                .collect { items ->
                    _items.update { items }
                }
        }
    }

    fun markScheduledBought(item: Item) {
        viewModelScope.launch {
            itemRepository.updateStatusAsCheckedInBasket(item)
        }
    }

    fun unMarkScheduledBought(item: Item) {
        viewModelScope.launch {
            itemRepository.updateStatusAsInBasket(item)
        }
    }

    fun changeBoughtConfirm(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isLoading.emit(true)
            val jobs = uiState.value.checkedInShoppingListItems.map { item ->
                launch {
                    itemRepository.updateStatusAsBought(item.id)
                }
            }
            delay(FINISH_SHOPPING_DELAY_MS)
            jobs.joinAll()
            _isLoading.emit(false)
            onFinished()
        }
    }

    companion object {
        /**
         * 買い物を終えるときの待ち時間。
         *
         * 更新が速すぎるとスピナーが一瞬だけ光って消える。**本来は UI 層の関心**で、
         * ここに置いているのは暫定（技術改善バックログ T-27）。
         */
        internal const val FINISH_SHOPPING_DELAY_MS = 500L
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
