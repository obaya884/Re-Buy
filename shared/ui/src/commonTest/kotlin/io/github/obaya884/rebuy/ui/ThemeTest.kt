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

    /** 3 値とも「選ぶ → 保存 → 次の起動で読む」が往復すること。 */
    @Test
    fun 選ぶとその場で流れて次の起動でも同じ() = runTest {
        ThemePalette.entries.forEach { palette ->
            repository.select(palette)

            assertEquals(palette, repository.palette.first())
            assertEquals(palette.name, store.getString(key))
            assertEquals(palette, ThemeRepository(store).palette.first())
        }
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

    /** 6 通りが互いに別物であること。**若葉と柿の暗いほうはここでしか評価されない。** */
    @Test
    fun パレットと明暗の6通りがすべて別物() {
        val all = listOf(false, true).flatMap { dark ->
            ThemePalette.entries.map { reBuyColors(it, dark) }
        }

        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun パレットごとに面とアクセントが違う() {
        listOf(false, true).forEach { dark ->
            val colors = ThemePalette.entries.map { reBuyColors(it, dark) }

            assertEquals(3, colors.map { it.page }.toSet().size)
            assertEquals(3, colors.map { it.accent }.toSet().size)
            assertEquals(3, colors.map { it.accentSoft }.toSet().size)
        }
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
        listOf(false, true).forEach { dark ->
            val byPalette = ThemePalette.entries.map { reBuyColors(it, dark) }

            assertEquals(1, byPalette.map { it.ink }.toSet().size)
            assertEquals(1, byPalette.map { it.muted }.toSet().size)
            assertEquals(1, byPalette.map { it.danger }.toSet().size)
            assertEquals(1, byPalette.map { it.scrim }.toSet().size)
        }
    }

    /**
     * 共通トークンの値そのもの。**全画面の文字色がここで決まる**ので、8 値とも固定する
     * （パレット別の 42 値は上の関係性で見る）。
     */
    @Test
    fun 共通トークンは画面定義書の値() {
        val light = reBuyColors(ThemePalette.AI, darkTheme = false)
        val dark = reBuyColors(ThemePalette.AI, darkTheme = true)

        assertEquals(Color(0xFF232B21), light.ink)
        assertEquals(Color(0xFFE7ECE1), dark.ink)
        assertEquals(Color(0xFF6E7767), light.muted)
        assertEquals(Color(0xFF9BA492), dark.muted)
        assertEquals(Color(0xFFA8402E), light.danger)
        assertEquals(Color(0xFFE08A77), dark.danger)
        // 幕は rgba 指定。0-1 に写したものを見る
        assertEquals(Color(0.118f, 0.141f, 0.110f, 0.45f), light.scrim)
        assertEquals(Color(0.020f, 0.031f, 0.016f, 0.55f), dark.scrim)
    }
}
