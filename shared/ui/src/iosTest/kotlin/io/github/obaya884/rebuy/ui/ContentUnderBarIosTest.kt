package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.screen.LocalBarOverlapTop
import io.github.obaya884.rebuy.ui.screen.LocalReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.MaterialAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.NoAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * **中身がバーの背後を通ること**（`docs/仕様/13_画面定義書.md` §6）。
 *
 * **上端を注入して本番 iOS の構成を再現する**（仕掛けと、測れずに残るものは
 * `LocalBarOverlapTop` の KDoc）。
 *
 * **背後を通るところを測る画面は 04。** 09 は `padding(scaffold)` が `verticalScroll` の内側に
 * あり、**Step 5 以前から中身がバーの下へ流れていた**ので、そこはこの変更の網にならない
 * （実測で判明）。09 で測るのは「先頭行が隠れないこと」のほう。
 */
@OptIn(ExperimentalTestApi::class)
class ContentUnderBarIosTest {

    /** 外枠が被さっている高さ。共有の入口と同じ値を使う（`IosTestApp.kt`）。 */
    private val overlap = BAR_OVERLAP_TOP

    /** 一覧の件数。**走査の範囲とスクロール先がこれで決まる**ので 1 か所に置く。 */
    private val rowCount = 20

    private val manyInBasket: FakeDatabase.() -> Unit = {
        seed(items = (1..rowCount).map { item(id = it, status = ItemStatus.IN_SHOPPING_LIST) })
    }

    private val someCategories: FakeDatabase.() -> Unit = {
        seed(categories = (1..5).map { category(id = it) })
    }

    /** 04 へ入る（カゴに行き先付きが無いので 03 を挟まない）。 */
    private fun ComposeUiTest.startShopping() {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        waitForIdle()
    }

    /**
     * 被さるぶんを 0 と [overlap] で入れ替えて、同じノードの上端がどれだけ動くかを測る。
     *
     * **「隠れない」だけを見ると足しすぎを取り逃す**（`>=` は二重取りにも成立する）ので、
     * 差がちょうど [overlap] であることまで見る。
     */
    private fun assertMovesDownByOverlap(
        what: String,
        prepare: FakeDatabase.() -> Unit = {},
        open: ComposeUiTest.(IosAppProbe) -> Unit = {},
        node: ComposeUiTest.() -> SemanticsNodeInteraction,
    ) {
        fun top(barOverlapTop: Dp): Dp {
            var top: Dp? = null
            runIosApp(prepare, barOverlapTop = barOverlapTop) { probe ->
                open(probe)
                top = node().getUnclippedBoundsInRoot().top
            }
            return requireNotNull(top) { "$what の位置を測れていない" }
        }

        val without = top(0.dp)
        val with = top(overlap)

        assertEquals(overlap, with - without, "$what の下がり方が被さるぶんと合わない")
    }

    /** 04 の行 1 つぶんの寸法。クリップの前後を並べて持つ。 */
    private data class RowBounds(
        val id: Int,
        val unclippedTop: Dp,
        val unclippedHeight: Dp,
        /** **実際に描かれている**上端。器で切られていればその縁になる。 */
        val clippedTop: Dp,
        val clippedHeight: Dp,
    )

    /** いま見えている 04 の行。**高さ 0 のものは外す**——完全に切られた行は `Rect.Zero` を返す。 */
    private fun ComposeUiTest.visibleShoppingRows(): List<RowBounds> =
        (1..rowCount).mapNotNull { id ->
            val tag = TestTags.shoppingRow(itemId = id)
            if (onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) return@mapNotNull null
            val unclipped = onNodeWithTag(tag).getUnclippedBoundsInRoot()
            val clipped = onNodeWithTag(tag).getBoundsInRoot()
            val clippedHeight = clipped.bottom - clipped.top
            if (clippedHeight <= 0.dp) null
            else RowBounds(id, unclipped.top, unclipped.bottom - unclipped.top, clipped.top, clippedHeight)
        }

    /**
     * **上端の余白は「素の値 ＋ 被さっているぶん」**。
     *
     * 04 の一覧は `contentPadding` に 16dp を持つので、先頭行はその下から始まる。
     * ここが 16 のままなら、被さっているぶんを一覧が持てていない。
     */
    @Test
    fun 一覧の先頭は被さるぶんだけ下から始まる() = runIosApp(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        startShopping()

        val top = onNodeWithTag(TestTags.shoppingRow(itemId = 1)).getUnclippedBoundsInRoot().top

        assertEquals(16.dp + overlap, top, "先頭行の上端が 16dp ＋ 被さるぶんになっていない")
    }

    /**
     * **スクロールすると、行がバーの帯の中に切られず描かれる**（§6 の本体）。
     *
     * ここが崩れると、バーの背後をコンテンツが通らず**屈折する対象が無いただの半透明の板**になる。
     * **「帯の中に行の座標がある」だけでは足りない**——外側で余白を受け取る実装でも、器の上端で
     * 切られた行の座標は帯に入りうる。**切られていないこと**（クリップ前後で高さが同じ）まで見る。
     */
    @Test
    fun スクロールすると行がバーの帯の中に描かれる() = runIosApp(manyInBasket) {
        startShopping()
        onNodeWithTag(TestTags.SHOPPING_LIST).performScrollToIndex(rowCount - 1)
        waitForIdle()

        // **帯の中から始まる行**を選ぶ。画面の上端を跨いだ行は端で切られるので外す
        val inBand = visibleShoppingRows().filter { it.unclippedTop >= 0.dp && it.unclippedTop < overlap }
        assertTrue(inBand.isNotEmpty(), "バーの帯から始まる行が無い（背後を通っていない）")

        val row = inBand.first()
        assertEquals(
            row.unclippedHeight,
            row.clippedHeight,
            "帯の中の行が切られている（外側で余白を受け取っている）: 行 ${row.id}"
        )
    }

    /**
     * **足す値が 0 なら、今までと同じ形**（Android の構成）。
     *
     * この案件は「Android の挙動と見え方を変えない」ことを制約に置いているのに、
     * **Android のテストが 1 件も網にならない**（iOS だけを触るため）。同じ `commonMain` の
     * コードを Android と同じ構成で走らせて、**行がアプリバーへ潜り込まないこと**をここで押さえる。
     */
    @Test
    fun 足す値が0なら行はアプリバーへ潜らない() =
        runIosApp(manyInBasket, barOverlapTop = 0.dp, delegate = MaterialAppBarRenderer) {
            startShopping()
            onNodeWithTag(TestTags.SHOPPING_LIST).performScrollToIndex(rowCount - 1)
            waitForIdle()

            val titleBottom = onNodeWithTag(TestTags.TOP_APP_BAR_TITLE)
                .getUnclippedBoundsInRoot().bottom
            // **描かれている位置で見る**——Android では器が切るので、真の座標は上にあってよい
            val top = visibleShoppingRows().minOf { it.clippedTop.value }.dp

            assertTrue(top >= titleBottom, "行が見出しより上に来ている（上端 $top・見出しの下端 $titleBottom）")
        }

    /**
     * **下端には漏れていない。** 被さっているぶんを全辺や下端に足すと、
     * 01・04 の下部 CTA がホームインジケータを避ける余白（§6）が動く。
     */
    @Test
    fun 被さるぶんは下端の余白を動かさない() {
        // **既定値を置かない。** 置くと、ブロックが assert に届かなかったときに両辺 0 で成立する
        fun ctaBottomGap(barOverlapTop: Dp): Float {
            var gap: Float? = null
            runIosApp(oneItem(ItemStatus.NO_DEAL), barOverlapTop = barOverlapTop) {
                val cta = onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON)
                    .getUnclippedBoundsInRoot()
                gap = onRoot().getUnclippedBoundsInRoot().bottom.value - cta.bottom.value
            }
            return requireNotNull(gap) { "CTA の位置を測れていない" }
        }

        assertEquals(ctaBottomGap(0.dp), ctaBottomGap(overlap), "上端の注入が下端へ漏れている")
    }

    /**
     * **流す一覧を持たない画面は、被さるぶんだけ下がる**（07・08。§6）。
     *
     * 07 と 08 は同じ形で外側から足すので、**片方だけ書き忘れる**のが典型的な壊れ方。2 画面とも通す。
     */
    @Test
    fun 設定の先頭行は被さるぶんだけ下がる() = assertMovesDownByOverlap(
        what = "07 の先頭行",
        open = { probe -> probe.tap(ReBuyAppBarIcon.SETTINGS) },
        node = { onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT) },
    )

    @Test
    fun テーマの先頭行は被さるぶんだけ下がる() = assertMovesDownByOverlap(
        what = "08 の先頭行",
        open = { probe ->
            probe.tap(ReBuyAppBarIcon.SETTINGS)
            onNodeWithTag(TestTags.SETTING_ROW_THEME).performClick()
            waitForIdle()
        },
        node = { onNodeWithText("若葉") },
    )

    /**
     * **09 の先頭行も隠れない。** 09 は中身がバーの下へ流れる画面だが、`scaffold` から上端が
     * 消えたぶんを内側で持てていないと、**1 行目がバーの下に潜ったまま**になる。
     */
    @Test
    fun 管理の先頭行は被さるぶんだけ下がる() = assertMovesDownByOverlap(
        what = "09 の先頭行",
        prepare = someCategories,
        open = { probe ->
            probe.tap(ReBuyAppBarIcon.SETTINGS)
            onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()
            waitForIdle()
        },
        node = { onNodeWithTag(TestTags.manageRow(1)) },
    )

    /**
     * **01 のチップ列もバーに隠れない**（§6）。
     *
     * 01 はチップ列が一覧とバーの間にいるので、**一覧はバーまで届かない**。
     */
    @Test
    fun プールのチップ列は被さるぶんだけ下がる() = assertMovesDownByOverlap(
        what = "01 のチップ列",
        prepare = oneItem(ItemStatus.NO_DEAL),
        node = { onNodeWithTag(TestTags.POOL_CHIP_ALL) },
    )

    /**
     * **01 の一覧は二重に下がらない。** チップ列が外側で受け取っているので、一覧が内側でも
     * 持つと 2 回下がる。チップ列と先頭行の間隔で見る。
     */
    @Test
    fun プールの一覧はチップ列の直下から始まる() {
        fun gap(barOverlapTop: Dp): Float {
            var gap: Float? = null
            runIosApp(oneItem(ItemStatus.NO_DEAL), barOverlapTop = barOverlapTop) {
                val chips = onNodeWithTag(TestTags.POOL_CHIP_ALL).getUnclippedBoundsInRoot()
                val row = onNodeWithTag(TestTags.poolRow(itemId = 1)).getUnclippedBoundsInRoot()
                gap = row.top.value - chips.bottom.value
            }
            return requireNotNull(gap) { "01 の間隔を測れていない" }
        }

        assertEquals(gap(0.dp), gap(overlap), "一覧が二重に下がっている")
    }

    /**
     * **シートは被さるぶんに動かされない。**
     *
     * シートは下端に貼り付いて外枠まで覆う（段 4 の着手前のスパイクで実測）。上端が被さっても
     * 位置は変わらないのが意図で、**ここが動くなら中身の配り方を間違えている**。
     */
    @Test
    fun シートは被さるぶんに動かされない() {
        fun sheetTop(barOverlapTop: Dp): Dp {
            var top: Dp? = null
            runIosApp(oneItem(ItemStatus.NO_DEAL), barOverlapTop = barOverlapTop) { probe ->
                probe.tap(ReBuyAppBarIcon.ADD)
                top = onNodeWithTag(TestTags.REGISTER_NAME_FIELD).getUnclippedBoundsInRoot().top
            }
            return requireNotNull(top) { "登録シートの位置を測れていない" }
        }

        assertEquals(sheetTop(0.dp), sheetTop(overlap), "シートが被さるぶんに動かされている")
    }

    /**
     * **iOS の入口が上端を渡している。**
     *
     * 外から別の値を provide しても、入口が自分で測った値（テストでは safe area ＝ 0）で
     * 上書きする。**provide の 1 行を落とすと外の値が素通りする**ので、そこだけは測れる
     * ——測れないのは「渡す値が実際の SwiftUI のバー高と一致すること」だけに縮む。
     */
    @Test
    fun 入口が上端を上書きする() = runComposeUiTest {
        startTestKoin(oneItem(ItemStatus.IN_SHOPPING_LIST))
        setContent {
            CompositionLocalProvider(LocalBarOverlapTop provides overlap) {
                ReBuyIosApp { }
            }
        }
        waitForIdle()
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        waitForIdle()

        val top = onNodeWithTag(TestTags.shoppingRow(itemId = 1)).getUnclippedBoundsInRoot().top

        assertEquals(16.dp, top, "入口が上端を provide していない（外の値が素通りしている）")
    }
}
