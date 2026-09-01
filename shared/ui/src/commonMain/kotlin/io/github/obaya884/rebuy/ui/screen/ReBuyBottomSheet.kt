package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/**
 * シートの外枠（02・03・06 で共通）。見出し＋中身を縦に積む。
 *
 * **半開きにしない。** 既定だと下のほう（保存・削除・行）が画面の外に出て触れない（実測）。
 * 中身が画面に収まらないときのためにスクロールも入れてある。
 *
 * 閉じ方（スクリムタップ・下スワイプ・システムバック）は `ModalBottomSheet` に任せ、
 * すべて [onDismiss] を通す（画面定義書 §2）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReBuyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ReBuyTheme.colors.ink
            )
            content()
        }
    }
}
