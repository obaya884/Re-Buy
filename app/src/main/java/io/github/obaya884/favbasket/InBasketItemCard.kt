package io.github.obaya884.favbasket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun InBasketItemCard(item: ItemInterface) {
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
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@Preview(showBackground = true, widthDp = 240)
@Composable
fun InBasketItemCardPreview() {
    InBasketItemCard(item = object : ItemInterface {
        override val name: String = "Sample"
    })
}