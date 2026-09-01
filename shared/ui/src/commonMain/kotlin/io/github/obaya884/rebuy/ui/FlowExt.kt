package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.domain.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> {
    return kotlinx.coroutines.flow.combine(
        flow,
        flow2,
        flow3,
        flow4,
        flow5,
        flow6
    ) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }
}

/**
 * 名前を伴う保存の結果を、エラーの置き場へ反映する。
 *
 * 保存できたらエラーを消して [onSaved]（ふつうはシートやダイアログを閉じる）、
 * 弾かれたら開いたまま理由を出す（画面定義書 §2）。**この分岐を画面ごとに書き写さない**
 * ために置いている。
 */
fun MutableStateFlow<NameError?>.applySaveResult(result: SaveResult, onSaved: () -> Unit) {
    when (result) {
        is SaveResult.Saved -> {
            value = null
            onSaved()
        }

        is SaveResult.Rejected -> value = result.error
    }
}
