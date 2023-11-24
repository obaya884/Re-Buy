package io.github.obaya884.favbasket.ui.screen.main

import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.ItemWithCategory

data class MainUiState(
    val categories: List<Category>,
    val inBasketItems: List<ItemWithCategory>,
    val preparedItems: List<ItemWithCategory>
)
