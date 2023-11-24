package io.github.obaya884.favbasket.ui.shared

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.obaya884.favbasket.R

@Composable
fun EditScreenItem(
    name: String,
    onTapCategory: (() -> Unit)? = null,
    onTapEdit: () -> Unit,
    onTapDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                textAlign = TextAlign.Start
            )
            if (onTapCategory != null)
                IconButton(
                    modifier = Modifier.align(Alignment.Bottom),
                    onClick = {
                        onTapCategory()
                    }
                ) {
                    Icon(
                        painterResource(id = R.drawable.icon_folder),
                        contentDescription = "Edit Category"
                    )
                }
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapEdit()
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
            }
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapDelete()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // TODO: implement the divider with the color of the theme
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .border(1.dp, Color.LightGray)
            )
        }
    }
}
