package io.github.obaya884.rebuy.ui.screen.item_edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.formatFullDate
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ChipRow
import io.github.obaya884.rebuy.ui.screen.NameTextField
import io.github.obaya884.rebuy.ui.screen.NewNameDialog
import io.github.obaya884.rebuy.ui.screen.NoneChip
import io.github.obaya884.rebuy.ui.screen.NewNameTarget
import io.github.obaya884.rebuy.ui.screen.ReBuyBottomSheet
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 品目編集シート（画面 06）。プール（01）の行の長押しで開く。
 *
 * **02 と同じ型**（名前入力欄＋カテゴリ・行き先のチップ列）に、最終購入日の表示と削除を足したもの。
 * 削除は確認ダイアログを挟み、**戻せないこと**を文言で伝える（画面 06）。
 */
@Composable
fun ItemEditSheet(item: Item, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<ItemEditViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val closeRequests by viewModel.closeRequests.collectAsState()
    var isDeleteDialogOpen by remember { mutableStateOf(false) }

    // 開いた対象が変わったら読み直す。閉じるときは reset で捨てる
    LaunchedEffect(item.id) { viewModel.start(item) }

    val dismiss = {
        viewModel.reset()
        onDismiss()
    }
    LaunchedEffect(closeRequests) {
        if (closeRequests > 0) dismiss()
    }

    val editing = uiState.editing ?: return

    // 見出しは**編集前の名前**。入力に追随させると、直している最中に見出しが揺れる
    ReBuyBottomSheet(title = editing.originalName, onDismiss = dismiss) {
        NameTextField(
            value = editing.name,
            onValueChange = viewModel::changeName,
            error = uiState.nameError,
            placeholder = stringResource(Res.string.item_form_name_placeholder),
            modifier = Modifier.testTag(TestTags.ITEM_SHEET_NAME_FIELD)
        )

        ChipRow(
            label = stringResource(Res.string.item_form_category_label),
            chips = uiState.categoryChips,
            selectedId = editing.categoryId,
            newLabel = stringResource(Res.string.item_form_new_category),
            onSelect = viewModel::selectCategory,
            onCreate = { viewModel.showNewNameDialog(NewNameTarget.CATEGORY) },
            newChipTag = TestTags.ITEM_FORM_NEW_CATEGORY_CHIP,
            chipTag = TestTags::itemFormCategoryChip,
            noneChip = NoneChip(
                label = stringResource(Res.string.item_sheet_none),
                tag = TestTags.ITEM_SHEET_CATEGORY_NONE_CHIP,
                onSelect = viewModel::clearCategory
            )
        )
        ChipRow(
            label = stringResource(Res.string.item_form_destination_label),
            chips = uiState.destinationChips,
            selectedId = editing.destinationId,
            newLabel = stringResource(Res.string.item_form_new_destination),
            onSelect = viewModel::selectDestination,
            onCreate = { viewModel.showNewNameDialog(NewNameTarget.DESTINATION) },
            newChipTag = TestTags.ITEM_FORM_NEW_DESTINATION_CHIP,
            chipTag = TestTags::itemFormDestinationChip,
            noneChip = NoneChip(
                label = stringResource(Res.string.item_sheet_none),
                tag = TestTags.ITEM_SHEET_DESTINATION_NONE_CHIP,
                onSelect = viewModel::clearDestination
            )
        )

        Text(
            text = editing.lastBoughtAt
                ?.let {
                    stringResource(Res.string.item_sheet_last_bought_at, formatFullDate(it))
                }
                ?: stringResource(Res.string.item_sheet_last_bought_at_never),
            style = MaterialTheme.typography.labelMedium,
            color = ReBuyTheme.colors.muted
        )

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.ITEM_SHEET_SAVE)
        ) {
            Text(stringResource(Res.string.item_sheet_save))
        }
        TextButton(
            onClick = { isDeleteDialogOpen = true },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.ITEM_SHEET_DELETE)
        ) {
            Text(
                text = stringResource(Res.string.item_sheet_delete, editing.originalName),
                color = ReBuyTheme.colors.danger
            )
        }
    }

    uiState.newNameDialog?.let { dialog ->
        NewNameDialog(
            state = dialog,
            onNameChange = viewModel::changeNewName,
            onCreate = viewModel::createNewName,
            onDismiss = viewModel::dismissNewNameDialog
        )
    }

    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = {
                Text(
                    stringResource(
                        Res.string.item_sheet_delete_dialog_title,
                        editing.originalName
                    )
                )
            },
            text = { Text(stringResource(Res.string.item_sheet_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteDialogOpen = false
                        viewModel.delete()
                    },
                    modifier = Modifier.testTag(TestTags.ITEM_SHEET_DELETE_CONFIRM)
                ) {
                    Text(
                        text = stringResource(Res.string.item_sheet_delete_dialog_confirm),
                        color = ReBuyTheme.colors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogOpen = false }) {
                    Text(stringResource(Res.string.item_sheet_delete_dialog_cancel))
                }
            }
        )
    }
}
