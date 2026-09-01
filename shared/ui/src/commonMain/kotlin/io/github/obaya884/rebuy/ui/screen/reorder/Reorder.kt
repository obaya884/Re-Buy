package io.github.obaya884.rebuy.ui.screen.reorder

import kotlin.math.roundToInt

/**
 * ドラッグ量から**落とし先の位置**を出す（画面 09）。
 *
 * 行の高さは一定（画面 09。長い名前は 1 行に省略する）なので、動かした距離を行の間隔で
 * 割れば何行ぶん動いたかが出る。**半行を超えたところで隣と入れ替わる**——`roundToInt` が
 * その丸めをそのまま表す。
 *
 * @param fromIndex 掴んだときの位置
 * @param dragPx 掴んでからの移動量。下が正
 * @param rowPitchPx **行の高さ＋行間**。高さだけで割ると、動かすほど行数を多く数える。
 *   **0 以下なら動かさない**（測る前に呼ばれても壊れないように）
 * @param count 並びの件数。**末尾の破線行は数に入れない**——落とし先にはならない（画面 09）
 */
fun dropTargetIndex(fromIndex: Int, dragPx: Float, rowPitchPx: Float, count: Int): Int {
    if (count <= 0) return 0
    if (rowPitchPx <= 0f) return fromIndex.coerceIn(0, count - 1)

    val moved = (dragPx / rowPitchPx).roundToInt()
    return (fromIndex + moved).coerceIn(0, count - 1)
}

/**
 * [from] の要素を [to] へ移した並び。**間の要素は 1 つずつ押し出される**（詰まった連番の
 * 移動なので、入れ替えではない）。
 */
fun <T> List<T>.moveItem(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
