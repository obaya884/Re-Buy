package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.combine
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.domain.CategoryRepository
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    // nullにするべき？
    private val _items = MutableStateFlow<List<ItemWithCategory>>(listOf())
    private val _categories = MutableStateFlow<List<Category?>>(listOf())
    private val _isShowItemAddDialog = MutableStateFlow(false)
    private val _isShowItemEditDialog = MutableStateFlow(false)
    private val _isShowItemDeleteDialog = MutableStateFlow(false)
    private val _editingItem = MutableStateFlow<Item?>(null)

    val uiState: StateFlow<ItemEditScreenUiState> =
        combine(
            _items,
            _categories,
            _isShowItemAddDialog,
            _isShowItemEditDialog,
            _isShowItemDeleteDialog,
            _editingItem
        ) { items, categories, isShowItemAddDialog, isShowItemEditDialog, isShowItemDeleteDialog, editingItem ->
            ItemEditScreenUiState(
                items = items,
                categories = categories,
                isShowItemAddDialog = isShowItemAddDialog,
                isShowItemEditDialog = isShowItemEditDialog,
                isShowItemDeleteDialog = isShowItemDeleteDialog,
                editingItem = editingItem
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ItemEditScreenUiState(
                items = listOf(),
                categories = listOf(),
                isShowItemAddDialog = false,
                isShowItemEditDialog = false,
                isShowItemDeleteDialog = false,
                editingItem = null
            )
        )

    init {
        viewModelScope.launch {
            launch {
                itemRepository.getAllWithCategory()
                    .collect { items ->
                        _items.update { items }
                    }
            }
            launch {
                categoryRepository.getAll()
                    .collect { categories ->
                        _categories.update { listOf(null) + categories }
                    }
            }
        }
    }

    fun setEditingItem(item: Item) {
        viewModelScope.launch {
            _editingItem.emit(item)
        }
    }

    fun addItem(newItemName: String, categoryId: Int? = null) {
        val newItem = Item(
            name = newItemName,
            categoryId = categoryId
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

    fun editItemCategory(itemId: Int, newCategoryId: Int?) {
        viewModelScope.launch {
            itemRepository.updateCategory(itemId, newCategoryId)
        }
    }

    fun deleteItem() {
        uiState.value.editingItem?.let {
            viewModelScope.launch {
                itemRepository.delete(it)
            }
        }
    }

    fun showItemAddDialog() {
        viewModelScope.launch {
            _isShowItemAddDialog.emit(true)
        }
    }

    fun hideItemAddDialog() {
        viewModelScope.launch {
            _isShowItemAddDialog.emit(false)
        }
    }

    fun showItemEditDialog() {
        viewModelScope.launch {
            _isShowItemEditDialog.emit(true)
        }
    }

    fun hideItemEditDialog() {
        viewModelScope.launch {
            _isShowItemEditDialog.emit(false)
        }
    }

    fun showItemDeleteDialog() {
        viewModelScope.launch {
            _isShowItemDeleteDialog.emit(true)
        }
    }

    fun hideItemDeleteDialog() {
        viewModelScope.launch {
            _isShowItemDeleteDialog.emit(false)
        }
    }

}
