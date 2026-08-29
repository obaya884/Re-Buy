package io.github.obaya884.rebuy.ui.screen.shopping

import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.ui.screen.BottomNavigationScreenUiState

data class ShoppingScreenUiState(
    private val items: List<ItemWithCategory>,
    val isLoading: Boolean,
    val isShowFinishShoppingAlertDialog: Boolean
) : BottomNavigationScreenUiState {
    override val inShoppingListItems: List<ItemWithCategory>
        get() = items.filter { it.item.status == ItemStatus.IN_SHOPPING_LIST }

    val shoppingListItems = items.filter {
        it.item.status == ItemStatus.IN_SHOPPING_LIST
                || it.item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST
    }.map { it.item }
    val isExistCheckedInShoppingListItems: Boolean
        get() = shoppingListItems.any { it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
    val checkedInShoppingListItems: List<Item>
        get() = items.filter { it.item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST }
            .map { it.item }
}
