package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 行き先の管理（画面 09）の**画面段**。
 *
 * **画面そのものは `ManageIosTest` がカテゴリ向きで見ている**（09 は同型の 2 画面で、
 * 実装は 1 つ）。ここが見るのは**向きで変わるところ**だけ——設定からの入口・タイトル・
 * 破線行と削除の文言、そして「行き先を触ってもカテゴリは動かない」こと。
 *
 * **ドラッグは置かない。** ジェスチャは `ManageIosTest` が、向きごとの保存先は
 * `ManageViewModelTest` が見ており、両方の積でしかないものを遅い段で再演しない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class DestinationManageIosTest {

    private val twoOfEach: FakeDatabase.() -> Unit = {
        seed(
            categories = listOf(
                category(id = 1, name = "カテゴリA", sortOrder = 1),
                category(id = 2, name = "カテゴリB", sortOrder = 2)
            ),
            destinations = listOf(
                destination(id = 1, name = "行き先A", sortOrder = 1),
                destination(id = 2, name = "行き先B", sortOrder = 2)
            )
        )
    }

    /** 設定から「行き先の管理」を開く。 */
    private fun manage(
        prepare: FakeDatabase.() -> Unit = twoOfEach,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithTag(TestTags.SETTING_ROW_DESTINATION_MANAGE).performClick()
        block()
    }

    private fun ComposeUiTest.rowNames(): List<String> =
        onAllNodesWithTag(TestTags.MANAGE_ROW_NAME, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    /** **カテゴリではなく行き先が出る。** 向きを取り違えるとここで落ちる。 */
    @Test
    fun 設定から開くと行き先が並ぶ() = manage {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("行き先の管理")
        assertEquals(listOf("行き先A", "行き先B"), rowNames())
    }

    @Test
    fun 破線行の文言は行き先を追加() = manage {
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertTextEquals("＋ 行き先を追加")
    }

    @Test
    fun 破線行から追加すると行き先が増える() = manage {
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).performClick()
        onNodeWithText("新しい行き先").assertExists()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).performTextInput("行き先Z")
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_CREATE).performClick()

        assertEquals(listOf("行き先A", "行き先B", "行き先Z"), rowNames())
    }

    /** 名前を変えても**カテゴリは動かない**（向きの取り違えはここで出る）。 */
    @Test
    fun 名前を変えても他方は動かない() = manage {
        onNodeWithText("行き先A").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).performTextReplacement("行き先A改")
        onNodeWithTag(TestTags.MANAGE_SHEET_SAVE).performClick()

        assertEquals(listOf("行き先A改", "行き先B"), rowNames())
        // 設定へ戻ってカテゴリ側を見る。名前も並びも無傷
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()
        assertEquals(listOf("カテゴリA", "カテゴリB"), rowNames())
    }

    /** 削除の確認は**「どこでも買えるもの」に戻る**と言う（画面 09b）。 */
    @Test
    fun 削除の確認はどこでも買えるものに戻ると言う() = manage({
        seed(
            items = listOf(item(id = 1, destinationId = 1), item(id = 2, destinationId = 1)),
            destinations = listOf(destination(id = 1, name = "行き先A"))
        )
    }) {
        onNodeWithText("行き先A").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("紐づく 2 件は「どこでも買えるもの」になります。").assertExists()
    }

    /** 消しても品目は残り、どこでも買えるものに戻る（データモデル定義書 §7）。 */
    @Test
    fun 削除しても品目は残る() = manage({
        seed(
            items = listOf(item(id = 1, destinationId = 1, name = "アイテムA")),
            destinations = listOf(destination(id = 1, name = "行き先A"))
        )
    }) {
        onNodeWithText("行き先A").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE_CONFIRM).performClick()

        assertEquals(emptyList(), rowNames())
        // プールへ戻ると、品目は残って**「どこでも買えるもの」の側**に入っている
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()
        onNodeWithText("アイテムA").assertExists()
    }
}
