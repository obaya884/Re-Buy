package io.github.obaya884.rebuy.ui.screen.home

import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.ui.screen.BottomNavigationScreenUiState

data class HomeScreenUiState(
    val categories: List<Category>,
    val items: List<ItemWithCategory>
) : BottomNavigationScreenUiState {
    val inBasketItems
        get() = items.filter { it.item.status != ItemStatus.NO_DEAL }
    override val inShoppingListItems: List<ItemWithCategory>
        get() = items.filter { it.item.status == ItemStatus.IN_SHOPPING_LIST }
}
