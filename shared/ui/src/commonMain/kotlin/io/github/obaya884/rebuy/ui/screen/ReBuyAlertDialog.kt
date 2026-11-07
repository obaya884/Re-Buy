package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable

/**
 * アプリのダイアログ。`AlertDialog` に、端末の戻りで閉じる iOS の口（[IosEdgeSwipeDismissHandler]）を足す。
 *
 * **`AlertDialog` を直に呼ばない。** 呼んでも画面には出るが、iOS の端スワイプで閉じなくなる（画面定義書 §2）。
 */
@Composable
fun ReBuyAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text
    )
    IosEdgeSwipeDismissHandler(onDismiss = onDismissRequest)
}
