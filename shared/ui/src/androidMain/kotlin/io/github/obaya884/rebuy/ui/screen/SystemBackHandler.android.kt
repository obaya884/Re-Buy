package io.github.obaya884.rebuy.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * `NavDisplay` が持つ戻るより内側で登録されるので、**こちらが先に受ける**。
 */
@Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
