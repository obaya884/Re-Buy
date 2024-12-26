package io.github.obaya884.favbasket.ui.screen.shopping

import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus

data class ShoppingScreenUiState(
    val isLoading: Boolean,
    val inShoppingListItems: List<Item>,
    val isShowNavigateBackAlertDialog: Boolean,
    val isShowFinishShoppingAlertDialog: Boolean
) {
    val isExistCheckedInShoppingListItems: Boolean
        get() = inShoppingListItems.any { it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
    val checkedInShoppingListItems: List<Item>
        get() = inShoppingListItems.filter { it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
}
