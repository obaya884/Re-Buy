package io.github.obaya884.favbasket

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun InBasketItemList(
    contentPadding: PaddingValues,
    items: List<InBasketItem>
) {
    LazyColumn(contentPadding = contentPadding) {
        items(items) { item ->
            InBasketItemCard(item)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InBasketItemListPreview() {
    InBasketItemList(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        items = listOf(
            InBasketItem(name = "ゴミ袋"),
            InBasketItem(name = "箱ティッシュ")
        )
    )
}