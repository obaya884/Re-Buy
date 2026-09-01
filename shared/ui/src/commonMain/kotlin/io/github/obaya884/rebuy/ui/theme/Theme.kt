package io.github.obaya884.rebuy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.obaya884.rebuy.domain.ThemePalette

/**
 * アプリのテーマ。**Material You の動的カラーは持たない**（3 パレットの見え方が壊れる）。
 *
 * 明暗は OS 設定に追従し、アプリ内に切り替えを置かない（画面定義書 §5・画面 08）。
 */
@Composable
fun ReBuyTheme(
    palette: ThemePalette = ThemePalette.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = reBuyColors(palette, darkTheme)

    CompositionLocalProvider(LocalReBuyColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            shapes = Shapes,
            typography = reBuyTypography(),
            content = content
        )
    }
}

/** 画面から配色トークンを引く入口。`MaterialTheme.colorScheme` より**こちらを使う**。 */
object ReBuyTheme {
    val colors: ReBuyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReBuyColors.current
}

/**
 * 既定値を置いているのは、`@Preview` が [ReBuyTheme] を通さずに部品を描くため。
 * 本番の経路は必ず [ReBuyTheme] を通る（`ReBuyApp` が全体を包む）。
 */
private val LocalReBuyColors = staticCompositionLocalOf {
    reBuyColors(ThemePalette.DEFAULT, darkTheme = false)
}

/**
 * Material のコンポーネント（`TextField`・`Button`・`TopAppBar` など）へ橋渡しする。
 *
 * **ロールの対応はここ 1 か所で決める。** 面は 2 段で、`background` と `surface` の
 * どちらにも地（[ReBuyColors.page]）を当てる——**アプリバーが拾うのは `surface`** なので、
 * ここを分けると本文との間に境目が出る（画面定義書 §5）。`surfaceContainer` 系は行・カード。
 * `primaryContainer` と `secondaryContainer` はどちらも選択面として使われるので
 * [ReBuyColors.accentSoft] を入れる。
 *
 * **画面定義書 §5 に無いロールは、いちばん近いトークンで埋める。** `onError` は
 * 危険色の上に載る文字（表に無いので [ReBuyColors.onAccent] を流用）、`onSurfaceVariant` は
 * 選択面の上の補助文字（[ReBuyColors.muted]）。**新しい画面はこれらではなくトークンを直に引く。**
 */
internal fun ReBuyColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = ink,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentSoft,
        onSecondaryContainer = ink,
        background = page,
        onBackground = ink,
        surface = page,
        onSurface = ink,
        surfaceVariant = accentSoft,
        onSurfaceVariant = muted,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = card,
        outline = line,
        outlineVariant = line,
        error = danger,
        onError = onAccent,
        scrim = scrim
    )
}
