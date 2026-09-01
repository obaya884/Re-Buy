package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable

/**
 * 端末の「戻る」を画面側で受け止める。
 *
 * **Compose Multiplatform の `androidx.compose.ui.backhandler.BackHandler` は使えない。**
 * Android では `compose.ui` が androidx の実装へ解決され、そこにこのパッケージが無い
 * （compose-ui 1.12.0 で実測）。
 *
 * iOS には端末の戻るが無いので、実装は何もしない——**戻る道はアプリバーの ← だけ**になる。
 */
@Composable
expect fun SystemBackHandler(onBack: () -> Unit)
