package io.github.obaya884.rebuy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.zen_maru_gothic_bold
import org.jetbrains.compose.resources.Font

/**
 * 書体（画面定義書 §5）。**見出しと CTA だけ丸ゴシックを同梱**し、本文は OS 既定に任せる。
 *
 * 同梱するのは Zen Maru Gothic の Bold 1 ウェイトだけ（約 3.8MB）。日本語フォントは
 * 1 ウェイトでこの大きさなので、**目に付く場所に効く見出しと CTA に絞る**という判断
 * （経緯は `log_13_画面定義書.md`）。ライセンス（OFL-1.1）は一覧に手で足してある。
 *
 * `Font()` が `@Composable` なので、`Typography` も composable として組む。
 */
@Composable
fun reBuyTypography(): Typography {
    val display = FontFamily(Font(Res.font.zen_maru_gothic_bold, FontWeight.Bold))
    val body = FontFamily.Default

    return Typography(
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp
        ),
        // CTA（ボタンの文字）
        labelLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        // 補助（最終購入日・カテゴリー名）。見出しでも CTA でもないので本文側
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    )
}

/**
 * 等幅数字にする（画面定義書 §5）。件数・日付・進捗のように**桁が揃っていてほしい**
 * ところで使う。書体側の `tnum` フィーチャを有効にするだけなので、字面は変わらない。
 */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
