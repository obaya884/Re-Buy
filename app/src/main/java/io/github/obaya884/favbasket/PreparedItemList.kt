package io.github.obaya884.favbasket

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PreparedItemList(
    contentPadding: PaddingValues,
    items: List<PreparedItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(contentPadding = contentPadding, modifier = modifier) {
        items(items) { item ->
            InBasketItemCard(item)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreparedItemListPreview() {
    InBasketItemList(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        items = listOf(
            InBasketItem(name = "ゴミ袋"),
            InBasketItem(name = "箱ティッシュ")
        )
    )
}