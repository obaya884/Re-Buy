package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 外枠が被さっているぶんの扱い（`docs/仕様/13_画面定義書.md` §6）のうち、**値として測れるところ**。
 *
 * **増えてよいのは上端だけ。** 全辺に足すと行が左右から狭まり、下端に足すと
 * **01・04 の下部 CTA がホームインジケータを避ける余白**（§6）が動く。
 *
 * **ここは画面を描かないので `commonTest` に置ける**（テスト戦略定義書 §1）。両プラットフォームの
 * ホスト段で走るので、**Android 側でもこの算術が固定される**。
 */
class ReBuyContentPaddingTest {

    private fun padding(barOverlapTop: Dp, all: Dp = 16.dp, scaffold: Dp = 0.dp) =
        ReBuyContentPadding(scaffold = PaddingValues(scaffold), barOverlapTop = barOverlapTop)
            .insideScroll(all = all)

    /** Android はここを通る。**足す値が 0 のときは今までと同じ値**。 */
    @Test
    fun 被さっていなければ全辺が素の値() {
        val result = padding(barOverlapTop = 0.dp)

        assertEquals(16.dp, result.calculateTopPadding())
        assertEquals(16.dp, result.calculateBottomPadding())
        assertEquals(16.dp, result.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, result.calculateEndPadding(LayoutDirection.Ltr))
    }

    @Test
    fun 被さっているぶんは上端にだけ足される() {
        val result = padding(barOverlapTop = 44.dp)

        assertEquals(16.dp + 44.dp, result.calculateTopPadding(), "上端に足されていない")
        assertEquals(16.dp, result.calculateBottomPadding(), "下端に漏れている（CTA の余白が動く）")
        assertEquals(16.dp, result.calculateStartPadding(LayoutDirection.Ltr), "左に漏れている")
        assertEquals(16.dp, result.calculateEndPadding(LayoutDirection.Ltr), "右に漏れている")
    }

    /** ライセンス画面は素の値を持たず、被さるぶんだけを渡す。 */
    @Test
    fun 素の値が0なら上端は被さるぶんだけ() {
        val result = padding(barOverlapTop = 44.dp, all = 0.dp)

        assertEquals(44.dp, result.calculateTopPadding())
        assertEquals(0.dp, result.calculateBottomPadding())
    }

    /**
     * **`Scaffold` が渡す枠は混ぜない。** 画面は枠を別の `padding` で当てているので、
     * ここで混ぜると二重になる。
     */
    @Test
    fun 枠の値は内側の余白に混ざらない() {
        val withFrame = padding(barOverlapTop = 44.dp, scaffold = 24.dp)
        val withoutFrame = padding(barOverlapTop = 44.dp, scaffold = 0.dp)

        assertEquals(withoutFrame.calculateTopPadding(), withFrame.calculateTopPadding())
        assertEquals(withoutFrame.calculateBottomPadding(), withFrame.calculateBottomPadding())
    }

    /**
     * **落とすのは上端だけ**（[ContentInsetSides]）。
     *
     * 幾何としては測れない条項だが、**辺の集合としてはここで測れる**。下端まで落とすと
     * **Android でも**下部 CTA がホームインジケータを避けなくなる（§6）。
     */
    @Test
    fun 中身へ渡す枠は上端だけを落とす() {
        val density = Density(1f)
        val insets = WindowInsets(left = 1, top = 10, right = 2, bottom = 20).only(ContentInsetSides)

        assertEquals(0, insets.getTop(density), "上端が渡っている")
        assertEquals(20, insets.getBottom(density), "下端まで落ちている（CTA がホームインジケータに重なる）")
        assertEquals(1, insets.getLeft(density, LayoutDirection.Ltr), "左が落ちている")
        assertEquals(2, insets.getRight(density, LayoutDirection.Ltr), "右が落ちている")
    }
}
