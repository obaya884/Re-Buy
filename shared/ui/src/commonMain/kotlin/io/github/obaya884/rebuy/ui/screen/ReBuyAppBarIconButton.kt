package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * アプリバーのアイコンボタン。**変えるのは的の大きさだけ**（`docs/仕様/13_画面定義書.md` §5）。
 *
 * **`IconButton` に `size` を渡さないこと。** 当たり判定は
 * `minimumInteractiveComponentSize()`（48dp）が別に確保していて、`size` が動かすのは
 * 状態レイヤ＝押した跡（既定 40dp）のほう。**判定だけを広げることはできない。**
 */
@Composable
fun ReBuyAppBarIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(APP_BAR_ICON_SIZE))
    }
}

/** 狙う的。既定の 24dp では小さいという指摘（FB-03）に対する値。 */
private val APP_BAR_ICON_SIZE = 28.dp
