package io.github.obaya884.rebuy.ui.screen.category_edit

import io.github.obaya884.rebuy.data.category.Category

data class CategoryEditScreenUiState(
    val categories: List<Category>,
    val isShowCategoryAddDialog: Boolean,
    val isShowCategoryEditDialog: Boolean,
    val isShowCategoryDeleteDialog: Boolean,
    val editingCategory: Category?
)
