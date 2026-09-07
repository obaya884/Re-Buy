package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import io.github.obaya884.rebuy.ui.screen.SystemBackHandler
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `SystemBackHandler` の iOS 実装そのもの。**画面を挟まずに単体で描く。**
 *
 * FB-18 で壊れていたのはこの `actual` で、当初は「iOS には端末の戻るが無い」という誤った前提で
 * 何もしない実装だった。**アプリ全体を通すテスト（`ShoppingIosTest`）では `enabled` の結線を
 * 守れない**——同じ優先度のハンドラは後に登録されたものが先に取るので、シートやダイアログが
 * 開いていればこちらが有効かどうかに関わらず呼ばれず、`enabled` を潰す変異が素通りする（実測）。
 * ここは他のハンドラが 1 つも無いので、[SystemBackHandler] の受け取り方だけが観測できる。
 */
@OptIn(ExperimentalTestApi::class)
class SystemBackHandlerIosTest {

    /** 受けた回数。**真偽でなく数で持つ**——2 回呼ばれる壊れ方も捕まえる。 */
    private fun handler(enabled: Boolean, block: ComposeUiTest.(DirectNavigationEventInput, () -> Int) -> Unit) =
        runComposeUiTest {
            val input = DirectNavigationEventInput()
            var count = 0
            setContent {
                InstallSystemBack(input)
                SystemBackHandler(enabled = enabled) { count++ }
            }
            block(input) { count }
        }

    @Test
    fun 有効なら端末の戻りを受ける() = handler(enabled = true) { input, count ->
        pressSystemBack(input)

        assertEquals(1, count())
    }

    @Test
    fun 無効なら端末の戻りを受けない() = handler(enabled = false) { input, count ->
        pressSystemBack(input)

        assertEquals(0, count())
    }

    /**
     * 端から引きかけて指を戻したときは受けない。**完了だけを流していると、`onBackCompleted` と
     * `onBackCancelled` の取り違えに気づけない。**
     */
    @Test
    fun 引きかけてやめたら受けない() = handler(enabled = true) { input, count ->
        cancelSystemBack(input)

        assertEquals(0, count())
    }
}
