package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.tabularNumbers

/**
 * アプリバーに添える数（01 の「全 n 件」、04 の進捗「x / n」）。等幅数字で組む。
 *
 * **右の余白はここが持つ**（`docs/仕様/13_画面定義書.md` §5）。空ける先は画面で違い、
 * 01 は右隣の ＋、04 はバーの右端。**呼ぶ側からは打ち消せない。**
 */
@Composable
fun ReBuyAppBarCount(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.tabularNumbers(),
        color = ReBuyTheme.colors.muted,
        modifier = modifier.padding(end = APP_BAR_COUNT_END_SPACE)
    )
}

/** 右に空ける間。 */
private val APP_BAR_COUNT_END_SPACE = 16.dp
