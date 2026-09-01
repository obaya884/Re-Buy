package io.github.obaya884.rebuy.ui.screen.reorder

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ドラッグ並び替えの算術（画面 09）。**UI にも DB にも依存しない**ので、
 * 両プラットフォームで走るホスト段で全分岐を通す。
 *
 * ジェスチャそのもの（指を置いて動かして離す）は画面段でしか見られないが、
 * **どこへ落ちるかの判断はここが唯一の正**。
 */
class ReorderTest {

    private val rowHeight = 100f

    @Test
    fun 動かさなければ元の位置() {
        assertEquals(1, dropTargetIndex(fromIndex = 1, dragPx = 0f, rowHeightPx = rowHeight, count = 5))
    }

    /** **半行を超えたところで入れ替わる。** 半行ちょうどは次へ送る（`roundToInt` の丸め）。 */
    @Test
    fun 半行に満たなければまだ動かない() {
        assertEquals(1, dropTargetIndex(1, dragPx = 49f, rowHeightPx = rowHeight, count = 5))
    }

    @Test
    fun 半行を超えると1つ下へ() {
        assertEquals(2, dropTargetIndex(1, dragPx = 51f, rowHeightPx = rowHeight, count = 5))
    }

    @Test
    fun 上へも同じように動く() {
        assertEquals(0, dropTargetIndex(1, dragPx = -51f, rowHeightPx = rowHeight, count = 5))
        assertEquals(1, dropTargetIndex(1, dragPx = -49f, rowHeightPx = rowHeight, count = 5))
    }

    @Test
    fun 何行ぶんでも動く() {
        assertEquals(4, dropTargetIndex(0, dragPx = 400f, rowHeightPx = rowHeight, count = 5))
    }

    /** 一覧の外へは出ない。**末尾の破線行は落とし先にならない**（count に入れない）。 */
    @Test
    fun 下端より下へは行かない() {
        assertEquals(4, dropTargetIndex(2, dragPx = 9999f, rowHeightPx = rowHeight, count = 5))
    }

    @Test
    fun 上端より上へは行かない() {
        assertEquals(0, dropTargetIndex(2, dragPx = -9999f, rowHeightPx = rowHeight, count = 5))
    }

    /** 高さを測る前に呼ばれても壊れない。 */
    @Test
    fun 行の高さが未測定なら動かさない() {
        assertEquals(2, dropTargetIndex(2, dragPx = 500f, rowHeightPx = 0f, count = 5))
    }

    @Test
    fun 空の一覧では0を返す() {
        assertEquals(0, dropTargetIndex(0, dragPx = 500f, rowHeightPx = rowHeight, count = 0))
    }

    // ---- 並びの組み替え ----

    /** **入れ替えではなく押し出し。** 間の要素が 1 つずつずれる。 */
    @Test
    fun 下から上へ動かすと間の要素は下へずれる() {
        assertEquals(listOf("D", "A", "B", "C"), listOf("A", "B", "C", "D").moveItem(3, 0))
    }

    @Test
    fun 上から下へ動かすと間の要素は上へずれる() {
        assertEquals(listOf("B", "C", "A", "D"), listOf("A", "B", "C", "D").moveItem(0, 2))
    }

    @Test
    fun 同じ位置へ動かしても変わらない() {
        assertEquals(listOf("A", "B", "C"), listOf("A", "B", "C").moveItem(1, 1))
    }

    @Test
    fun 範囲の外を指されても壊れない() {
        assertEquals(listOf("A", "B"), listOf("A", "B").moveItem(0, 5))
        assertEquals(listOf("A", "B"), listOf("A", "B").moveItem(-1, 0))
    }
}
