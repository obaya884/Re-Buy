package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 画面の下端に置く CTA（01 の「買い物を始める」・04 の「買い物を終了する」）。
 *
 * 大きさは `docs/仕様/13_画面定義書.md` §5。**ダイアログやシートのボタンはここを通さない**
 * ——揃えるのは下部 CTA だけと決めている。
 */
@Composable
fun ReBuyBottomCta(
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            // **`height` ではなく下限で置く**。文字を大きくする設定で中身が入らなくなる
            .heightIn(min = BOTTOM_CTA_HEIGHT)
            .testTag(testTag),
        content = content
    )
}

/** 下部 CTA の高さ（画面定義書 §5）。Material のボタンの中間サイズの段に乗せた値。 */
private val BOTTOM_CTA_HEIGHT = 56.dp
