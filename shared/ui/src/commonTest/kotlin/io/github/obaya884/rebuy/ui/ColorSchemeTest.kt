package io.github.obaya884.rebuy.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.ui.theme.reBuyColors
import io.github.obaya884.rebuy.ui.theme.toColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * 配色トークンから Material のロールへの対応（`toColorScheme`）。
 *
 * **既存画面の色はほぼ全部 `MaterialTheme.colorScheme` を通る**ので、ここが配色の実効経路。
 * 対応表の 1 行が落ちても画面は Material の既定色で描かれて動き続けるため、
 * 「トークンと一致すること」と「既定色が残っていないこと」の両方を見る。
 */
class ColorSchemeTest {

    private val palette = ThemePalette.AI
    private val colors = reBuyColors(palette, darkTheme = false)
    private val scheme = colors.toColorScheme()

    @Test
    fun 画面が引くロールがトークンと一致する() {
        assertEquals(colors.accent, scheme.primary)
        assertEquals(colors.onAccent, scheme.onPrimary)
        assertEquals(colors.accentSoft, scheme.primaryContainer)
        assertEquals(colors.accent, scheme.secondary)
        assertEquals(colors.accentSoft, scheme.secondaryContainer)
        assertEquals(colors.page, scheme.background)
        assertEquals(colors.ink, scheme.onBackground)
        // 地は 1 段。**アプリバーが拾う surface も地**（画面定義書 §5）
        assertEquals(colors.page, scheme.surface)
        assertEquals(colors.ink, scheme.onSurface)
        assertEquals(colors.accentSoft, scheme.surfaceVariant)
        assertEquals(colors.muted, scheme.onSurfaceVariant)
        assertEquals(colors.line, scheme.outline)
        assertEquals(colors.line, scheme.outlineVariant)
        assertEquals(colors.danger, scheme.error)
        assertEquals(colors.scrim, scheme.scrim)
    }

    /** 行・カードは面の 3 段目。`surfaceContainer` 系はどれも同じ面を指す。 */
    @Test
    fun 行とカードの面がすべてcardになる() {
        assertEquals(colors.card, scheme.surfaceContainerLowest)
        assertEquals(colors.card, scheme.surfaceContainerLow)
        assertEquals(colors.card, scheme.surfaceContainer)
        assertEquals(colors.card, scheme.surfaceContainerHigh)
        assertEquals(colors.card, scheme.surfaceContainerHighest)
    }

    /**
     * **Material の既定色が 1 つも残っていないこと。** 対応表から 1 行落ちると、
     * その部品だけ紫の既定色で描かれる——値の一致だけを見ていると、
     * 落ちた行の分は「見ていないロール」として素通りする。
     */
    @Test
    fun 上書きしたロールに既定色が残っていない() {
        val default = lightColorScheme()

        checkedRoles.forEach { (name, role) ->
            assertNotEquals(role(default), role(scheme), "$name が Material の既定色のまま")
        }
    }

    /** 見ているロールの一覧。**足したら上の 2 つのテストにも足すこと。** */
    private val checkedRoles: List<Pair<String, (ColorScheme) -> Color>> = listOf(
        "primary" to { it.primary },
        "onPrimary" to { it.onPrimary },
        "primaryContainer" to { it.primaryContainer },
        "secondary" to { it.secondary },
        "secondaryContainer" to { it.secondaryContainer },
        "background" to { it.background },
        "onBackground" to { it.onBackground },
        "surface" to { it.surface },
        "onSurface" to { it.onSurface },
        "surfaceVariant" to { it.surfaceVariant },
        "onSurfaceVariant" to { it.onSurfaceVariant },
        "surfaceContainer" to { it.surfaceContainer },
        "outline" to { it.outline },
        "outlineVariant" to { it.outlineVariant },
        "error" to { it.error },
        "scrim" to { it.scrim }
    )
}
