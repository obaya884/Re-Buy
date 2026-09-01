package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 新しいカテゴリ／行き先（画面 02b）。名前入力欄だけの小さなダイアログ。
 *
 * **システムバックはダイアログだけを閉じる**（画面定義書 §2）——`Dialog` の既定の挙動。
 *
 * 旧 `TextFieldAddDialog` と形は似ているが寄せていない。あちらは入力を自分で持つ作りで、
 * ここは**入力も検証の結果も ViewModel が持つ**（弾かれたら開いたまま理由を出す）。
 * あちらは F-012 で 09b に置き換わって消えるので、そこまでの相乗りを避けた。
 */
@Composable
fun NewNameDialog(
    state: NewNameDialogState,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (state.target) {
                    NewNameTarget.CATEGORY -> stringResource(Res.string.item_form_dialog_category_title)
                    NewNameTarget.DESTINATION ->
                        stringResource(Res.string.item_form_dialog_destination_title)
                }
            )
        },
        text = {
            NameTextField(
                value = state.name,
                onValueChange = onNameChange,
                error = state.error,
                placeholder = stringResource(Res.string.item_form_name_placeholder),
                modifier = Modifier.testTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                modifier = Modifier.testTag(TestTags.ITEM_FORM_DIALOG_CREATE)
            ) {
                Text(stringResource(Res.string.item_form_dialog_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.item_form_dialog_cancel))
            }
        }
    )
}
