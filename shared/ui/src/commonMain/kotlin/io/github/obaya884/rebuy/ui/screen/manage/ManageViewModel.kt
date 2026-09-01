package io.github.obaya884.rebuy.ui.screen.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.applySaveResult
import io.github.obaya884.rebuy.ui.combine
import io.github.obaya884.rebuy.ui.screen.NameTarget
import io.github.obaya884.rebuy.ui.screen.NewNameDialogState
import io.github.obaya884.rebuy.ui.screen.NewNameEditor
import io.github.obaya884.rebuy.ui.screen.reorder.dropTargetIndex
import io.github.obaya884.rebuy.ui.screen.reorder.moveItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * カテゴリの管理／行き先の管理（画面 09）と、その編集シート（09b）。
 *
 * **2 つは同型の画面**なので、向きは [target] だけで決まる（画面定義書 画面 09）。
 * 一覧は名前と id しか使わないので、両方を [ManagedRecord] に均してから扱う。
 *
 * ドラッグ中の px は画面が持ち、ここへ来るのは**確定した位置**だけ（[startDrag]・[dragTo]）。
 * **離すまで DB に書かない**（画面 09 の「離した時点で保存」）。
 */
class ManageViewModel(
    private val categoryRepository: CategoryRepository,
    private val destinationRepository: DestinationRepository,
    itemRepository: ItemRepository,
    private val target: NameTarget
) : ViewModel() {

    private val newNameEditor = NewNameEditor(categoryRepository, destinationRepository)

    /**
     * いまの一覧。**ドラッグの最中に同期で読みたい**のでここへ流す——`uiState` は
     * `stateIn` 越しで、指を動かした直後・離した直後には 1 つ前の並びが返る（実測）。
     */
    private val records = MutableStateFlow<List<ManagedRecord>>(emptyList())
    private val drag = MutableStateFlow<DragState?>(null)
    private val editing = MutableStateFlow<EditingRecord?>(null)
    private val nameError = MutableStateFlow<NameError?>(null)

    init {
        viewModelScope.launch { recordsFlow().collect { records.value = it } }
    }

    val uiState: StateFlow<ManageScreenUiState> = combine(
        records,
        itemRepository.getAll(),
        drag,
        editing,
        nameError,
        newNameEditor.state
    ) { records, items, drag, editing, error, addDialog ->
        ManageScreenUiState(
            target = target,
            records = records,
            items = items,
            drag = drag,
            editing = editing,
            nameError = error,
            addDialog = addDialog
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ManageScreenUiState(target = target))

    // ---- 並び替え ----

    fun startDrag(index: Int) {
        drag.value = DragState(fromIndex = index, toIndex = index)
    }

    /**
     * ドラッグ中の落とし先。**ここでは書かない**——指を動かすたびに DB を叩かない。
     *
     * 画面から来るのは画素の値だけで、**どこへ落ちるかの判断はここで下す**
     * （`dropTargetIndex`）。掴んだ位置は自分が持っているので、画面が覚え直さなくてよい。
     *
     * @param dragPx 掴んでからの移動量
     * @param rowPitchPx 行の高さ＋行間
     */
    fun dragBy(dragPx: Float, rowPitchPx: Float) {
        val current = drag.value ?: return
        drag.value = current.copy(
            toIndex = dropTargetIndex(
                fromIndex = current.fromIndex,
                dragPx = dragPx,
                rowPitchPx = rowPitchPx,
                count = records.value.size
            )
        )
    }

    /**
     * 指を離した。**ここで初めて保存する**（画面 09）。
     *
     * 途中で切れたとき（別の指が触れた・通話で中断）も `draggable` はここへ来る。
     * **そのときも最後に見えていた並びで保存する**——見えている並びと保存が食い違うより、
     * 見えたとおりに残るほうが驚かない。
     */
    fun endDrag() {
        val current = drag.value ?: return
        drag.value = null
        if (current.fromIndex == current.toIndex) return
        val orderedIds = records.value.moveItem(current.fromIndex, current.toIndex).map { it.id }
        viewModelScope.launch { updateOrder(orderedIds) }
    }

    // ---- 09b 編集シート ----

    /** 行の長押しで開く。**開くたびに前回の状態を捨てる。** */
    fun startEditing(record: ManagedRecord) {
        nameError.value = null
        editing.value = EditingRecord(id = record.id, originalName = record.name, name = record.name)
    }

    fun changeName(newName: String) {
        editing.value = editing.value?.copy(name = newName)
    }

    /** シートを閉じるときに呼ぶ。打ちかけの名前とエラーを捨てる（画面定義書 §2）。 */
    fun dismissEditing() {
        editing.value = null
        nameError.value = null
    }

    /** 「保存」。弾かれたら閉じない（画面定義書 §2）。 */
    fun save() {
        val current = editing.value ?: return
        viewModelScope.launch {
            val result = when (target) {
                NameTarget.CATEGORY -> categoryRepository.updateName(current.id, current.name)
                NameTarget.DESTINATION -> destinationRepository.updateName(current.id, current.name)
            }
            nameError.applySaveResult(result) { dismissEditing() }
        }
    }

    /** 「削除する」。**紐づく品目は消えない**（データモデル定義書 §7）。 */
    fun delete() {
        val current = editing.value ?: return
        viewModelScope.launch {
            when (target) {
                NameTarget.CATEGORY -> categoryRepository.delete(current.id)
                NameTarget.DESTINATION -> destinationRepository.delete(current.id)
            }
            dismissEditing()
        }
    }

    // ---- 02b 追加ダイアログ ----

    /** 末尾の破線行。**作った行は末尾に現れる**（画面 09。採番は Repository が持つ）。 */
    fun showAddDialog() = newNameEditor.show(target)

    fun changeNewName(newName: String) = newNameEditor.changeName(newName)

    fun dismissAddDialog() = newNameEditor.dismiss()

    fun createNewName() {
        viewModelScope.launch { newNameEditor.create { _, _ -> } }
    }

    private fun recordsFlow(): Flow<List<ManagedRecord>> = when (target) {
        NameTarget.CATEGORY ->
            categoryRepository.getAll().map { list -> list.map { ManagedRecord(it.id, it.name) } }

        NameTarget.DESTINATION ->
            destinationRepository.getAll().map { list -> list.map { ManagedRecord(it.id, it.name) } }
    }

    private suspend fun updateOrder(orderedIds: List<Int>) = when (target) {
        NameTarget.CATEGORY -> categoryRepository.updateOrder(orderedIds)
        NameTarget.DESTINATION -> destinationRepository.updateOrder(orderedIds)
    }
}
