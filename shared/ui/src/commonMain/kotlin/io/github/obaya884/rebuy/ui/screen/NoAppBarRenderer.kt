package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable

/**
 * バーを描かない描き手。**iOS の本番はこれを通る**（`docs/仕様/13_画面定義書.md` §6）——外枠は
 * SwiftUI が持つので、Compose も描くと**見出しが縦に 2 つ並ぶ**。
 *
 * **`commonMain` に置いている。** 空の composable はプラットフォーム API を一切触らないので、
 * `iosMain` へ置く理由（アーキテクチャ定義書 §1.1）に当たらない。描き手の 3 つ
 * （[ReBuyAppBarRenderer]・[MaterialAppBarRenderer]・これ）が同じ段に並ぶほうが読める。
 * **Android から挿せてしまうが、差し替え口は iOS の入口とテストの 2 か所しかない。**
 */
internal object NoAppBarRenderer : ReBuyAppBarRenderer {

    @Composable
    override fun Render(appBar: ReBuyAppBarState) {
        // 何も描かない。`Scaffold` の topBar が 0×0 になる
    }
}
