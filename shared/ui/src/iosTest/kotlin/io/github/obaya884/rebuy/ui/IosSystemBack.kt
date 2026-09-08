package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * テストから **iOS の端末の戻り**（画面の端から内側へのスワイプ。画面定義書 §2）を流す口。
 *
 * `runComposeUiTest` は Compose のセマンティクスを中から叩くもので UIKit のジェスチャを
 * 起こせない。かわりに **実物と同じ入力の枠**（`DirectNavigationEventInput`）を dispatcher へ
 * 足して、同じ経路へ流し込む——実機の端スワイプを流す `UIKitNavigationEventInput` も、
 * 同じ基底クラスの、同じ優先度なしの `addInput` で刺さる。
 *
 * **配り先は入力ではなくハンドラ側で決まる**ので、この口を足しても既存のテストの挙動は変わらない。
 */
@Composable
internal fun InstallSystemBack(input: DirectNavigationEventInput) {
    val owner = checkNotNull(LocalNavigationEventDispatcherOwner.current) {
        "LocalNavigationEventDispatcherOwner が無い。ComposeUIViewController の外で描いている"
    }
    val dispatcher = owner.navigationEventDispatcher
    DisposableEffect(dispatcher) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
}

/**
 * 端スワイプを最後まで引く。
 *
 * **同期はここで畳む。** ハンドラの有効・無効は `SideEffect` で押し込まれるので、composition の
 * 直後に流すと古い値のまま扱われ、**有効なのに配られない／無効なのに配られる**という逆の結果が出る
 * （両方を実測。上流も `NavigationEventHandler` の KDoc に "Timing Consideration" として書いている）。
 * **忘れると緑になる向きに転ぶ**ので、呼ぶ側の記憶に頼らずここに閉じ込める。
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pressSystemBack(input: DirectNavigationEventInput) {
    waitForIdle()
    input.backCompleted()
    waitForIdle()
}

/**
 * 端から引きかけて**やめる**。指を離さずに戻したときの経路で、[pressSystemBack] とは終わり方が違う。
 *
 * これを通すと `onBackCompleted` と `onBackCancelled` の取り違えが捕まる——完了だけを流していると、
 * 両方に同じ処理を繋いでも気づけない。
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.cancelSystemBack(input: DirectNavigationEventInput) {
    waitForIdle()
    input.backStarted(NavigationEvent(progress = 0f))
    input.backProgressed(NavigationEvent(progress = 0.3f))
    input.backCancelled()
    waitForIdle()
}
