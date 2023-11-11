package io.github.obaya884.favbasket.ui.screen.category_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.domain.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(listOf())
    val categories: StateFlow<List<Category>> = _categories

    init {
        viewModelScope.launch {
            categoryRepository.getAll()
                .collect { categories ->
                    _categories.value = categories
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

}
