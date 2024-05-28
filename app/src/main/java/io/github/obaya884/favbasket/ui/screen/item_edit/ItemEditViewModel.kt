package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.domain.CategoryRepository
import io.github.obaya884.favbasket.domain.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<ItemWithCategory>>(listOf())
    val items: StateFlow<List<ItemWithCategory>> = _items

    private val _categories = MutableStateFlow<List<Category>>(listOf())
    val categories: StateFlow<List<Category>> = _categories

    init {
        viewModelScope.launch {
            launch {
                itemRepository.getAllWithCategory()
                    .collect { items ->
                        _items.value = items.reversed()
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

    fun editItemCategory(itemId: Int, newCategoryId: Int) {
        viewModelScope.launch {
            itemRepository.updateCategory(itemId, newCategoryId)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemRepository.delete(item)
        }
    }
}
