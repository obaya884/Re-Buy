package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 登録シート（02）と品目編集シート（06）が共通で使う部品。
 *
 * **2 つのシートは「名前＋カテゴリ・行き先のチップ列」という同じ型**を持つ（画面 06 が
 * 「02 と同じ型」と定めている）。違うのは初期値・確定のボタン・削除の有無だけなので、
 * 型の部分をここに置く。
 */

/**
 * ラベル＋チップ列。末尾は必ず「＋ 新しい…」で、そこから 02b を開く。
 *
 * [noneLabel] を渡すと先頭に「なし」チップが出る（画面 06。**選択を外す道を明示する**）。
 */
@Composable
fun ChipRow(
    label: String,
    chips: List<ChipItem>,
    selectedId: Int?,
    newLabel: String,
    onSelect: (Int) -> Unit,
    onCreate: () -> Unit,
    newChipTag: String,
    chipTag: (Int) -> String,
    noneLabel: String? = null,
    onSelectNone: () -> Unit = {},
    noneChipTag: String = ""
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
            noneLabel?.let {
                FilterChip(
                    selected = selectedId == null,
                    onClick = onSelectNone,
                    label = { Text(it) },
                    modifier = Modifier.testTag(noneChipTag)
                )
            }
            chips.forEach { chip ->
                FilterChip(
                    selected = chip.id == selectedId,
                    onClick = { onSelect(chip.id) },
                    label = { Text(chip.label) },
                    modifier = Modifier.testTag(chipTag(chip.id))
                )
            }
            FilterChip(
                selected = false,
                onClick = onCreate,
                label = { Text(newLabel) },
                modifier = Modifier.testTag(newChipTag)
            )
        }
    }
}

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
