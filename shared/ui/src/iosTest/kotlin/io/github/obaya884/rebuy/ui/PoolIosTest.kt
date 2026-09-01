package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * プール（画面 01）の**画面段**。`PoolViewModelTest` が見るのは状態までで、
 * **チップを押しても一覧が絞られない・行にタグを出し忘れる、といった結線の抜けは
 * そこでは捕まらない**（変異で実測）。
 *
 * 文言は実装と同じくリテラルで持つ（テスト戦略定義書 §2.1）。行の中身は
 * `TestData` の連番の名前（`アイテム1` など）で引く。
 */
@OptIn(ExperimentalTestApi::class)
class PoolIosTest {

    /**
     * 品目 2 件。**カテゴリーも行き先も違う**ので、どのチップで絞ったかが行の顔ぶれに出る。
     * 1 件目は最終購入日を持ち、2 件目は何も持たない。
     */
    private fun twoItems(): FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(
                    id = 1,
                    categoryId = 1,
                    destinationId = 1,
                    lastBoughtAt = Instant.parse("2026-08-29T00:00:00Z")
                ),
                item(id = 2)
            ),
            categories = listOf(category(id = 1)),
            destinations = listOf(destination(id = 1))
        )
    }

    private fun pool(
        prepare: FakeDatabase.() -> Unit = {},
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        block()
    }

    // ---- 行の中身（画面 01） ----

    /**
     * 行＝名前＋カテゴリタグ＋行き先タグ＋「前回 M/D」。**🏬 は表示のときの前置**で、
     * 行き先の名前の一部ではない。
     */
    @Test
    fun 行にカテゴリと行き先と前回の日付が出る() = pool(twoItems()) {
        val row = onNodeWithTag(TestTags.poolRow(itemId = 1))

        row.assertTextContains("アイテム1")
        row.assertTextContains("カテゴリー1")
        row.assertTextContains("🏬 行き先1")
        row.assertTextContains("前回 8/29")
    }

    /** 何も持たない品目は、タグを出さず「前回 —」だけを出す。 */
    @Test
    fun 何も持たない品目はタグを出さず未購入と出る() = pool(twoItems()) {
        val row = onNodeWithTag(TestTags.poolRow(itemId = 2))

        row.assertTextContains("アイテム2")
        row.assertTextContains("前回 —")
        // タグを出していないことは `PoolViewModelTest` が状態側で見る
        // （🏬 は「どこでも」チップにも出るので、画面全体からは引けない）
    }

    // ---- 登録シート（画面 02・02b） ----

    /**
     * ＋ からシートを開いて登録すると、**一覧の末尾に現れてシートが閉じる**（画面 02）。
     *
     * `RegisterViewModelTest` は状態までしか見ないので、**＋ がシートを開かない・
     * 「登録」が結線されていない**といった抜けはここでしか捕まらない。
     */
    @Test
    fun 登録シートから登録すると一覧の末尾に現れる() = pool(twoItems()) {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムC")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        // シートは閉じ、登録順の末尾（id = 3）に現れる
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.poolRow(itemId = 3)).assertTextContains("アイテムC")
        onNodeWithTag(TestTags.poolRow(itemId = 3)).assertTextContains("前回 —")
    }

    /**
     * **登録した後にもう一度開ける。**
     *
     * シートの ViewModel はプールの entry に属するので、シートを閉じても破棄されない。
     * 閉じる合図や入力が残ると、2 回目に開いた瞬間に閉じる／前回の入力が残る。
     */
    /** 開いたら名前欄にフォーカスが入る（画面 02）。**打ち始められる状態**であること。 */
    @Test
    fun 開くと名前欄にフォーカスが入る() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsFocused()
    }

    @Test
    fun 登録した後にもう一度シートを開ける() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムA")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()
    }

    /**
     * **保存せずに閉じたら入力は捨てる**（画面定義書 §2）。
     *
     * 下スワイプで閉じる経路を `Dismiss` のセマンティクスから踏む——`ModalBottomSheet` の
     * グリップが持っているので、ピクセルを動かさずに同じ道を通れる。
     */
    @Test
    fun 保存せずに閉じると入力は残らない() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("書きかけ")

        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()

        onNodeWithText("書きかけ").assertDoesNotExist()
        // 品目としても残っていない
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertDoesNotExist()
    }

    /** 「続けて登録」はシートを開いたままにする（画面 02）。 */
    @Test
    fun 続けて登録ではシートが開いたまま() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムA")
        onNodeWithTag(TestTags.REGISTER_SUBMIT_AND_CONTINUE).performClick()

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()
    }

    /** 弾かれたらシートは開いたまま、入力欄の下に理由が出る（画面定義書 §2）。 */
    @Test
    fun 同じ名前で登録すると理由が出てシートは開いたまま() = pool(twoItems()) {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテム1")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        onNodeWithText("同じ名前がすでにあります").assertIsDisplayed()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()
    }

    /** 02b で作ったカテゴリは、呼び出し元のチップ列に**選択済み**で現れる。 */
    @Test
    fun 新しいカテゴリを作るとその品目に付く() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_NEW_CATEGORY_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).performTextInput("カテゴリA")
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_CREATE).performClick()

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムA")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        // 作ったカテゴリが選ばれたまま登録されるので、行にタグが出る
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertTextContains("カテゴリA")
    }

    /** 行き先側の結線はカテゴリとは別のコピーなので、こちらも 1 件見る。 */
    @Test
    fun 新しい行き先を作るとその品目に付く() = pool {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_NEW_DESTINATION_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).performTextInput("行き先A")
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_CREATE).performClick()

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムA")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertTextContains("🏬 行き先A")
    }

    /** すでにあるチップを選ぶ経路。**作る経路とは別の結線**。 */
    @Test
    fun 既存のチップを選ぶとその品目に付く() = pool(twoItems()) {
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        onNodeWithTag(TestTags.itemFormCategoryChip(categoryId = 1)).performClick()
        onNodeWithTag(TestTags.itemFormDestinationChip(destinationId = 1)).performClick()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("アイテムC")
        onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()

        val row = onNodeWithTag(TestTags.poolRow(itemId = 3))
        row.assertTextContains("カテゴリー1")
        row.assertTextContains("🏬 行き先1")
    }

    // ---- 品目編集シート（画面 06） ----

    /**
     * 行の長押しで編集シートが開き、保存すると一覧に反映される（画面 01・06）。
     *
     * **長押しは行タップ（カゴの出し入れ）と同じ行に載っている**ので、
     * 取り違えるとカゴに入るだけでシートが開かない。
     */
    @Test
    fun 行の長押しで編集シートが開いて保存できる() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolRow(itemId = 2)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextClearance()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextInput("アイテムZ")
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertTextContains("アイテムZ")
    }

    /** 削除は確認ダイアログを挟む。**確認を押すまで消えない**（画面 06）。 */
    @Test
    fun 確認してから削除すると一覧から消える() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolRow(itemId = 2)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()

        // 確認を押すまでは消えない
        onNodeWithText("削除すると元に戻せません。").assertIsDisplayed()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertExists()

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertDoesNotExist()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
    }

    /** 最終購入日は編集シートでは YYYY-MM-DD（画面定義書 §2）。 */
    @Test
    fun 編集シートは最終購入日をYYYYMMDDで出す() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolRow(itemId = 1)).performTouchInput { longClick() }

        onNodeWithText("最終購入日: 2026-08-29").assertIsDisplayed()
    }

    @Test
    fun 未購入の品目は最終購入日をダッシュで出す() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolRow(itemId = 2)).performTouchInput { longClick() }

        onNodeWithText("最終購入日: —").assertIsDisplayed()
    }

    /**
     * 「なし」チップで行き先を外せる（画面 06）。
     *
     * **外れたことは「🏬 どこでも」で絞って確かめる**——行き先の名前は絞り込みチップにも
     * 出るので、画面から消えたかどうかでは見られない。
     */
    @Test
    fun なしチップで行き先を外せる() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolRow(itemId = 1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.ITEM_SHEET_DESTINATION_NONE_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_SAVE).performClick()

        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
        // カテゴリは触っていないので残る
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertTextContains("カテゴリー1")
    }

    // ---- 絞り込みチップ（画面 01） ----

    @Test
    fun カテゴリのチップで一覧が絞られる() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolCategoryChip(categoryId = 1)).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertDoesNotExist()
    }

    /** 行き先のチップは厳密。**「どこでも」は行き先なしだけ**を出す。 */
    @Test
    fun 行き先とどこでもで出る行が入れ替わる() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolDestinationChip(destinationId = 1)).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertDoesNotExist()

        onNodeWithTag(TestTags.POOL_CHIP_ALL).performClick()
        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertIsDisplayed()
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertDoesNotExist()
    }

    @Test
    fun すべてで絞り込みが解ける() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolCategoryChip(categoryId = 1)).performClick()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertDoesNotExist()

        onNodeWithTag(TestTags.POOL_CHIP_ALL).performClick()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertIsDisplayed()
        onNodeWithTag(TestTags.poolRow(itemId = 2)).assertIsDisplayed()
    }

    /** 絞り込んで 0 件になったときは、**空状態とは別の文言**を出す（画面 01）。 */
    @Test
    fun 絞り込んで0件なら別の文言が出る() = pool(twoItems()) {
        onNodeWithTag(TestTags.poolCategoryChip(categoryId = 1)).performClick()
        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()

        onNodeWithText("この条件のものはありません").assertIsDisplayed()
        onNodeWithText("まだ何も登録されていません").assertDoesNotExist()
    }
}
