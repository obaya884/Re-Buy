package io.github.obaya884.favbasket.ui.screen.shopping

import io.github.obaya884.favbasket.data.item.Item

data class ShoppingScreenUiState(
    val isLoading: Boolean,
    val inBasketItems: List<Item>,
    val scheduledBoughtItemIds: List<Int>,
    val isShowNavigateBackAlertDialog: Boolean,
    val isShowFinishShoppingAlertDialog: Boolean
)
