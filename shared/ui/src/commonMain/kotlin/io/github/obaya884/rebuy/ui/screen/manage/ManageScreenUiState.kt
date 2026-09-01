package io.github.obaya884.rebuy.ui.screen.manage

import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.screen.NameTarget
import io.github.obaya884.rebuy.ui.screen.NewNameDialogState
import io.github.obaya884.rebuy.ui.screen.reorder.moveItem

/** 一覧の 1 件。カテゴリと行き先で使うのは id と名前だけ。 */
data class ManagedRecord(val id: Int, val name: String)

/** ドラッグ中。**掴んだ位置と、いま落ちる予定の位置**だけを持つ（px は画面が持つ）。 */
data class DragState(val fromIndex: Int, val toIndex: Int)

/**
 * 編集中の 1 件。**元の名前を持つ**のは、タイトルと削除の確認文言に使うため——
 * 入力中の名前で確認すると、まだ保存していない名前で聞くことになる（06 と同じ）。
 */
data class EditingRecord(val id: Int, val originalName: String, val name: String)

/**
 * 画面 09 と 09b の状態。
 *
 * **並びの見え方はここで作る。** ドラッグ中は [rows] が落とし先を当てた並びになり、
 * 画面はそれをそのまま描く（アーキテクチャ定義書 §4.3）。
 */
data class ManageScreenUiState(
    val target: NameTarget,
    val records: List<ManagedRecord> = emptyList(),
    val items: List<Item> = emptyList(),
    val drag: DragState? = null,
    val editing: EditingRecord? = null,
    val nameError: NameError? = null,
    val addDialog: NewNameDialogState? = null
) {
    /** 表示する並び。ドラッグ中は落とし先を当てた順。 */
    val rows: List<ManagedRecord> =
        drag?.let { records.moveItem(it.fromIndex, it.toIndex) } ?: records

    /** 掴んでいる行。持ち上げて描くのに使う。 */
    val draggingId: Int? = drag?.let { records.getOrNull(it.fromIndex)?.id }

    /**
     * 掴んだ行が**並びの上で何行ずれたか**。
     *
     * [rows] は落とし先を当てた並びなので、掴んだ行はすでにそのぶん動いた位置に描かれる。
     * 指に付いてくるように見せるには、**移動量からこのぶんを引く**——引かないと、
     * 落とし先が 1 つ変わるたびに行が 1 行ぶん飛び出す。
     */
    val draggingRowShift: Int = drag?.let { it.toIndex - it.fromIndex } ?: 0

    /** 空状態は破線行だけを出す（画面 09）。1 度しか読まないので get()（§4.3）。 */
    val isEmpty: Boolean get() = records.isEmpty()

    /**
     * 削除で影響を受ける品目の件数（画面 09b）。**0 のときは文言が変わる**ので、
     * 件数そのものを渡して呼び出し側で出し分ける。
     */
    val affectedItemCount: Int = editing?.let { record ->
        when (target) {
            NameTarget.CATEGORY -> items.count { it.categoryId == record.id }
            NameTarget.DESTINATION -> items.count { it.destinationId == record.id }
        }
    } ?: 0
}
