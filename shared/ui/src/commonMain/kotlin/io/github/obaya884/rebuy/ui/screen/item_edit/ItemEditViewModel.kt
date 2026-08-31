package io.github.obaya884.rebuy.ui.screen.item_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.ui.combine
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.applySaveResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItemEditViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<ItemWithCategory>>(listOf())
    private val _categories = MutableStateFlow<List<Category?>>(listOf())
    private val _isShowItemAddDialog = MutableStateFlow(false)
    private val _isShowItemEditDialog = MutableStateFlow(false)
    private val _isShowItemDeleteDialog = MutableStateFlow(false)
    private val _editingItem = MutableStateFlow<Item?>(null)
    private val _nameError = MutableStateFlow<NameError?>(null)

    /**
     * 名前が弾かれた理由。**確定のたびに更新される一過性の状態**で、入力欄の下にしか
     * 出ないので `uiState` には載せず、画面が直に見る。`null` は「まだ弾かれていない」。
     * 打ち直しでは消えない（消えるのは次の確定か、ダイアログを開き直したとき）。
     */
    val nameError: StateFlow<NameError?> = _nameError.asStateFlow()

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

    /** 弾かれたらダイアログを開いたままエラーを出す（画面定義書 §2）。 */
    fun addItem(newItemName: String, categoryId: Int? = null) {
        val newItem = Item(
            name = newItemName,
            categoryId = categoryId
        )
        viewModelScope.launch {
            _nameError.applySaveResult(itemRepository.insert(newItem)) { hideItemAddDialog() }
        }
    }

    fun editItemName(itemId: Int, newName: String) {
        viewModelScope.launch {
            _nameError.applySaveResult(itemRepository.updateName(itemId, newName)) {
                hideItemEditDialog()
            }
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
            _nameError.emit(null)
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
            _nameError.emit(null)
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
