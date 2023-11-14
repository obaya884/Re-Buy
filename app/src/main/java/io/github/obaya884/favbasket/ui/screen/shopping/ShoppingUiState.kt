package io.github.obaya884.favbasket.ui.screen.shopping

import io.github.obaya884.favbasket.data.item.Item

data class ShoppingUiState(
    val inBasketItems: List<Item>,
    val scheduledBoughtItemIds: List<Int>
)
