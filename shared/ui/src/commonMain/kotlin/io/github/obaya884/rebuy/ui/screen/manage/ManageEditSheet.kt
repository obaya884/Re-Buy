package io.github.obaya884.rebuy.ui.screen.manage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.NameTarget
import io.github.obaya884.rebuy.ui.screen.NameTextField
import io.github.obaya884.rebuy.ui.screen.ReBuyBottomSheet
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource

/**
 * カテゴリ／行き先の編集シート（画面 09b）。09 の行の長押しで開く。
 *
 * **06 と同じ型**（タイトル＝現在の名前、名前入力欄＋「保存」＋削除）。削除は確認ダイアログで
 * **影響を件数で示す**——品目は消えず、カテゴリなし／どこでも買えるものに戻るだけなので、
 * 何件が戻るのかが分かれば判断できる（画面 09b）。
 */
@Composable
fun ManageEditSheet(uiState: ManageScreenUiState, viewModel: ManageViewModel) {
    val editing = uiState.editing ?: return
    var isDeleteDialogOpen by remember { mutableStateOf(false) }

    ReBuyBottomSheet(
        // 見出しは**編集前の名前**。入力に追随させると直している最中に見出しが揺れる
        title = editing.originalName,
        onDismiss = viewModel::dismissEditing
    ) {
        NameTextField(
            value = editing.name,
            onValueChange = viewModel::changeName,
            error = uiState.nameError,
            placeholder = stringResource(Res.string.item_form_name_placeholder),
            modifier = Modifier.testTag(TestTags.MANAGE_SHEET_NAME_FIELD)
        )

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.MANAGE_SHEET_SAVE)
        ) {
            Text(stringResource(Res.string.manage_sheet_save))
        }
        TextButton(
            onClick = { isDeleteDialogOpen = true },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.MANAGE_SHEET_DELETE)
        ) {
            Text(
                text = stringResource(Res.string.manage_sheet_delete, editing.originalName),
                color = ReBuyTheme.colors.danger
            )
        }
    }

    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = {
                Text(
                    stringResource(
                        Res.string.manage_sheet_delete_dialog_title,
                        editing.originalName
                    )
                )
            },
            text = { Text(deleteMessage(uiState)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteDialogOpen = false
                        viewModel.delete()
                    },
                    modifier = Modifier.testTag(TestTags.MANAGE_SHEET_DELETE_CONFIRM)
                ) {
                    Text(
                        text = stringResource(Res.string.manage_sheet_delete_dialog_confirm),
                        color = ReBuyTheme.colors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogOpen = false }) {
                    Text(stringResource(Res.string.manage_sheet_delete_dialog_cancel))
                }
            }
        )
    }
}

/**
 * 削除で何が起きるか。**0 件のときは戻り先を言わない**——戻るものが無いのに
 * 「カテゴリなしになります」と言われても意味が取れない（画面 09b）。
 */
@Composable
private fun deleteMessage(uiState: ManageScreenUiState): String = when {
    uiState.affectedItemCount == 0 ->
        stringResource(Res.string.manage_sheet_delete_dialog_none)

    uiState.target == NameTarget.CATEGORY -> stringResource(
        Res.string.manage_sheet_delete_dialog_category,
        uiState.affectedItemCount
    )

    else -> stringResource(
        Res.string.manage_sheet_delete_dialog_destination,
        uiState.affectedItemCount
    )
}
