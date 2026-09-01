package io.github.obaya884.rebuy.ui.screen

import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.domain.SaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * 新しいカテゴリ／行き先（画面 02b）の状態と保存。**02 と 06 が同じものを内包する**ので、
 * ViewModel に継承させず、部品として持たせる。
 *
 * 作れたときは [create] の `onCreated` で作った行の id を渡す——呼び出し元は
 * それを**選択済みにする**（画面 02b）。
 */
class NewNameEditor(
    private val categoryRepository: CategoryRepository,
    private val destinationRepository: DestinationRepository
) {
    private val _state = MutableStateFlow<NewNameDialogState?>(null)

    val state: StateFlow<NewNameDialogState?> = _state.asStateFlow()

    fun show(target: NameTarget) {
        _state.value = NewNameDialogState(target = target)
    }

    fun changeName(newName: String) {
        _state.value = _state.value?.copy(name = newName)
    }

    fun dismiss() {
        _state.value = null
    }

    suspend fun create(onCreated: (NameTarget, Int) -> Unit) {
        val dialog = _state.value ?: return
        val result = when (dialog.target) {
            NameTarget.CATEGORY -> categoryRepository.insert(dialog.name)
            NameTarget.DESTINATION -> destinationRepository.insert(dialog.name)
        }
        when (result) {
            is SaveResult.Saved -> {
                onCreated(dialog.target, result.id)
                _state.value = null
            }

            // 打っている途中の文字を巻き戻さないよう、退避ではなく今の値に付ける
            is SaveResult.Rejected -> _state.value = _state.value?.copy(error = result.error)
        }
    }
}

/**
 * カテゴリと行き先のどちら向きか。**2 つは形が同じで、違いはこれだけ**——
 * 02b（新規作成）でも 09（管理画面）でも、この 1 つで振り分ける。
 *
 * ルート（`Screen.Manage`）が持ち回るので保存・復元の対象になる。
 */
@Serializable
enum class NameTarget { CATEGORY, DESTINATION }

data class NewNameDialogState(
    val target: NameTarget,
    val name: String = "",
    val error: NameError? = null
)

