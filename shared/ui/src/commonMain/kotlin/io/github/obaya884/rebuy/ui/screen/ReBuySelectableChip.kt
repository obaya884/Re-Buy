package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 選べるチップ。01 の絞り込みと、02・06 の「カテゴリ（任意）」「行き先（任意）」で共通。
 *
 * **選択中は先頭に ✓ を出す**（`docs/仕様/13_画面定義書.md` §2）。色だけで示さないのが条項。
 *
 * **選択で幅が ✓ のぶん伸びる**ので、右隣のチップがずれる。同時に ✓ が付くのは
 * 各群 0〜1 個まで。
 */
@Composable
fun ReBuySelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        modifier = modifier
    )
}
