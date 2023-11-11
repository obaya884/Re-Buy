package io.github.obaya884.favbasket.ui.screen.main.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.obaya884.favbasket.data.item.Item

@Composable
fun InBasketItemCard(item: Item) {
    var checked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { checked = !checked }
            )
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            textAlign = TextAlign.Start,
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, widthDp = 240)
@Composable
fun InBasketItemCardPreview() {
    InBasketItemCard(
        item = Item(name = "Sample")
    )
}
