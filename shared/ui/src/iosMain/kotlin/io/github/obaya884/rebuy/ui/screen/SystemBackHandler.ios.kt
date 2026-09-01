package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable

/** iOS には端末の戻りが無い。**何もしない**のが正しい実装。 */
@Composable
actual fun SystemBackHandler(onBack: () -> Unit) = Unit
