package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.obaya884.rebuy.ui.R

@Composable
fun TextFieldEditDialog(
    title: String,
    editId: Int,
    editName: String,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputString by remember { mutableStateOf("") }

    SideEffect {
        inputString = editName
    }

    Dialog(
        onDismissRequest = {
            onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .defaultMinSize(minWidth = 200.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TextField(
                    value = inputString,
                    onValueChange = { inputString = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                        }
                    ) {
                        Text(
                            stringResource(id = R.string.text_field_edit_dialog_negative_button)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(editId, inputString)
                        }
                    ) {
                        Text(
                            stringResource(id = R.string.text_field_edit_dialog_positive_button)
                        )
                    }
                }
            }
        }
    }
}
