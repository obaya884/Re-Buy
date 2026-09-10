package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import io.github.obaya884.rebuy.ui.TestTags

/**
 * [ReBuyAppBarState] を Material のアプリバーとして描く。**既定の描き手**。
 *
 * **本番の iOS では使わない**——外枠は SwiftUI が持つ（`docs/仕様/13_画面定義書.md` §6）。
 * iOS のテストは、バーが無いと ⚙ や ← を押せなくなるのでこれを描かせている。
 */
internal object MaterialAppBarRenderer : ReBuyAppBarRenderer {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(appBar: ReBuyAppBarState) {
        TopAppBar(
            title = { Text(appBar.title, modifier = Modifier.testTag(TestTags.TOP_APP_BAR_TITLE)) },
            navigationIcon = {
                appBar.onBack?.let { onBack ->
                    ReBuyAppBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        modifier = Modifier.testTag(TestTags.BACK_BUTTON)
                    )
                }
            },
            actions = {
                appBar.count?.let { count ->
                    ReBuyAppBarCount(
                        text = count.text,
                        modifier = Modifier.testTagIfPresent(count.testTag)
                    )
                }
                appBar.actions.forEach { action ->
                    ReBuyAppBarIconButton(
                        icon = action.icon.materialIcon,
                        onClick = action.onClick,
                        modifier = Modifier.testTag(action.testTag)
                    )
                }
            }
        )
    }
}

/** Material 側の対応表（`docs/仕様/13_画面定義書.md` §6）。iOS は SF Symbols の表を別に持つ。 */
private val ReBuyAppBarIcon.materialIcon: ImageVector
    get() = when (this) {
        ReBuyAppBarIcon.ADD -> Icons.Default.Add
        ReBuyAppBarIcon.SETTINGS -> Icons.Default.Settings
    }

/**
 * 印の無いものに空文字のタグを付けない——`onNodeWithTag("")` で拾えてしまう。
 *
 * **この分岐に網は張らない。** 守れるのは Android の段だけで、段 4 のあと iOS には効かなくなる
 * （バーを描くのが SwiftUI になる）。網の寿命に対して張る手間が見合わない。
 */
private fun Modifier.testTagIfPresent(tag: String?): Modifier = if (tag == null) this else testTag(tag)
