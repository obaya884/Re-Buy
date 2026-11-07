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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import kotlinx.coroutines.launch

/**
 * シートの外枠（02・03・05・06・09b で共通）。見出し＋中身を縦に積む。
 *
 * **半開きにしない。** 既定だと下のほう（保存・削除・行）が画面の外に出て触れない（実測）。
 * 中身が画面に収まらないときのためにスクロールも入れてある。
 *
 * 閉じ方（スクリムタップ・下スワイプ・Android のシステムバック）は `ModalBottomSheet` に任せ、
 * すべて [onDismiss] を通す（画面定義書 §2）。**iOS の端スワイプだけは任せられない**ので
 * [IosEdgeSwipeDismissHandler] で受け、同じように畳んでから閉じる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReBuyBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
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
    IosEdgeSwipeDismissHandler(
        onDismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
        }
    )
}
