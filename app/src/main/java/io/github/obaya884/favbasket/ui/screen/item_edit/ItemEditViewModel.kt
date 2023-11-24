package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(listOf())
    val items: StateFlow<List<Item>> = _items

    init {
        viewModelScope.launch {
            itemRepository.getAll()
                .collect { items ->
                    _items.value = items
                }
        }
    }

    fun addItem(newItemName: String) {
        val newItem = Item(
            name = newItemName,
        )
        viewModelScope.launch {
            itemRepository.insert(newItem)
        }
    }

    fun editItemName(itemId: Int, newName: String) {
        viewModelScope.launch {
            itemRepository.updateName(itemId, newName)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemRepository.delete(item)
        }
    }
}
