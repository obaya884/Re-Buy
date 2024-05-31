package io.github.obaya884.favbasket.ui.screen.item_edit

import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemWithCategory

data class ItemEditScreenUiState(
    val items: List<ItemWithCategory>,
    val categories: List<Category>,
    val isShowItemAddDialog: Boolean,
    val isShowItemEditDialog: Boolean,
    val isShowItemDeleteDialog: Boolean,
    val editingItem: Item?
)
