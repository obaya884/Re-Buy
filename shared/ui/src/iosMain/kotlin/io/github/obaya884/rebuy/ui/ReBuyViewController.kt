package io.github.obaya884.rebuy.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.github.obaya884.rebuy.ui.di.initKoin
import platform.UIKit.UIViewController

/**
 * iOS 側の入口。**入口の関数はこの 1 ファイルにまとめる**——Kotlin/Native は
 * トップレベル関数の入れ物クラス名をファイル名から作るので、分けると
 * Swift 側に `...Kt` が 2 つ並ぶ。
 */
fun ReBuyViewController(): UIViewController = ComposeUIViewController {
    ReBuyApp()
}

/**
 * Koin を起動する。Android の `ReBuyApplication.onCreate()` にあたる。
 * 起動の作法そのものは [initKoin] が持っており、ここは Swift から呼べる入口。
 *
 * Android の `androidContext()` にあたるものは渡さない。Context に触るのは DB の
 * パス解決だけで、iOS 側は `NSDocumentDirectory` から自力で引ける。
 *
 * Swift からは `ReBuyViewControllerKt.setupKoin()` で呼ぶ。**`ReBuyViewController()` より
 * 先に呼ぶこと**——画面が `koinViewModel()` を引くので、後だと最初の描画で落ちる。
 * 2 回呼ぶと `KoinApplicationAlreadyStartedException` で落ちるが、それでよい——
 * `allowOverride(false)` と同じで、黙って動き続けるより起動時に気づけるほうを採る。
 */
fun setupKoin() {
    initKoin()
}
