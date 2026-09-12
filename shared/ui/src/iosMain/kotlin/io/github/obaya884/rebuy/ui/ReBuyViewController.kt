package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.ComposeUIViewController
import io.github.obaya884.rebuy.ui.di.initKoin
import io.github.obaya884.rebuy.ui.screen.LocalReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.NoAppBarRenderer
import platform.UIKit.UIViewController

/**
 * iOS 側の入口。**入口の関数はこの 1 ファイルにまとめる**——Kotlin/Native は
 * トップレベル関数の入れ物クラス名をファイル名から作るので、分けると
 * Swift 側に `...Kt` が 2 つ並ぶ。
 *
 * **段 4 の Step 4 で消す**。バーを Compose が描く版で、いまの `ContentView` はこちらを呼ぶ。
 */
fun ReBuyViewController(): UIViewController = ComposeUIViewController {
    ReBuyApp()
}

/**
 * バーを SwiftUI が描く版の入口（`docs/仕様/13_画面定義書.md` §6）。
 *
 * [onAppBar] は**バーに出すものが変わったときだけ**呼ばれる（[PublishingAppBarRenderer]）。
 * Compose のメインスレッドから同期で呼ばれるので、**中から Kotlin を呼び返さないこと**。
 *
 * **`setupKoin()` を先に呼ぶ約束は変わらない**——この関数は [onAppBar] を抱えるだけで Koin を
 * 引かず、引くのは最初のコンポーズ（view の load 後）。裏返しに、**起動直後の 1 フレームは
 * Swift 側にバーの内容がまだ無い**（最初の publish はコンポーズの後）。
 */
fun ReBuyViewController(onAppBar: (ReBuyToolbar) -> Unit): UIViewController =
    ComposeUIViewController { ReBuyIosApp(onAppBar) }

/**
 * [ReBuyViewController] の中身。**`UIViewController` を組み立てずに描けるようにするため**に
 * 切り出している——`iosTest` は `ComposeUIViewController` を通せないので、ここが無いと
 * 本番の経路（バーを描かず、購読口へ流す）に網が 1 本も掛からない。
 */
@Composable
internal fun ReBuyIosApp(onAppBar: (ReBuyToolbar) -> Unit) {
    // **描き手は作り直さない**。`LocalReBuyAppBarRenderer` は static なので提供する値が変わると
    // subtree 全体が再コンポーズされ、しかも作り直すと「最後に渡した鍵」が消えて重複して渡る。
    // **キーに `onAppBar` を使わない**のはそのため——ラムダの同一性で作り直す形になる
    val latestOnAppBar = rememberUpdatedState(onAppBar)
    val renderer = remember { PublishingAppBarRenderer(NoAppBarRenderer) { latestOnAppBar.value(it) } }
    CompositionLocalProvider(LocalReBuyAppBarRenderer provides renderer) {
        ReBuyApp()
    }
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
