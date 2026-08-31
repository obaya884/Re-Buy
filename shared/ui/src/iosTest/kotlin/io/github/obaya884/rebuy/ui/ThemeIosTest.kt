package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.theme.ReBuyColors
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.reBuyColors
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    /**
     * 同梱した書体が **iOS の .app にも載っている**こと。バンドルの機構はプラットフォーム別で、
     * Android 側は instrumented の `BundledFontTest` が見る。`Font()` は遅延読み込みなので、
     * **載っていなくても既定の書体で描かれて緑になる**（宣言を見る下のテストでも捕まらない）。
     */
    @Test
    fun 同梱した書体がバンドルに載っている() = runBlocking {
        val bytes = Res.readBytes("font/zen_maru_gothic_bold.ttf")

        assertTrue(bytes.size > 1_000_000, "フォントが空か載っていない（${bytes.size} バイト）")
    }

    @Test
    fun 見出しは同梱した書体で本文は既定() = runComposeUiTest {
        var titleLarge: FontFamily? = null
        var titleMedium: FontFamily? = null
        var cta: FontFamily? = null
        var body: FontFamily? = null
        setContent {
            ReBuyTheme {
                titleLarge = MaterialTheme.typography.titleLarge.fontFamily
                titleMedium = MaterialTheme.typography.titleMedium.fontFamily
                cta = MaterialTheme.typography.labelLarge.fontFamily
                body = MaterialTheme.typography.bodyLarge.fontFamily
            }
        }
        waitForIdle()

        // **null チェックを先に置く。** `fontFamily` の行ごと落とす変異は `null` になり、
        // `assertNotEquals(FontFamily.Default, null)` はそれを通してしまう
        assertNotNull(titleLarge)
        assertNotNull(titleMedium)
        assertNotNull(cta)
        assertNotEquals(FontFamily.Default, titleLarge)
        assertNotEquals(FontFamily.Default, titleMedium)
        assertNotEquals(FontFamily.Default, cta)
        // 見出しと CTA は同じ書体
        assertEquals(titleLarge, cta)
        assertEquals(FontFamily.Default, body)
    }

    /**
     * 選んだパレットが `ReBuyTheme` の中まで届くこと。**`CompositionLocalProvider` を
     * 外しても既定の藍で描かれて全件緑になる**ので、ここで押さえる。
     */
    @Test
    fun 渡したパレットがトークンと配色に届く() = runComposeUiTest {
        var tokens: ReBuyColors? = null
        var primary: Color? = null
        setContent {
            ReBuyTheme(palette = ThemePalette.KAKI, darkTheme = false) {
                tokens = ReBuyTheme.colors
                primary = MaterialTheme.colorScheme.primary
            }
        }
        waitForIdle()

        val expected = reBuyColors(ThemePalette.KAKI, darkTheme = false)
        assertEquals(expected, tokens)
        assertEquals(expected.accent, primary)
    }

    @Test
    fun 設定からテーマを開いて選べる() = runComposeUiTest {
        startTestKoin()
        setContent { ReBuyApp() }

        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithText("テーマ").performClick()

        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("テーマ")

        // 事前状態を明示する。**ここを置かないと、前のテストの選択が残ったまま
        // タップが no-op でも最後の assert が通る**（Repository は single で、
        // 保存先を空にしても生成済みのインスタンスは読み直さない）
        assertEquals(ThemePalette.AI, themeRepository.palette.value)
        onNodeWithText("藍").assertIsSelected()

        onNodeWithText("柿").performClick()

        // 押した瞬間に選択が変わる（保存ボタンは無い）
        assertEquals(ThemePalette.KAKI, themeRepository.palette.value)
        onNodeWithText("柿").assertIsSelected()
        onNodeWithText("藍").assertIsNotSelected()
    }
}
