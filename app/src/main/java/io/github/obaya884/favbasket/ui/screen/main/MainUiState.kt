package io.github.obaya884.favbasket.ui.screen.main

import io.github.obaya884.favbasket.data.item.Item

data class MainUiState(
    val inBasketItems: List<Item>,
    val preparedItems: List<Item>
)
