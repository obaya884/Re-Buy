package io.github.obaya884.rebuy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.obaya884.rebuy.domain.ThemePalette

/**
 * 画面が使う配色トークン（画面定義書 §5）。
 *
 * **素の Material のロールではなくこの語彙で書く。** 面が 2 段（[page] / [card]）
 * あることと、選択面とカゴ入り行が同じ [accentSoft] を使うことは Material のロール名では
 * 表せない。Material のコンポーネントへは [toColorScheme] で橋渡しする。
 */
@Immutable
data class ReBuyColors(
    /** 画面の地 */
    val page: Color,
    /** コンテンツ面 */
    /** 行・カード */
    val card: Color,
    /** 罫線・枠 */
    val line: Color,
    /** 主要色 */
    val accent: Color,
    /** 主要色の上に載る文字 */
    val onAccent: Color,
    /** 選択面・カゴ入り行 */
    val accentSoft: Color,
    /** 基本文字 */
    val ink: Color,
    /** 補助文字 */
    val muted: Color,
    /** 破壊的操作 */
    val danger: Color,
    /** シート背後の幕 */
    val scrim: Color,
    /** 明暗のどちら側か。**色見本のように「いまの明暗のまま別のパレットを引く」ときに使う** */
    val isDark: Boolean
)

// 共通トークン。パレットで変わらない（画面定義書 §5）
private val inkLight = Color(0xFF232B21)
private val inkDark = Color(0xFFE7ECE1)
private val mutedLight = Color(0xFF6E7767)
private val mutedDark = Color(0xFF9BA492)
private val dangerLight = Color(0xFFA8402E)
private val dangerDark = Color(0xFFE08A77)
private val scrimLight = Color(0.118f, 0.141f, 0.110f, 0.45f)
private val scrimDark = Color(0.020f, 0.031f, 0.016f, 0.55f)

/**
 * 3 パレット × 明暗。値の正は画面定義書 §5 の表で、**並び順も表の列と揃えてある**。
 *
 * 明暗はここで 1 度だけ分岐する。`colors` に渡すのはパレット別の 7 色だけで、
 * 共通トークン（文字色・危険色・幕）は [darkTheme] から決まる。
 */
fun reBuyColors(palette: ThemePalette, darkTheme: Boolean): ReBuyColors {
    fun colors(
        page: Long,
        card: Long,
        line: Long,
        accent: Long,
        onAccent: Long,
        accentSoft: Long
    ) = ReBuyColors(
        page = Color(page),
        card = Color(card),
        line = Color(line),
        accent = Color(accent),
        onAccent = Color(onAccent),
        accentSoft = Color(accentSoft),
        ink = if (darkTheme) inkDark else inkLight,
        muted = if (darkTheme) mutedDark else mutedLight,
        danger = if (darkTheme) dangerDark else dangerLight,
        scrim = if (darkTheme) scrimDark else scrimLight,
        isDark = darkTheme
    )

    return when (palette) {
        ThemePalette.WAKABA -> if (darkTheme) {
            colors(
                page = 0xFF171B15, card = 0xFF2A3127, line = 0xFF3A4234,
                accent = 0xFF74BD93, onAccent = 0xFF14201A, accentSoft = 0xFF2D3E33
            )
        } else {
            colors(
                page = 0xFFECEFE9, card = 0xFFFFFFFF, line = 0xFFDCE2D5,
                accent = 0xFF2E6B4A, onAccent = 0xFFF6FAF6, accentSoft = 0xFFE4EFE6
            )
        }

        ThemePalette.AI -> if (darkTheme) {
            colors(
                page = 0xFF14171C, card = 0xFF262B33, line = 0xFF3A414C,
                accent = 0xFF8FB0E3, onAccent = 0xFF111927, accentSoft = 0xFF2B3644
            )
        } else {
            colors(
                page = 0xFFEBEDF1, card = 0xFFFFFFFF, line = 0xFFDBDFE7,
                accent = 0xFF34558B, onAccent = 0xFFF4F7FB, accentSoft = 0xFFE2E9F4
            )
        }

        ThemePalette.KAKI -> if (darkTheme) {
            colors(
                page = 0xFF1C1712, card = 0xFF302921, line = 0xFF463B2E,
                accent = 0xFFDC9660, onAccent = 0xFF251507, accentSoft = 0xFF413228
            )
        } else {
            colors(
                page = 0xFFF1EDE6, card = 0xFFFFFFFF, line = 0xFFE5DCCE,
                accent = 0xFFB5541F, onAccent = 0xFFFCF7F2, accentSoft = 0xFFF5E6D8
            )
        }
    }
}
