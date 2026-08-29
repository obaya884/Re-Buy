package io.github.obaya884.rebuy.ui.screen.category_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.domain.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(listOf())
    private val _isShowCategoryAddDialog = MutableStateFlow(false)
    private val _isShowCategoryEditDialog = MutableStateFlow(false)
    private val _isShowCategoryDeleteDialog = MutableStateFlow(false)
    private val _editingCategory = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<CategoryEditScreenUiState> =
        combine(
            _categories,
            _isShowCategoryAddDialog,
            _isShowCategoryEditDialog,
            _isShowCategoryDeleteDialog,
            _editingCategory
        ) { categories, isShowCategoryAddDialog, isShowCategoryEditDialog, isShowCategoryDeleteDialog, editingCategory ->
            CategoryEditScreenUiState(
                categories = categories,
                isShowCategoryAddDialog = isShowCategoryAddDialog,
                isShowCategoryEditDialog = isShowCategoryEditDialog,
                isShowCategoryDeleteDialog = isShowCategoryDeleteDialog,
                editingCategory = editingCategory
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            CategoryEditScreenUiState(
                categories = listOf(),
                isShowCategoryAddDialog = false,
                isShowCategoryEditDialog = false,
                isShowCategoryDeleteDialog = false,
                editingCategory = null
            )
        )

    init {
        viewModelScope.launch {
            categoryRepository.getAll()
                .collect { categories ->
                    _categories.update { categories }
                }
        }
    }

    fun addCategory(newCategoryName: String) {
        val newCategory = Category(
            name = newCategoryName,
        )
        viewModelScope.launch {
            categoryRepository.insert(newCategory)
        }
    }

    fun editCategoryName(categoryId: Int, newName: String) {
        viewModelScope.launch {
            categoryRepository.updateName(categoryId, newName)
        }
    }

    fun deleteCategory() {
        uiState.value.editingCategory?.let {
            viewModelScope.launch {
                categoryRepository.delete(it)
            }
        }
    }

    fun setEditingCategory(category: Category) {
        viewModelScope.launch {
            _editingCategory.emit(category)
        }
    }

    fun showCategoryAddDialog() {
        viewModelScope.launch {
            _isShowCategoryAddDialog.emit(true)
        }
    }

    fun hideCategoryAddDialog() {
        viewModelScope.launch {
            _isShowCategoryAddDialog.emit(false)
        }
    }

    fun showCategoryEditDialog() {
        viewModelScope.launch {
            _isShowCategoryEditDialog.emit(true)
        }
    }

    fun hideCategoryEditDialog() {
        viewModelScope.launch {
            _isShowCategoryEditDialog.emit(false)
        }
    }

    fun showCategoryDeleteDialog() {
        viewModelScope.launch {
            _isShowCategoryDeleteDialog.emit(true)
        }
    }

    fun hideCategoryDeleteDialog() {
        viewModelScope.launch {
            _isShowCategoryDeleteDialog.emit(false)
        }
    }

}
