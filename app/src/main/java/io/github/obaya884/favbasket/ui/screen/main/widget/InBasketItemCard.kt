package io.github.obaya884.favbasket.ui.screen.main.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.obaya884.favbasket.data.item.Item

@Composable
fun InBasketItemCard(item: Item) {
    Text(
        text = item.name,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Preview(showBackground = true, widthDp = 240)
@Composable
fun InBasketItemCardPreview() {
    InBasketItemCard(
        item = Item(name = "Sample")
    )
}
