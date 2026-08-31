package io.github.obaya884.rebuy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 角丸は大きめに取る（画面定義書 §5）。行 13dp・CTA 15dp・シート上部 22dp が目安。
 *
 * Material のロールへは「小さい部品＝行、中＝ CTA、大＝シートやダイアログ」で当てる。
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(13.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(22.dp)
)
