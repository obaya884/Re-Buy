package io.github.obaya884.favbasket.ui.screen.home

import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.ItemWithCategory

data class HomeScreenUiState(
    val categories: List<Category>,
    val inBasketItems: List<ItemWithCategory>,
    val preparedItems: List<ItemWithCategory>,
    val isAnimationPlaying: Boolean
)
