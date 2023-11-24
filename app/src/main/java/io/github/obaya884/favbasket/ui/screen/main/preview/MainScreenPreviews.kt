package io.github.obaya884.favbasket.ui.screen.main.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.ui.screen.main.InBasketItemRow
import io.github.obaya884.favbasket.ui.screen.main.PreparedItemRow


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
