package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/**
 * ラベル＋チップ列。**登録シート（02）と品目編集シート（06）が共通で使う**
 * （画面 06 が「02 と同じ型」と定めている）。末尾は必ず「＋ 新しい…」で、そこから 02b を開く。
 *
 * [noneChip] を渡すと先頭に「なし」チップが出る（画面 06。**選択を外す道を明示する**）。
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
    noneChip: NoneChip? = null
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
            noneChip?.let {
                FilterChip(
                    selected = selectedId == null,
                    onClick = it.onSelect,
                    label = { Text(it.label) },
                    modifier = Modifier.testTag(it.tag)
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
 * チップ 1 つぶん。カテゴリと行き先は形が同じなので、**チップ列は同じ型で扱う**
 * （どちらを描いているかは呼び出し側が知っている）。
 */
data class ChipItem(val id: Int, val label: String)

/**
 * 先頭の「なし」チップ。**3 つは揃って初めて意味を持つ**ので 1 つにまとめる
 * （タグだけ渡し忘れると、テストから掴めない理由が見えなくなる）。
 */
data class NoneChip(val label: String, val tag: String, val onSelect: () -> Unit)
