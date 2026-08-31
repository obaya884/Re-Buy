package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TextFieldAddDialog(
    title: String,
    error: NameError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputString by remember { mutableStateOf("") }

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
                NameTextField(
                    value = inputString,
                    onValueChange = { inputString = it },
                    error = error
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
                            stringResource(Res.string.text_field_add_dialog_negative_button)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(inputString)
                        }
                    ) {
                        Text(
                            stringResource(Res.string.text_field_add_dialog_positive_button)
                        )
                    }
                }
            }
        }
    }
}

// private にしているのは、public だと iOS の framework（ReBuyUi）の公開ヘッダにも出るため
@Preview
@Composable
private fun TextFieldAddDialogPreview() {
    TextFieldAddDialog(
        title = "Add Item",
        error = null,
        onConfirm = {},
        onDismiss = {}
    )
}
