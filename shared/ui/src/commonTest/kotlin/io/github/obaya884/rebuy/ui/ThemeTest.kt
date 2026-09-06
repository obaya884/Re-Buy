package io.github.obaya884.rebuy.ui

import androidx.compose.ui.graphics.Color
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.theme.ReBuyColors
import io.github.obaya884.rebuy.ui.theme.reBuyColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.math.pow
import kotlin.math.round
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
        assertEquals(Color(0xFFB6C4DA), colors.accentSoft)
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

    // ---- 選択面のコントラスト（画面定義書 §5・FB-16） ----

    /**
     * **`card` は `page` の上に浮いて見える**（画面定義書 §5「地は 1 段。その上に `card` が浮く」）。
     *
     * **下の下限テストの土台。** 選択面の下限は「`card` が `page` に浮く比」を基準にするので、
     * **この 2 つが近づくと下限も一緒に緩む**——極端には同色で下限が 1.0 に退化し、
     * 「`page` と 1 ビットでも違えば緑」になる。だから土台の側にも絶対の網を置く。
     */
    @Test
    fun cardはpageの上に浮いて見える() {
        assertAllPalettes { colors ->
            val ratio = contrastRatio(colors.card, colors.page)
            buildList {
                if (ratio <= SURFACE_STEP_FLOOR) {
                    add("card と page の比 ${round2(ratio)} が $SURFACE_STEP_FLOOR 以下")
                }
            }
        }
    }

    /**
     * **選択面は `card` が `page` に浮く比より大きく離れている**（画面定義書 §5 の下限）。
     *
     * 比べる相手は役目で違う——**選択チップは地（`page`）の上、カゴ入り行が並ぶのは
     * 隣の白い行（`card`）**。ライトはチップ側、ダークは行側が弱くなるので両方見る。
     *
     * **下限に固有の数値を置かない。** 「`card` が `page` に浮く比」そのものを下限にするので、
     * 配色を作り直しても条件が付いてくる。
     */
    @Test
    fun 選択面は地とも行ともcardがpageに浮く比より離れている() {
        assertAllPalettes { colors ->
            val floor = contrastRatio(colors.card, colors.page)
            buildList {
                listOf(
                    "地" to contrastRatio(colors.accentSoft, colors.page),
                    "行" to contrastRatio(colors.accentSoft, colors.card)
                ).forEach { (relation, ratio) ->
                    if (ratio <= floor) {
                        add("${relation}との比 ${round2(ratio)} が浮く比 ${round2(floor)} 以下")
                    }
                }
            }
        }
    }

    /**
     * **選択面の上の基本文字は AA を満たす**（画面定義書 §5 の上限）。
     *
     * 下限と対で「濃くしすぎない」側を押さえる。**これが無いと `accent-soft` を `accent`
     * そのものにしても、残りの網（下限と `藍のライトは画面定義書の値`）では
     * 6 通り中 5 通りが緑で通る**——下限は 6 通りとも通ってしまう（実測）。
     *
     * **補助文字（`muted`）はここに含めない**——カゴ入り行の上で AA を割っており、
     * どう扱うかは FB-17 が持つ（画面定義書 §5）。
     */
    @Test
    fun 選択面の上の基本文字はAAを満たす() {
        assertAllPalettes { colors ->
            val ratio = contrastRatio(colors.ink, colors.accentSoft)
            buildList {
                if (ratio < AA_CONTRAST) add("ink との比 ${round2(ratio)} が AA の $AA_CONTRAST 未満")
            }
        }
    }

    /**
     * **コントラスト比の式そのものを固定する。** 上の 2 つは比の大小しか見ないので、
     * **式が間違っていても不変条件が守られているように見える方向に転ぶ**（変異で実測）。
     *
     * **3 件要る。** 灰色は無彩色なので**線形化の抜けだけ**を捕まえ、重み付けの誤りは通す。
     * 赤（G=B=0）が押さえるのは **R の重みだけ**で、**G と B の入れ替えは赤も灰色も素通りする**
     * ——青を足して B を押さえると、和が 1 なので G も決まる。
     *
     * **白と黒は錨にならない**（線形化を恒等に落とす変異に対して。21 のまま動かない）。
     *
     * **低域の枝（`c <= 0.03928`）には錨を置かない**——本アプリの 42 トークンは
     * 1 チャネルも低域に入らない（いちばん暗い `#14171C` でも 0.0706）ので、
     * ここを壊しても本番の色は 1 つも動かない。**意図して空けている穴**。
     */
    @Test
    fun コントラスト比の式が正しい() {
        // 線形化を落とすと 2.05 になる
        assertEquals(4.54, contrastRatio(Color(0xFF767676), Color.White), absoluteTolerance = 0.01)
        // 重み付けを等分にすると 2.74。R と G の入れ替えは 1.37
        assertEquals(4.00, contrastRatio(Color.Red, Color.White), absoluteTolerance = 0.01)
        // G と B の入れ替えは 1.37。**赤と灰色だけでは素通りする**
        assertEquals(8.59, contrastRatio(Color.Blue, Color.White), absoluteTolerance = 0.01)
    }

    /**
     * 6 通りすべてを見て、**破りを集めてから 1 度に落とす**。
     * 1 件目で止めると、6 値を一斉に差し替えたときに 1 パレットずつしか分からない。
     *
     * どのパレット・明暗かの前置は**ここで付ける**ので、[violations] は測る中身だけを書く。
     */
    private fun assertAllPalettes(violations: (ReBuyColors) -> List<String>) {
        val found = listOf(false, true).flatMap { dark ->
            ThemePalette.entries.flatMap { palette ->
                violations(reBuyColors(palette, dark)).map { "$palette dark=$dark の$it" }
            }
        }
        assertTrue(found.isEmpty(), found.joinToString("\n"))
    }

    /** 失敗メッセージ用。**生の `Double` を 6 行並べると読めない。** */
    private fun round2(value: Double): Double = round(value * 100) / 100

    /**
     * [WCAG の相対輝度](https://www.w3.org/TR/WCAG21/#dfn-relative-luminance)。
     * **`Color` の成分は既に 0〜1** なので、8bit へ戻さずそのまま線形化する。
     *
     * **アルファは見ない。** 不透明なトークン専用で、`scrim` に当てると黙って誤った値を返す。
     */
    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }

    /** 明るいほう ÷ 暗いほう。順序は問わない。 */
    private fun contrastRatio(a: Color, b: Color): Double {
        val one = relativeLuminance(a)
        val other = relativeLuminance(b)
        return (maxOf(one, other) + 0.05) / (minOf(one, other) + 0.05)
    }

    private companion object {
        /** [WCAG 1.4.3](https://www.w3.org/TR/WCAG21/#contrast-minimum) の本文の下限。 */
        const val AA_CONTRAST = 4.5

        /**
         * `card` が `page` に浮いていると言える下限。**実測は 1.16〜1.30**（画面定義書 §5）。
         *
         * **面が縮退していないことだけを見る値**なので、実測より 1 段低く取る——
         * 実測に張り付けると、地の色を少し触るたびに鳴る。
         */
        const val SURFACE_STEP_FLOOR = 1.1
    }
}
