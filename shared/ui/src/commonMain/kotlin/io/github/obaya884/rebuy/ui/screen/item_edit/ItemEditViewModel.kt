package io.github.obaya884.rebuy.ui.screen.item_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.applySaveResult
import io.github.obaya884.rebuy.ui.screen.ChipItem
import io.github.obaya884.rebuy.ui.screen.NewNameDialogState
import io.github.obaya884.rebuy.ui.screen.NewNameEditor
import io.github.obaya884.rebuy.ui.screen.NameTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * 品目編集シート（画面 06）。プール（01）の行の長押しで開く。
 *
 * **開く対象は [start] で渡す。** 登録シートと同じく ViewModel はシートより長生きするので、
 * 閉じるときに [reset] で捨てる（アーキテクチャ定義書 §4.3）。
 */
class ItemEditViewModel(
    private val itemRepository: ItemRepository,
    categoryRepository: CategoryRepository,
    destinationRepository: DestinationRepository
) : ViewModel() {

    private val editing = MutableStateFlow<EditingItem?>(null)
    private val nameError = MutableStateFlow<NameError?>(null)
    private val newNameEditor = NewNameEditor(categoryRepository, destinationRepository)
    private val _closeRequests = MutableStateFlow(0)

    /** 保存できた・削除できた回数。**変わったらシートを閉じる**。 */
    val closeRequests: StateFlow<Int> = _closeRequests

    val uiState: StateFlow<ItemEditSheetUiState> = combine(
        editing,
        nameError,
        newNameEditor.state,
        categoryRepository.getAll(),
        destinationRepository.getAll()
    ) { editing, error, dialog, categories, destinations ->
        ItemEditSheetUiState(
            editing = editing,
            nameError = error,
            categories = categories,
            destinations = destinations,
            newNameDialog = dialog
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ItemEditSheetUiState())

    /** 長押しされた品目で開く。**開くたびに前回の状態を捨てる。** */
    fun start(item: Item) {
        reset()
        editing.value = EditingItem(
            id = item.id,
            originalName = item.name,
            name = item.name,
            categoryId = item.categoryId,
            destinationId = item.destinationId,
            lastBoughtAt = item.lastBoughtAt
        )
    }

    fun reset() {
        editing.value = null
        nameError.value = null
        newNameEditor.dismiss()
        _closeRequests.value = 0
    }

    fun changeName(newName: String) {
        editing.value = editing.value?.copy(name = newName)
    }

    /** 同じチップをもう一度押すと外れる（画面定義書 §2）。「なし」チップでも外せる。 */
    fun selectCategory(categoryId: Int) {
        editing.update { current ->
            current?.copy(categoryId = categoryId.takeIf { it != current.categoryId })
        }
    }

    fun selectDestination(destinationId: Int) {
        editing.update { current ->
            current?.copy(destinationId = destinationId.takeIf { it != current.destinationId })
        }
    }

    fun clearCategory() {
        editing.value = editing.value?.copy(categoryId = null)
    }

    fun clearDestination() {
        editing.value = editing.value?.copy(destinationId = null)
    }

    /** 「保存」。名前・カテゴリ・行き先をまとめて反映する（重複判定から自分自身は除く）。 */
    fun save() {
        val current = editing.value ?: return
        viewModelScope.launch {
            val result = itemRepository.update(
                id = current.id,
                name = current.name,
                categoryId = current.categoryId,
                destinationId = current.destinationId
            )
            nameError.applySaveResult(result) { _closeRequests.value += 1 }
        }
    }

    /** 「削除する」。**物理削除で、戻せない**（データモデル定義書 §7）。 */
    fun delete() {
        val current = editing.value ?: return
        viewModelScope.launch {
            itemRepository.delete(current.id)
            _closeRequests.value += 1
        }
    }

    fun showNewNameDialog(target: NameTarget) = newNameEditor.show(target)

    fun changeNewName(newName: String) = newNameEditor.changeName(newName)

    fun dismissNewNameDialog() = newNameEditor.dismiss()

    fun createNewName() {
        viewModelScope.launch {
            newNameEditor.create { target, id ->
                editing.value = when (target) {
                    NameTarget.CATEGORY -> editing.value?.copy(categoryId = id)
                    NameTarget.DESTINATION -> editing.value?.copy(destinationId = id)
                }
            }
        }
    }
}

/**
 * 編集中の品目。**元の名前を持つ**のは、タイトルと削除の確認文言に使うため——
 * 入力中の名前で「「◯◯」を削除しますか？」と聞くと、まだ保存していない名前で確認することになる。
 */
data class EditingItem(
    val id: Int,
    val originalName: String,
    val name: String,
    val categoryId: Int?,
    val destinationId: Int?,
    val lastBoughtAt: Instant?
)

data class ItemEditSheetUiState(
    val editing: EditingItem? = null,
    val nameError: NameError? = null,
    val categories: List<Category> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val newNameDialog: NewNameDialogState? = null
) {
    // 1 度しか読まないので get()（アーキテクチャ定義書 §4.3）
    val categoryChips: List<ChipItem> get() = categories.map { ChipItem(it.id, it.name) }
    val destinationChips: List<ChipItem> get() = destinations.map { ChipItem(it.id, it.name) }
}
