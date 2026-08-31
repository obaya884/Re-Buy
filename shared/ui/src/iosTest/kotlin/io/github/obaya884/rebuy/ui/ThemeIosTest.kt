package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * テーマ画面（画面 08）を実際に描いて操作する。
 *
 * ここでしか見られないのは 2 つ。**設定から辿り着けること**と、**タップした瞬間に
 * 選択が変わること**（保存ボタンが無いので、押し忘れではなく即時反映が仕様）。
 * 配色の値そのものは `commonTest` の `ThemeTest` が見る。
 *
 * 文言は実装と同じくリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ThemeIosTest {

    private val themeRepository: ThemeRepository
        get() = KoinPlatformTools.defaultContext().get().get()

    /**
     * 同梱した丸ゴシックが**見出しと CTA に実際に当たっている**こと（画面定義書 §5）。
     *
     * `BundledFontTest`（instrumented）はフォントが APK に載っていることまでしか見ない。
     * `Typography` から差し替えを外しても**画面は既定の書体で描かれて全件緑になる**ので
     * （変異で実測）、当たっていること自体はここで押さえる。
     *
     * **本文が既定のままであること**も同時に見る。本文用のフォントは同梱しないと決めた
     * （配布サイズが倍になる）ので、うっかり同じ書体を当てると約 3.8MB ぶんの判断が崩れる。
     */
    @Test
    fun 見出しは同梱した書体で本文は既定() = runComposeUiTest {
        var title: FontFamily? = null
        var cta: FontFamily? = null
        var body: FontFamily? = null
        setContent {
            ReBuyTheme {
                title = MaterialTheme.typography.titleLarge.fontFamily
                cta = MaterialTheme.typography.labelLarge.fontFamily
                body = MaterialTheme.typography.bodyLarge.fontFamily
            }
        }

        assertNotEquals(FontFamily.Default, title)
        assertNotEquals(FontFamily.Default, cta)
        assertEquals(FontFamily.Default, body)
    }

    @Test
    fun 設定からテーマを開いて選べる() = runComposeUiTest {
        // 選択は startTestKoin が既定へ戻す
        startTestKoin()
        setContent { ReBuyApp() }

        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        onNodeWithText("テーマ").performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("テーマ")
        onNodeWithText("柿").performClick()

        // 押した瞬間に選択が変わる（保存ボタンは無い）
        assertEquals(ThemePalette.KAKI, themeRepository.palette.value)
    }
}
