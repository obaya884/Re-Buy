package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * iOS の端末の戻りは**画面の端から内側へのスワイプ**（画面定義書 §2）。
 *
 * **配る側は「最後に登録された、有効なハンドラ」を選ぶ。** `NavDisplay` の戻りより内側で
 * 登録されるので、こちらが先に受ける（Android 側と同じ）。
 *
 * **`NavDisplay` と同じ `NavigationBackHandler` を使う。** `androidx.compose.ui.backhandler.BackHandler`
 * にも同じことができるが、あちらは `@Deprecated("Use NavigationEventHandler instead")` が付いている。
 *
 * **[NavigationEventInfo] は中身を持たせない。** これは進捗つきの戻り（引いている最中に行き先を
 * 見せる）で使う情報で、ここは完了だけを見るため。
 *
 * **[enabled] の効きは画面を通しては見えない。** 同じ規則（後に登録されたものが先に取る）で、
 * シートやダイアログが開いていればそちらが先に取るので、**こちらが有効かどうかに関わらず
 * 呼ばれない**。単体で描く `SystemBackHandlerIosTest` が結線を守る。
 *
 * **端スワイプが効くかは OS の版と依存の組み合わせで変わりうる**——CMP 1.12.0 では iOS 26.5 で効かず、
 * 1.12.2 では効いた（**どの変数が効いたかは未特定**。技術改善バックログ T-62）。効かない組み合わせでは
 * この実装が呼ばれないだけで、戻る道はアプリバーの ← が残る。
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}

@Composable
actual fun IosEdgeSwipeDismissHandler(onDismiss: () -> Unit) {
    SystemBackHandler(onBack = onDismiss)
}
