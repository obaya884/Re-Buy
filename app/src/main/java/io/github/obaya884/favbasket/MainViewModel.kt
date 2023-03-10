package io.github.obaya884.favbasket

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ItemInterface {
    val name: String
}

data class InBasketItem(
    override val name: String
) : ItemInterface

data class PreparedItem(
    override val name: String,
    val isInBasket: Boolean
) : ItemInterface

data class MainUiState(
    val inBasketItems: List<InBasketItem>,
    val preparedItems: List<PreparedItem>
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(
            inBasketItems = listOf(
                InBasketItem(name = "ゴミ袋"),
                InBasketItem(name = "箱ティッシュ")
            ),
            preparedItems = listOf(
                PreparedItem(name = "ゴミ袋", isInBasket = true),
                PreparedItem(name = "箱ティッシュ", isInBasket = false)
            )
        )
    )
    val uiState = _uiState.asStateFlow()

}