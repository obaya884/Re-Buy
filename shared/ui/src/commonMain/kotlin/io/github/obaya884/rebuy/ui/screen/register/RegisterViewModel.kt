package io.github.obaya884.rebuy.ui.screen.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.domain.SaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 登録シート（画面 02）と、その中の新規作成ダイアログ（02b）。
 *
 * **シートを閉じるかどうかは呼び出し側が決める。** ここは「閉じてよい」ことを
 * [closeRequests] で 1 回だけ知らせる——`nameError` と同じく、状態としては持たない。
 */
class RegisterViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    private val name = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<Int?>(null)
    private val selectedDestinationId = MutableStateFlow<Int?>(null)
    private val nameError = MutableStateFlow<NameError?>(null)
    private val newNameDialog = MutableStateFlow<NewNameDialogState?>(null)

    private val _closeRequests = MutableStateFlow(0)

    /** 「登録」で保存できた回数。**変わったらシートを閉じる**（`collect` 側の合図）。 */
    val closeRequests: StateFlow<Int> = _closeRequests

    val uiState: StateFlow<RegisterSheetUiState> = combine(
        combine(name, nameError, newNameDialog) { name, error, dialog -> Triple(name, error, dialog) },
        categoryRepository.getAll(),
        destinationRepository.getAll(),
        selectedCategoryId,
        selectedDestinationId
    ) { (name, error, dialog), categories, destinations, categoryId, destinationId ->
        RegisterSheetUiState(
            name = name,
            nameError = error,
            categories = categories,
            destinations = destinations,
            selectedCategoryId = categoryId,
            selectedDestinationId = destinationId,
            newNameDialog = dialog
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RegisterSheetUiState())

    fun changeName(newName: String) {
        name.value = newName
    }

    /** チップは各 0〜1 個。同じものをもう一度押すと外れる（画面 01 と同じ作法）。 */
    fun selectCategory(categoryId: Int) {
        selectedCategoryId.value =
            if (categoryId == selectedCategoryId.value) null else categoryId
    }

    fun selectDestination(destinationId: Int) {
        selectedDestinationId.value =
            if (destinationId == selectedDestinationId.value) null else destinationId
    }

    /** 「登録」。**登録直後の品目はカゴに入れない**（画面 02）。 */
    fun register() {
        save { _closeRequests.value += 1 }
    }

    /** 「続けて登録」。**名前だけ消して、チップの選択は残す**（画面 02）。 */
    fun registerAndContinue() {
        save { name.value = "" }
    }

    private fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val result = itemRepository.insert(
                Item(
                    name = name.value,
                    categoryId = selectedCategoryId.value,
                    destinationId = selectedDestinationId.value
                )
            )
            when (result) {
                is SaveResult.Saved -> {
                    nameError.value = null
                    onSaved()
                }

                is SaveResult.Rejected -> nameError.value = result.error
            }
        }
    }

    // ---- 02b 新しいカテゴリ／行き先ダイアログ ----

    fun showNewNameDialog(target: NewNameTarget) {
        newNameDialog.value = NewNameDialogState(target = target)
    }

    fun changeNewName(newName: String) {
        newNameDialog.value = newNameDialog.value?.copy(name = newName)
    }

    fun dismissNewNameDialog() {
        newNameDialog.value = null
    }

    /** 「作成」。**作ったものは呼び出し元のチップ列に選択済みで現れる**（画面 02b）。 */
    fun createNewName() {
        val dialog = newNameDialog.value ?: return
        viewModelScope.launch {
            val result = when (dialog.target) {
                NewNameTarget.CATEGORY -> categoryRepository.insert(dialog.name)
                NewNameTarget.DESTINATION -> destinationRepository.insert(dialog.name)
            }
            when (result) {
                is SaveResult.Saved -> {
                    when (dialog.target) {
                        NewNameTarget.CATEGORY -> selectedCategoryId.value = result.id
                        NewNameTarget.DESTINATION -> selectedDestinationId.value = result.id
                    }
                    newNameDialog.value = null
                }

                is SaveResult.Rejected -> {
                    newNameDialog.value = dialog.copy(error = result.error)
                }
            }
        }
    }
}

/** 02b が作る対象。カテゴリと行き先で作りが同じなので、違いはこれだけ。 */
enum class NewNameTarget { CATEGORY, DESTINATION }

data class NewNameDialogState(
    val target: NewNameTarget,
    val name: String = "",
    val error: NameError? = null
)

data class RegisterSheetUiState(
    val name: String = "",
    val nameError: NameError? = null,
    val categories: List<Category> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val selectedCategoryId: Int? = null,
    val selectedDestinationId: Int? = null,
    val newNameDialog: NewNameDialogState? = null
)
