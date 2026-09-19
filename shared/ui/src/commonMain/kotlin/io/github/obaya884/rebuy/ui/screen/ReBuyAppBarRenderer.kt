package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * [ReBuyAppBarState] を実際のバーとして描く役。
 *
 * 実装は [MaterialAppBarRenderer]（Android と、バーを押すテスト）と [NoAppBarRenderer]
 * （iOS の本番。外枠は SwiftUI が持つ）の 2 つ。
 *
 * `fun interface` にしていないのは、SAM 変換で `@Composable` なラムダを推論させると
 * 呼ぶ側の書き方に制約が出るため。
 */
internal interface ReBuyAppBarRenderer {
    @Composable
    fun Render(appBar: ReBuyAppBarState)
}

/**
 * いま使う描き手。**既定は Material**。
 *
 * **引数ではなく CompositionLocal で渡す。** 差し替えるのはアプリ全体で 1 回きり（iOS の入口）で、
 * 6 画面すべてに引数を通しても中継するだけになる。**`static` なのは差し替えが起動時の 1 回だけ**
 * だから——読み手を追跡する必要がなく、再コンポーズの手間がかからない。
 */
internal val LocalReBuyAppBarRenderer = staticCompositionLocalOf<ReBuyAppBarRenderer> {
    MaterialAppBarRenderer
}
