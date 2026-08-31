package io.github.obaya884.rebuy.ui.screen.category_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.applySaveResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryEditViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(listOf())
    private val _isShowCategoryAddDialog = MutableStateFlow(false)
    private val _isShowCategoryEditDialog = MutableStateFlow(false)
    private val _isShowCategoryDeleteDialog = MutableStateFlow(false)
    private val _editingCategory = MutableStateFlow<Category?>(null)
    private val _nameError = MutableStateFlow<NameError?>(null)

    /**
     * 名前が弾かれた理由。**確定のたびに更新される一過性の状態**で、入力欄の下にしか
     * 出ないので `uiState` には載せず、画面が直に見る。`null` は「まだ弾かれていない」。
     * 打ち直しでは消えない（消えるのは次の確定か、ダイアログを開き直したとき）。
     */
    val nameError: StateFlow<NameError?> = _nameError.asStateFlow()

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

    /** 弾かれたらダイアログを開いたままエラーを出す（画面定義書 §2）。 */
    fun addCategory(newCategoryName: String) {
        viewModelScope.launch {
            _nameError.applySaveResult(categoryRepository.insert(newCategoryName)) {
                hideCategoryAddDialog()
            }
        }
    }

    fun editCategoryName(categoryId: Int, newName: String) {
        viewModelScope.launch {
            _nameError.applySaveResult(categoryRepository.updateName(categoryId, newName)) {
                hideCategoryEditDialog()
            }
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
            _nameError.emit(null)
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
            _nameError.emit(null)
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
