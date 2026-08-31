package io.github.obaya884.rebuy.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.Shapes
import io.github.obaya884.rebuy.ui.theme.tabularNumbers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 形状と数字の書式（画面定義書 §5）。どちらも値そのものが仕様なのでリテラルで固定する。
 *
 * 書体は `Font()` が `@Composable` なので画面段（`iosTest` の `ThemeIosTest`）で見る。
 */
class ShapeAndTypeTest {

    /** 行 13dp・CTA 15dp・シート上部 22dp。 */
    @Test
    fun 角丸は画面定義書の目安どおり() {
        assertEquals(RoundedCornerShape(13.dp), Shapes.extraSmall)
        assertEquals(RoundedCornerShape(13.dp), Shapes.small)
        assertEquals(RoundedCornerShape(15.dp), Shapes.medium)
        assertEquals(RoundedCornerShape(22.dp), Shapes.large)
        assertEquals(RoundedCornerShape(22.dp), Shapes.extraLarge)
    }

    /**
     * 等幅数字は OpenType の `tnum`。**CSS 記法の `tabular-nums` を書いても無音で効かない**ので、
     * 綴りそのものを固定する。
     */
    @Test
    fun 等幅数字はtnumで指定する() {
        assertEquals("tnum", TextStyle().tabularNumbers().fontFeatureSettings)
    }

    /** 元のスタイルは変えない（字の大きさや色を巻き添えにしない）。 */
    @Test
    fun 等幅数字は他の指定を変えない() {
        val base = TextStyle(fontFeatureSettings = null)

        assertEquals(base.copy(fontFeatureSettings = "tnum"), base.tabularNumbers())
    }
}
