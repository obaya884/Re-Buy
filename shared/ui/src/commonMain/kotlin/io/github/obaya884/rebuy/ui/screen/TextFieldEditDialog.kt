package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TextFieldEditDialog(
    title: String,
    editId: Int,
    editName: String,
    error: NameError?,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    // 開いた対象が変わったときだけ入れ直す。**再コンポーズのたびに state へ代入する
    // `SideEffect` は置かない**——書き込みが次の再コンポーズを呼ぶ形になっていた
    var inputString by remember(editId) { mutableStateOf(editName) }

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
                            stringResource(Res.string.text_field_edit_dialog_negative_button)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(editId, inputString)
                        }
                    ) {
                        Text(
                            stringResource(Res.string.text_field_edit_dialog_positive_button)
                        )
                    }
                }
            }
        }
    }
}
