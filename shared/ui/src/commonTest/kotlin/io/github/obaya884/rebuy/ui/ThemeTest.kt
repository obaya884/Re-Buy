package io.github.obaya884.rebuy.ui

import androidx.compose.ui.graphics.Color
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.theme.reBuyColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * テーマの選択と配色トークン（画面定義書 §5・画面 08、データモデル定義書 §9）。
 *
 * **色の値そのものは 1 か所だけリテラルで固定する**（テスト戦略定義書 §2.1）。
 * 21 色 × 6 通りを書き写すと、書き写しどうしが一致するだけになるので、
 * 残りは「パレットと明暗で別物になること」と「共通トークンは変わらないこと」で見る。
 */
class ThemeTest {

    /** 保存キー。**変えると選択済みの端末が既定へ戻る**ので、値そのものを固定する。 */
    private val key = "rebuy.theme.palette"

    private val store = FakeSettingsStore()
    private val repository = ThemeRepository(store)

    // ---- 選択と保存 ----

    @Test
    fun 未設定なら藍() = runTest {
        assertEquals(ThemePalette.AI, repository.palette.first())
    }

    @Test
    fun 選ぶとその場で流れて保存もされる() = runTest {
        repository.select(ThemePalette.KAKI)

        assertEquals(ThemePalette.KAKI, repository.palette.first())
        assertEquals("KAKI", store.getString(key))
    }

    @Test
    fun 保存済みの選択を起動時に読む() = runTest {
        val restored = ThemeRepository(FakeSettingsStore(mapOf(key to "WAKABA")))

        assertEquals(ThemePalette.WAKABA, restored.palette.first())
    }

    /** 名前を変えたり消したりした後の端末でも、**起動できて既定に倒れる**こと。 */
    @Test
    fun 知らない名前が入っていたら既定に倒す() = runTest {
        val broken = ThemeRepository(FakeSettingsStore(mapOf(key to "SAKURA")))

        assertEquals(ThemePalette.AI, broken.palette.first())
    }

    // ---- 配色トークン ----

    /** 表の値の入り口が合っていることを 1 か所で見る（既定の藍・ライト）。 */
    @Test
    fun 藍のライトは画面定義書の値() {
        val colors = reBuyColors(ThemePalette.AI, darkTheme = false)

        assertEquals(Color(0xFFEBEDF1), colors.page)
        assertEquals(Color(0xFF34558B), colors.accent)
        assertEquals(Color(0xFFE2E9F4), colors.accentSoft)
    }

    @Test
    fun パレットごとにアクセントが違う() {
        val accents = ThemePalette.entries.map { reBuyColors(it, darkTheme = false).accent }

        assertEquals(accents.size, accents.toSet().size)
    }

    @Test
    fun 明暗で面と文字が入れ替わる() {
        val light = reBuyColors(ThemePalette.AI, darkTheme = false)
        val dark = reBuyColors(ThemePalette.AI, darkTheme = true)

        assertNotEquals(light.page, dark.page)
        assertNotEquals(light.ink, dark.ink)
        assertTrue(dark.isDark)
        assertFalse(light.isDark)
    }

    /** 文字色・危険色はパレットで変わらない（画面定義書 §5 の「共通トークン」）。 */
    @Test
    fun 共通トークンはパレットで変わらない() {
        val ai = reBuyColors(ThemePalette.AI, darkTheme = false)
        val kaki = reBuyColors(ThemePalette.KAKI, darkTheme = false)

        assertEquals(ai.ink, kaki.ink)
        assertEquals(ai.muted, kaki.muted)
        assertEquals(ai.danger, kaki.danger)
    }
}
