package io.github.obaya884.rebuy

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag

/** シートが開くまでの待ち。GMD では既定の 1 秒に収まらないことがある。 */
const val SHEET_TIMEOUT_MS = 5_000L

/**
 * [tag] のノードが現れるまで待つ。**シートの中身は `waitForIdle` では間に合わないことがある**
 * （GMD で実測）——開くアニメーションが負荷で伸びる。
 */
fun ComposeTestRule.waitUntilTagExists(tag: String) =
    waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
