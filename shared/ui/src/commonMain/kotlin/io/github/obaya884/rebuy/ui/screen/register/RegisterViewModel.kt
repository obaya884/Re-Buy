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
import io.github.obaya884.rebuy.ui.applySaveResult
import io.github.obaya884.rebuy.ui.screen.ChipItem
import io.github.obaya884.rebuy.ui.screen.NewNameDialogState
import io.github.obaya884.rebuy.ui.screen.NewNameEditor
import io.github.obaya884.rebuy.ui.screen.NewNameTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 登録シート（画面 02）と、その中の新規作成ダイアログ（02b）。
 *
 * **この ViewModel はシートより長生きする。** シートはプール（01）の中で条件付きに
 * 描かれるだけで、それ自体はナビゲーションの entry ではない——`koinViewModel` が引く
 * store はプールのもので、シートを閉じても破棄されない。
 *
 * そのため**閉じるときに [reset] で状態を捨てる**。これをしないと、2 回目に開いた瞬間に
 * [closeRequests] の残りで閉じてしまい、**二度と開けなくなる**（実測）。
 * 「保存されていない入力は破棄」（画面定義書 §2）も同じ経路で満たす。
 */
class RegisterViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    /** 入力中のもの。**「続けて登録」で名前だけ消す**のように、まとめて扱う場面が多い。 */
    private val input = MutableStateFlow(RegisterInput())
    private val nameError = MutableStateFlow<NameError?>(null)
    private val newNameEditor = NewNameEditor(categoryRepository, destinationRepository)

    private val _closeRequests = MutableStateFlow(0)

    /** 「登録」で保存できた回数。**変わったらシートを閉じる**（`collect` 側の合図）。 */
    val closeRequests: StateFlow<Int> = _closeRequests

    val uiState: StateFlow<RegisterSheetUiState> = combine(
        input,
        nameError,
        newNameEditor.state,
        categoryRepository.getAll(),
        destinationRepository.getAll()
    ) { input, error, dialog, categories, destinations ->
        RegisterSheetUiState(
            name = input.name,
            nameError = error,
            categories = categories,
            destinations = destinations,
            selectedCategoryId = input.categoryId,
            selectedDestinationId = input.destinationId,
            newNameDialog = dialog
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RegisterSheetUiState())

    fun changeName(newName: String) {
        input.value = input.value.copy(name = newName)
    }

    /** シートを閉じるときに呼ぶ。**入力も閉じる合図も捨てる。** */
    fun reset() {
        input.value = RegisterInput()
        nameError.value = null
        newNameEditor.dismiss()
        _closeRequests.value = 0
    }

    /** チップは各 0〜1 個。同じものをもう一度押すと外れる（画面 01 と同じ作法）。 */
    fun selectCategory(categoryId: Int) {
        input.value = input.value.copy(
            categoryId = categoryId.takeIf { it != input.value.categoryId }
        )
    }

    fun selectDestination(destinationId: Int) {
        input.value = input.value.copy(
            destinationId = destinationId.takeIf { it != input.value.destinationId }
        )
    }

    /** 「登録」。**登録直後の品目はカゴに入れない**（画面 02）。 */
    fun register() {
        save { _closeRequests.value += 1 }
    }

    /** 「続けて登録」。**名前だけ消して、チップの選択は残す**（画面 02）。 */
    fun registerAndContinue() {
        save { input.value = input.value.copy(name = "") }
    }

    private fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val current = input.value
            val result = itemRepository.insert(
                Item(
                    name = current.name,
                    categoryId = current.categoryId,
                    destinationId = current.destinationId
                )
            )
            nameError.applySaveResult(result, onSaved)
        }
    }

    // ---- 02b 新しいカテゴリ／行き先ダイアログ ----

    fun showNewNameDialog(target: NewNameTarget) = newNameEditor.show(target)

    fun changeNewName(newName: String) = newNameEditor.changeName(newName)

    fun dismissNewNameDialog() = newNameEditor.dismiss()

    /** 「作成」。**作ったものは呼び出し元のチップ列に選択済みで現れる**（画面 02b）。 */
    fun createNewName() {
        viewModelScope.launch {
            newNameEditor.create { target, id ->
                input.value = when (target) {
                    NewNameTarget.CATEGORY -> input.value.copy(categoryId = id)
                    NewNameTarget.DESTINATION -> input.value.copy(destinationId = id)
                }
            }
        }
    }
}

/** シートの入力。保存する 3 つをまとめて持つ。 */
private data class RegisterInput(
    val name: String = "",
    val categoryId: Int? = null,
    val destinationId: Int? = null
)

data class RegisterSheetUiState(
    val name: String = "",
    val nameError: NameError? = null,
    val categories: List<Category> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val selectedCategoryId: Int? = null,
    val selectedDestinationId: Int? = null,
    val newNameDialog: NewNameDialogState? = null
) {
    val categoryChips: List<ChipItem> = categories.map { ChipItem(it.id, it.name) }
    val destinationChips: List<ChipItem> = destinations.map { ChipItem(it.id, it.name) }
}

