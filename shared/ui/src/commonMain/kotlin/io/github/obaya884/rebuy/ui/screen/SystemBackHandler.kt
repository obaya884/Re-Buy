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
 *
 * @param enabled false のときは受けずに素通しする。シートが開いている間は
 *   シート側が閉じるために使うので、下の画面が横取りしない（画面定義書 §2）
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
