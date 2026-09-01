package io.github.obaya884.rebuy.ui.screen.register

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.NameTextField
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 登録シート（画面 02）。プール（01）のアプリバーの ＋ から開く。
 *
 * **名前だけで登録できる。** カテゴリと行き先は任意で、末尾のチップから 02b を開いて
 * その場で作れる。「続けて登録」は名前だけ消してチップの選択を残す——同じ売り場のものを
 * 続けて入れるときに、選び直さなくて済むように。
 *
 * 閉じ方（スクリムタップ・下スワイプ・システムバック）は `ModalBottomSheet` に任せる。
 * **保存していない入力は破棄**（画面定義書 §2）——ViewModel は画面ごとに作り直される。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSheet(onDismiss: () -> Unit) {
    val viewModel = koinViewModel<RegisterViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val closeRequests by viewModel.closeRequests.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val focusRequester = remember { FocusRequester() }

    // 「登録」で保存できたら閉じる。「続けて登録」では増えない
    LaunchedEffect(closeRequests) {
        if (closeRequests > 0) onDismiss()
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.register_title),
                style = MaterialTheme.typography.titleMedium,
                color = ReBuyTheme.colors.ink
            )

            NameTextField(
                value = uiState.name,
                onValueChange = viewModel::changeName,
                error = uiState.nameError,
                placeholder = stringResource(Res.string.register_name_placeholder),
                modifier = Modifier.focusRequester(focusRequester).testTag(TestTags.REGISTER_NAME_FIELD)
            )

            ChipRow(
                label = stringResource(Res.string.register_category_label),
                names = uiState.categories.map { it.id to it.name },
                selectedId = uiState.selectedCategoryId,
                newLabel = stringResource(Res.string.register_new_category),
                onSelect = viewModel::selectCategory,
                onCreate = { viewModel.showNewNameDialog(NewNameTarget.CATEGORY) },
                testTag = TestTags.REGISTER_NEW_CATEGORY_CHIP
            )
            ChipRow(
                label = stringResource(Res.string.register_destination_label),
                names = uiState.destinations.map { it.id to it.name },
                selectedId = uiState.selectedDestinationId,
                newLabel = stringResource(Res.string.register_new_destination),
                onSelect = viewModel::selectDestination,
                onCreate = { viewModel.showNewNameDialog(NewNameTarget.DESTINATION) },
                testTag = TestTags.REGISTER_NEW_DESTINATION_CHIP
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = viewModel::registerAndContinue,
                    modifier = Modifier.weight(1f).testTag(TestTags.REGISTER_SUBMIT_AND_CONTINUE)
                ) {
                    Text(stringResource(Res.string.register_submit_and_continue))
                }
                Button(
                    onClick = viewModel::register,
                    modifier = Modifier.weight(1f).testTag(TestTags.REGISTER_SUBMIT)
                ) {
                    Text(stringResource(Res.string.register_submit))
                }
            }
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
}

/** ラベル＋チップ列。末尾は必ず「＋ 新しい…」で、そこから 02b を開く。 */
@Composable
private fun ChipRow(
    label: String,
    names: List<Pair<Int, String>>,
    selectedId: Int?,
    newLabel: String,
    onSelect: (Int) -> Unit,
    onCreate: () -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ReBuyTheme.colors.muted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            names.forEach { (id, name) ->
                FilterChip(
                    selected = id == selectedId,
                    onClick = { onSelect(id) },
                    label = { Text(name) },
                    modifier = Modifier.testTag(TestTags.registerChip(testTag, id))
                )
            }
            FilterChip(
                selected = false,
                onClick = onCreate,
                label = { Text(newLabel) },
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

/**
 * 新しいカテゴリ／行き先（画面 02b）。名前入力欄だけの小さなダイアログ。
 *
 * **システムバックはダイアログだけを閉じる**（画面定義書 §2）——`Dialog` の既定の挙動。
 */
@Composable
private fun NewNameDialog(
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
                    NewNameTarget.CATEGORY -> stringResource(Res.string.register_dialog_category_title)
                    NewNameTarget.DESTINATION ->
                        stringResource(Res.string.register_dialog_destination_title)
                }
            )
        },
        text = {
            NameTextField(
                value = state.name,
                onValueChange = onNameChange,
                error = state.error,
                placeholder = stringResource(Res.string.register_name_placeholder),
                modifier = Modifier.testTag(TestTags.REGISTER_DIALOG_NAME_FIELD)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                modifier = Modifier.testTag(TestTags.REGISTER_DIALOG_CREATE)
            ) {
                Text(stringResource(Res.string.register_dialog_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.register_dialog_cancel))
            }
        }
    )
}
