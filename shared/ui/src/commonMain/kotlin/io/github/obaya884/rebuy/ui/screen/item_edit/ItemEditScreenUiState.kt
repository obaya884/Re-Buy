package io.github.obaya884.rebuy.ui.screen.item_edit

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemWithCategory

data class ItemEditScreenUiState(
    val items: List<ItemWithCategory>,
    val categories: List<Category?>,
    val isShowItemAddDialog: Boolean,
    val isShowItemEditDialog: Boolean,
    val isShowItemDeleteDialog: Boolean,
    val editingItem: Item?
)
