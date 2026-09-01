package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * 品目編集シート（画面 06）の**画面段**。`ItemEditViewModelTest` は状態までしか見ないので、
 * **長押しで開かない・チップの取り違え・確認ダイアログに打ちかけの名前が出る**といった
 * 結線の抜けはここでしか捕まらない。
 *
 * 専用の seed を持つ（カテゴリと行き先を 2 件ずつ）。**付け替えを見るには 2 件要る。**
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class ItemEditSheetIosTest {

    /**
     * 品目 2 件。1 件目はカテゴリ・行き先・最終購入日を持ち、2 件目は何も持たない。
     *
     * 最終購入日を正午にしてあるのは、**端末のタイムゾーンで日付に落とす**ので
     * 0 時だと西側の設定で前日になるため。
     */
    private fun twoItems(): FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(
                    id = 1,
                    categoryId = 1,
                    destinationId = 1,
                    lastBoughtAt = Instant.parse("2026-08-29T12:00:00Z")
                ),
                item(id = 2)
            ),
            categories = listOf(category(id = 1), category(id = 2)),
            destinations = listOf(destination(id = 1), destination(id = 2))
        )
    }

    private fun sheet(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        startTestKoin(twoItems())
        setContent { ReBuyApp() }
        block()
    }

    private fun ComposeUiTest.openSheetFor(itemId: Int) {
        onNodeWithTag(TestTags.poolRow(itemId)).performTouchInput { longClick() }
    }

    // ---- 開く・保存 ----

    @Test
    fun 行の長押しで編集シートが開いて保存できる() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextClearance()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextInput("アイテムZ")
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertTextContains("アイテムZ")
    }

    /** **保存した後にもう一度開ける**（閉じる合図が残ると、開いた瞬間に閉じる）。 */
    @Test
    fun 保存した後にもう一度長押しで開ける() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        openSheetFor(itemId = 2)

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertIsDisplayed()
    }

    /** 弾かれたらシートは開いたまま、入力欄の下に理由を出す（画面定義書 §2）。 */
    @Test
    fun 同じ名前にすると理由が出てシートは開いたまま() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextClearance()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextInput("アイテム1")
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithText("同じ名前がすでにあります").assertIsDisplayed()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertIsDisplayed()
    }

    // ---- チップ ----

    /** 別のカテゴリのチップへ付け替えられる（06 のチップは 02 とは別の結線）。 */
    @Test
    fun カテゴリを付け替えて保存できる() = sheet {
        openSheetFor(itemId = 1)
        onNodeWithTag(TestTags.itemFormCategoryChip(categoryId = 2)).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertTextContains("カテゴリー2")
    }

    /**
     * 「なし」チップで行き先を外せる（画面 06）。
     *
     * **外れたことは「🏬 どこでも」で絞って確かめる**——行き先の名前は絞り込みチップにも
     * 出るので、画面から消えたかどうかでは見られない。
     */
    @Test
    fun なしチップで行き先を外せる() = sheet {
        openSheetFor(itemId = 1)
        onNodeWithTag(TestTags.ITEM_SHEET_DESTINATION_NONE_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
        // カテゴリは触っていないので残る
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertTextContains("カテゴリー1")
    }

    @Test
    fun なしチップでカテゴリも外せる() = sheet {
        openSheetFor(itemId = 1)
        onNodeWithTag(TestTags.ITEM_SHEET_CATEGORY_NONE_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        // カテゴリ 1 で絞ると、外した品目は出てこない（プールの絞り込みチップ）
        onNodeWithTag(TestTags.poolCategoryChip(categoryId = 1)).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertDoesNotExist()
    }

    /** 06 からも 02b を開ける（画面 02b は「02・06 のチップから」開く）。 */
    @Test
    fun 新しい行き先をその場で作って付けられる() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_FORM_NEW_DESTINATION_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).performTextInput("行き先C")
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_CREATE).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertTextContains("🏬 行き先C")
    }

    // ---- 最終購入日 ----

    @Test
    fun 最終購入日はYYYYMMDDで出る() = sheet {
        openSheetFor(itemId = 1)

        onNodeWithText("最終購入日: 2026-08-29").assertIsDisplayed()
    }

    @Test
    fun 未購入の品目はダッシュで出る() = sheet {
        openSheetFor(itemId = 2)

        onNodeWithText("最終購入日: —").assertIsDisplayed()
    }

    // ---- 削除 ----

    /**
     * 削除は確認ダイアログを挟む。**確認に出るのは打ちかけの名前ではなく元の名前**
     * （画面 06。まだ保存していない名前で「これを削除しますか」と聞かない）。
     */
    @Test
    fun 削除の確認には元の名前が出る() = sheet {
        openSheetFor(itemId = 1)
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextClearance()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextInput("書きかけ")

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()

        onNodeWithText("「アイテム1」を削除しますか？").assertIsDisplayed()
        onNodeWithText("「書きかけ」を削除しますか？").assertDoesNotExist()
    }

    @Test
    fun 確認してから削除すると一覧から消えてシートも閉じる() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()
        // 確認を押すまでは消えない
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertExists()

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertDoesNotExist()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
    }

    /** キャンセルでは何も消えず、シートも開いたまま。 */
    @Test
    fun 削除をキャンセルすると品目もシートも残る() = sheet {
        openSheetFor(itemId = 2)
        onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()

        onNodeWithText("キャンセル").performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertExists()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertIsDisplayed()
    }
}
