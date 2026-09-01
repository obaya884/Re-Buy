package io.github.obaya884.rebuy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * カテゴリの管理（画面 09）と編集シート（09b）の**画面段**。
 *
 * `ManageViewModelTest` が見るのは並びと書き込みまでで、**ハンドルを掴んで動かすと
 * 並びが変わること**、**行の長押しで 09b が開くこと**、**削除の確認に件数が出ること**は
 * ここでしか見られない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ManageIosTest {

    private val threeCategories: FakeDatabase.() -> Unit = {
        seed(
            categories = listOf(
                category(id = 1, name = "カテゴリA", sortOrder = 1),
                category(id = 2, name = "カテゴリB", sortOrder = 2),
                category(id = 3, name = "カテゴリC", sortOrder = 3)
            )
        )
    }

    /** 設定から「カテゴリの管理」を開く。 */
    private fun manage(
        prepare: FakeDatabase.() -> Unit = threeCategories,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("カテゴリの管理")
        block()
    }

    /** 画面に出ている順の名前。**座標ではなく並びで見る**（位置の assert は脆い）。 */
    private fun ComposeUiTest.rowNames(): List<String> =
        onAllNodesWithTag(TestTags.MANAGE_ROW, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    @Test
    fun 並び順に行が出て末尾に破線行がある() = manage {
        onNodeWithTag(TestTags.manageRow(1)).assertExists()
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertExists()
    }

    /** 空状態は破線行だけ（画面 09）。 */
    @Test
    fun 一件も無ければ破線行だけ() = manage(prepare = {}) {
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertExists()
        onAllNodesWithTag(TestTags.MANAGE_ROW, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .let { assertEquals(0, it.size) }
    }

    /** 破線行から 02b を開いて作ると、**末尾に現れる**（画面 09）。 */
    @Test
    fun 破線行から追加すると末尾に現れる() = manage {
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).performTextInput("カテゴリZ")
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_CREATE).performClick()

        assertEquals(listOf("カテゴリA", "カテゴリB", "カテゴリC", "カテゴリZ"), rowNames())
    }

    /** 行の長押しで 09b が開き、保存で名前が変わる（画面 09・09b）。 */
    @Test
    fun 長押しで開いた編集シートで名前を変えられる() = manage {
        onNodeWithTag(TestTags.manageRow(1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertExists()

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).performTextReplacement("カテゴリA改")
        onNodeWithTag(TestTags.MANAGE_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithText("カテゴリA改").assertExists()
    }

    /** 削除の確認は**影響を件数で示す**（画面 09b）。 */
    @Test
    fun 削除の確認に紐づく件数が出る() = manage({
        seed(
            items = listOf(item(id = 1, categoryId = 1), item(id = 2, categoryId = 1)),
            categories = listOf(category(id = 1, name = "カテゴリA"))
        )
    }) {
        onNodeWithTag(TestTags.manageRow(1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("「カテゴリA」を削除しますか？").assertExists()
        onNodeWithText("紐づく 2 件はカテゴリなしになります。").assertExists()
    }

    /** 紐づくものが無ければ戻り先を言わない（画面 09b）。 */
    @Test
    fun 紐づくものが無ければその旨だけ出る() = manage({
        seed(categories = listOf(category(id = 1, name = "カテゴリA")))
    }) {
        onNodeWithTag(TestTags.manageRow(1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("紐づくものはありません。").assertExists()
    }

    @Test
    fun 削除すると行が消えて品目は残る() = manage({
        seed(
            items = listOf(item(id = 1, categoryId = 1, name = "アイテムA")),
            categories = listOf(category(id = 1, name = "カテゴリA"))
        )
    }) {
        onNodeWithTag(TestTags.manageRow(1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE_CONFIRM).performClick()

        onNodeWithTag(TestTags.manageRow(1)).assertDoesNotExist()
        // 品目は消えない。戻ってプールで確かめる
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithText("アイテムA").assertExists()
    }

    /**
     * **ハンドルを掴んで動かすと並びが変わり、離した時点で保存される**（画面 09）。
     *
     * `swipe()` は使わない——刻みが粗く、途中の位置が配送されないと落とし先が動かない。
     * `down` → `moveBy` → `up` を明示する。最初の小さい移動はタッチスロップに食われる。
     */
    @Test
    fun ハンドルを下へ動かすと並びが変わる() = manage {
        val handle = onNodeWithTag(TestTags.manageHandle(1), useUnmergedTree = true)
        val rowHeight = onNodeWithTag(TestTags.manageRow(1)).fetchSemanticsNode().size.height

        handle.performTouchInput {
            down(center)
            // 1 回目はタッチスロップで消える
            moveBy(Offset(0f, 32f))
            advanceEventTime(16)
            moveBy(Offset(0f, rowHeight.toFloat()))
            advanceEventTime(16)
            up()
        }

        assertEquals(listOf("カテゴリB", "カテゴリA", "カテゴリC"), rowNames())
    }

    /** 離した並びは残る。**開き直しても戻らない**なら保存されている（画面 09）。 */
    @Test
    fun 並び替えは開き直しても残る() = manage {
        val rowHeight = onNodeWithTag(TestTags.manageRow(1)).fetchSemanticsNode().size.height
        onNodeWithTag(TestTags.manageHandle(1), useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 32f))
            advanceEventTime(16)
            moveBy(Offset(0f, rowHeight.toFloat()))
            advanceEventTime(16)
            up()
        }

        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()

        assertEquals(listOf("カテゴリB", "カテゴリA", "カテゴリC"), rowNames())
    }
}
