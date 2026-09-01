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
import io.github.obaya884.rebuy.ui.screen.NameTarget
import io.github.obaya884.rebuy.ui.screen.manage.EditingRecord
import io.github.obaya884.rebuy.ui.screen.manage.ManageEditSheet
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
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

    /** 1 つ隣へ落ちるのに指が進む距離（行の高さ＋行間 8dp）。 */
    private fun ComposeUiTest.rowPitchPx(): Float {
        val first = onNodeWithTag(TestTags.manageRow(1)).fetchSemanticsNode().positionInRoot.y
        val second = onNodeWithTag(TestTags.manageRow(2)).fetchSemanticsNode().positionInRoot.y
        return second - first
    }

    /** 画面に出ている順の名前。**座標ではなく並びで見る**（位置の assert は脆い）。 */
    private fun ComposeUiTest.rowNames(): List<String> =
        onAllNodesWithTag(TestTags.MANAGE_ROW_NAME, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    @Test
    fun 並び順に行が出て末尾に破線行がある() = manage {
        assertEquals(listOf("カテゴリA", "カテゴリB", "カテゴリC"), rowNames())
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertExists()
    }

    /** 空状態は破線行だけ（画面 09）。 */
    @Test
    fun 一件も無ければ破線行だけ() = manage(prepare = {}) {
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertExists()
        onAllNodesWithTag(TestTags.MANAGE_ROW_NAME, useUnmergedTree = true)
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
        // 面ではなく**名前**が長押しを受ける（ハンドルはドラッグに使うため）
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
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
        // 面ではなく**名前**が長押しを受ける（ハンドルはドラッグに使うため）
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("「カテゴリA」を削除しますか？").assertExists()
        onNodeWithText("紐づく 2 件はカテゴリなしになります。").assertExists()
    }

    /**
     * **行き先向きの文言**。「どこでも買えるもの」に戻ることを言う（画面 09b）。
     *
     * `Screen.Manage(DESTINATION)` は F-013 まで UI から開けないので、シートを直に描く。
     * カテゴリ側と取り違えても、画面から踏むテストでは捕まらない。
     */
    @Test
    fun 行き先の削除はどこでも買えるものに戻ると言う() = runComposeUiTest {
        setContent {
            ReBuyTheme {
                ManageEditSheet(
                    editing = EditingRecord(id = 1, originalName = "行き先A", name = "行き先A"),
                    nameError = null,
                    target = NameTarget.DESTINATION,
                    affectedItemCount = 3,
                    onNameChange = {},
                    onSave = {},
                    onDelete = {},
                    onDismiss = {}
                )
            }
        }

        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("紐づく 3 件は「どこでも買えるもの」になります。").assertExists()
    }

    /** 紐づくものが無ければ戻り先を言わない（画面 09b）。 */
    @Test
    fun 紐づくものが無ければその旨だけ出る() = manage({
        seed(categories = listOf(category(id = 1, name = "カテゴリA")))
    }) {
        // 面ではなく**名前**が長押しを受ける（ハンドルはドラッグに使うため）
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
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
        // 面ではなく**名前**が長押しを受ける（ハンドルはドラッグに使うため）
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
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

    /** 削除の確認で「キャンセル」を選ぶと消えず、シートも残る（画面 09b）。 */
    @Test
    fun 削除の確認をキャンセルすると消えない() = manage {
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()

        onNodeWithText("キャンセル").performClick()

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertExists()
        assertEquals(listOf("カテゴリA", "カテゴリB", "カテゴリC"), rowNames())
    }

    /** 削除ボタンの文言は**編集前の名前**。打ちかけの名前では聞かない（06 と同じ）。 */
    @Test
    fun 削除の文言は打ちかけの名前にならない() = manage {
        onNodeWithText("カテゴリA").performTouchInput { longClick() }

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).performTextReplacement("打ちかけ")

        onNodeWithText("「カテゴリA」を削除…").assertExists()
    }

    /**
     * **ハンドルを掴んだまま止めても編集シートは開かない**（ドラッグと長押しの取り合い）。
     *
     * ハンドルが長押しを止めていないと、指を置いたまま 0.5 秒待った瞬間に 09b が開く。
     */
    @Test
    fun ハンドルを掴んだまま止めても編集シートは開かない() = manage {
        onNodeWithTag(TestTags.manageHandle(1), useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(1000)
            up()
        }

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
    }

    /**
     * **移動量は積み上がる。** 半行に満たない動きを重ねても、合計が半行を超えれば入れ替わる。
     *
     * 1 回ぶんだけを見る実装（`dragPx = delta`）だと、この筋書きでは 1 つも動かない。
     */
    @Test
    fun 半行に満たない動きも積み上がって入れ替わる() = manage {
        val pitch = rowPitchPx()
        onNodeWithTag(TestTags.manageHandle(1), useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(4) {
                moveBy(Offset(0f, pitch / 4f))
                advanceEventTime(16)
            }
            up()
        }

        assertEquals(listOf("カテゴリB", "カテゴリA", "カテゴリC"), rowNames())
    }

    /** **真ん中の行を上へ**。掴んだ位置を取り違えていると、別の行が動く。 */
    @Test
    fun 真ん中の行を上へ動かせる() = manage {
        val pitch = rowPitchPx()
        onNodeWithTag(TestTags.manageHandle(2), useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, -32f))
            advanceEventTime(16)
            moveBy(Offset(0f, -pitch))
            advanceEventTime(16)
            up()
        }

        assertEquals(listOf("カテゴリB", "カテゴリA", "カテゴリC"), rowNames())
    }

    /**
     * **2 行ぶん動かすと 2 つ動く。** 行間を数えていないと、動かすほど行き過ぎる
     * （高さだけで割ると 2 行ぶんの距離が 2.3 行と数えられる）。
     */
    @Test
    fun 二行ぶん動かすと二つ動く() = manage {
        val pitch = rowPitchPx()
        onNodeWithTag(TestTags.manageHandle(1), useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 32f))
            advanceEventTime(16)
            moveBy(Offset(0f, 2 * pitch - 32f))
            advanceEventTime(16)
            up()
        }

        assertEquals(listOf("カテゴリB", "カテゴリC", "カテゴリA"), rowNames())
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
