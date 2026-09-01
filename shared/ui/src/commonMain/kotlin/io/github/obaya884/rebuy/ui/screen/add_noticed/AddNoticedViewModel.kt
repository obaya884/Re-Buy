package io.github.obaya884.rebuy.ui.screen.add_noticed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.data.item.isInBasket
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.applySaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 気づいたものを足すシート（画面 05）。買い物モード（04）の一覧の末尾から開く。
 *
 * **どの経路も 1 件で閉じる**（画面 05）。足すのは「思い出した 1 件」なので、
 * 続けて足したいときは開き直すほうが、選んだものが一覧に現れたのを確かめてから進める。
 *
 * **この ViewModel はシートより長生きする**（store は 04 の entry のもの）。
 * 閉じるときに [reset] で状態を捨てないと、2 回目に開いた瞬間に閉じる合図の残りで
 * 閉じてしまい、二度と開けなくなる（`RegisterViewModel` で実測）。
 */
class AddNoticedViewModel(
    private val itemRepository: ItemRepository,
    destinationRepository: DestinationRepository,
    private val destinationId: Int?
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val nameError = MutableStateFlow<NameError?>(null)
    private val _closeRequest = MutableStateFlow(CloseRequest())

    /** 閉じる合図。**何回目か**と、**足した先を知らせるかどうか**を一緒に運ぶ。 */
    val closeRequest: StateFlow<CloseRequest> = _closeRequest

    val uiState: StateFlow<AddNoticedSheetUiState> = combine(
        query,
        nameError,
        itemRepository.getAll(),
        destinationRepository.getAll()
    ) { query, error, items, destinations ->
        AddNoticedSheetUiState(
            destinationId = destinationId,
            query = query,
            nameError = error,
            items = items,
            destinations = destinations
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AddNoticedSheetUiState(destinationId = destinationId)
    )

    fun changeQuery(newQuery: String) {
        query.value = newQuery
    }

    /** シートを閉じるときに呼ぶ。**打ちかけの検索語も閉じる合図も捨てる。** */
    fun reset() {
        query.value = ""
        nameError.value = null
        _closeRequest.value = CloseRequest()
    }

    /**
     * 未追加の行のタップ。カゴに入れて閉じる（画面 05）。
     *
     * **他の行き先のものは今の一覧に現れない**ので、足したことを文言で知らせる。
     */
    fun add(item: Item) {
        viewModelScope.launch {
            val elsewhere = uiState.value.elsewhereDestinationName(item)
            itemRepository.updateStatusAsInBasket(item)
            close(addedElsewhere = elsewhere)
        }
    }

    /**
     * 「＋ この名前で登録する」。**行き先は今の店・カテゴリなしで登録し、即カゴ入り**
     * （画面 05）。全件モードは今の店を持たないので行き先なしで登録する。
     */
    fun registerQuery() {
        viewModelScope.launch {
            val result = itemRepository.insert(
                Item(
                    name = query.value,
                    status = ItemStatus.IN_SHOPPING_LIST,
                    destinationId = destinationId
                )
            )
            nameError.applySaveResult(result) { close(addedElsewhere = null) }
        }
    }

    private fun close(addedElsewhere: String?) {
        _closeRequest.value = CloseRequest(
            count = _closeRequest.value.count + 1,
            addedElsewhere = addedElsewhere
        )
    }
}

/**
 * 閉じる合図。
 *
 * @param count 閉じる要求の回数。**0 のうちは開いたまま**（[AddNoticedViewModel.reset] が 0 に戻す）
 * @param addedElsewhere 足した先の行き先名。今の店・どこでも買えるものなら null
 */
data class CloseRequest(val count: Int = 0, val addedElsewhere: String? = null)

/**
 * シートの中身の導出（画面 05）。
 *
 * **空入力と検索中で当たりの意味が変わる。** 空のときは「未追加のものから選ぶ」なので
 * 未追加だけ、打ち始めると**追加済みも当たりに含めて**「追加済み」と添えて出す——
 * 探したものが見つからないと、もう入れたのか入れ忘れたのかが分からないため。
 */
data class AddNoticedSheetUiState(
    val destinationId: Int?,
    val query: String = "",
    val nameError: NameError? = null,
    val items: List<Item> = emptyList(),
    val destinations: List<Destination> = emptyList()
) {
    /** 全件モード。「今の行き先」を持たないので、行き先での仕分けをしない（画面 05）。 */
    val isAllMode: Boolean = destinationId == null

    private val trimmed: String = query.trim()

    private val isSearching: Boolean = trimmed.isNotEmpty()

    /** ひらがな・カタカナの同一視はしない。**単純な部分一致**（画面 05）。 */
    private val hits: List<Item> =
        if (isSearching) items.filter { it.name.contains(trimmed) } else items.filterNot { it.isInBasket }

    /** 「未追加のものから選ぶ」の今の行き先ぶん。全件モードでは仕分けないので当たり全部。 */
    val hereItems: List<Item> =
        if (isAllMode) hits else hits.filter { it.destinationId == destinationId }

    /** 同じセクションの中の区切り「どこでも買えるもの」の下。全件モードでは出ない。 */
    val anywhereItems: List<Item> =
        if (isAllMode) emptyList() else hits.filter { it.destinationId == null }

    /**
     * 「他の行き先から」。**検索中だけのセクション**（画面 05）——初期表示は今の店の買い物なので、
     * 他の店のものまで並べると選ぶものが埋もれる。全件モードでは使わず、当たりはすべて上へ。
     */
    val elsewhereRows: List<ElsewhereRow> = if (isAllMode || !isSearching) {
        emptyList()
    } else {
        hits.mapNotNull { item ->
            elsewhereDestinationName(item)?.let { ElsewhereRow(item, it) }
        }
    }

    // 1 度しか読まないので get()（アーキテクチャ定義書 §4.3）

    /** 空のセクションは見出しごと出さない（画面 05）。**当たりが他の行き先だけなら出さない。** */
    val isUnaddedSectionVisible: Boolean get() = hereItems.isNotEmpty() || anywhereItems.isNotEmpty()

    /** 「＋ この名前で登録する」は**入力が空白のみでない間は常に末尾に出る**（画面 05）。 */
    val canRegisterQuery: Boolean get() = isSearching

    /**
     * [item] が「他の行き先」のものなら、その行き先名。今の店・どこでも買えるもの・
     * 全件モードでは null。**通知を出すかどうかもこの判定で決まる。**
     */
    fun elsewhereDestinationName(item: Item): String? {
        if (isAllMode) return null
        val itemDestinationId = item.destinationId ?: return null
        if (itemDestinationId == destinationId) return null
        return destinations.firstOrNull { it.id == itemDestinationId }?.name
    }
}

/** 「他の行き先から」の 1 行。**どの店のものかを添える**（画面 05）。 */
data class ElsewhereRow(val item: Item, val destinationName: String)
