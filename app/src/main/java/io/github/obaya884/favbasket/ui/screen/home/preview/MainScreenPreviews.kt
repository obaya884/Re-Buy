package io.github.obaya884.favbasket.ui.screen.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.ui.screen.home.InBasketItemRow
import io.github.obaya884.favbasket.ui.screen.home.PreparedItemRow


@Preview(showBackground = true, widthDp = 240)
@Composable
fun InBasketItemCardPreview() {
    InBasketItemRow(
        item = Item(name = "Sample")
    )
}

@Preview(showBackground = true, widthDp = 240)
@Composable
fun PreparedItemCardPreview() {
    PreparedItemRow(
        item = Item(name = "Sample"),
        onCheckedChange = { }
    )
}
