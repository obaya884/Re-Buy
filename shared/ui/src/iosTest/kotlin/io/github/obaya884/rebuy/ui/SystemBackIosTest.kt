package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import kotlin.test.Test

/**
 * iOS の端末の戻り（端スワイプ）が画面定義書 §2 の条項に従うこと。04 で離脱確認が**出ること**と
 * 05 は `ShoppingIosTest`、ハンドラの有効・無効は `SystemBackHandlerIosTest` が持つ。
 *
 * **閉じたものだけでなく、残るものも見る。** 閉じたことだけだと、戻りがその下の画面まで
 * 抜けた場合も緑になる（FB-18）。
 *
 * **OS のジェスチャは経由しない**（[pressSystemBack]）ので、端スワイプが効かない OS の版でも走る。
 */
@OptIn(ExperimentalTestApi::class)
class SystemBackIosTest {

    /** カゴに 1 件、カテゴリーと行き先を 1 件ずつ。全画面の入口に足りる。 */
    private val seedAll: FakeDatabase.() -> Unit = {
        seed(
            items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST, destinationId = 1)),
            categories = listOf(category(id = 1, name = "カテゴリA")),
            destinations = listOf(destination(1))
        )
    }

    private fun app(block: ComposeUiTest.(IosAppProbe) -> Unit) = runIosApp(seedAll, block = block)

    private fun ComposeUiTest.assertOnPool(probe: IosAppProbe) {
        probe.assertScreen(ScreenTitle.pool)
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertExists()
    }

    private fun ComposeUiTest.openCategoryManage(probe: IosAppProbe) {
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()
        probe.assertScreen(ScreenTitle.categoryManage)
    }

    private fun ComposeUiTest.openCategorySheet() {
        onNodeWithText("カテゴリA").performTouchInput { longClick() }
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertExists()
    }

    private fun ComposeUiTest.openItemSheet() {
        onNodeWithTag(TestTags.poolRow(itemId = 1)).performTouchInput { longClick() }
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()
    }

    private fun ComposeUiTest.openItemDeleteDialog() {
        openItemSheet()
        onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()
        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).assertExists()
    }

    private fun ComposeUiTest.openCategoryDeleteDialog(probe: IosAppProbe) {
        openCategoryManage(probe)
        openCategorySheet()
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE).performClick()
        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE_CONFIRM).assertExists()
    }

    private fun ComposeUiTest.openShopping() {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()
        onNodeWithTag(TestTags.SHOPPING_FINISH_BUTTON).assertExists()
    }

    // ---- ダイアログ ----

    @Test
    fun 離脱確認の表示中はダイアログだけ閉じる() = app { probe ->
        openShopping()
        probe.back()
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertDoesNotExist()
        onNodeWithTag(TestTags.SHOPPING_FINISH_BUTTON).assertExists()
    }

    @Test
    fun 品目の削除確認の表示中はダイアログだけ閉じる() = app { probe ->
        openItemDeleteDialog()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).assertDoesNotExist()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()
    }

    @Test
    fun カテゴリの削除確認の表示中はダイアログだけ閉じる() = app { probe ->
        openCategoryDeleteDialog(probe)

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.MANAGE_SHEET_DELETE_CONFIRM).assertDoesNotExist()
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertExists()
    }

    // ---- シート ----

    @Test
    fun 登録シートはシートだけ閉じる() = app { probe ->
        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        assertOnPool(probe)
    }

    @Test
    fun 行き先を選ぶシートはシートだけ閉じる() = app { probe ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).assertDoesNotExist()
        assertOnPool(probe)
    }

    @Test
    fun 品目の編集シートはシートだけ閉じる() = app { probe ->
        openItemSheet()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        assertOnPool(probe)
    }

    @Test
    fun カテゴリの編集シートはシートだけ閉じる() = app { probe ->
        openCategoryManage(probe)
        openCategorySheet()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
        probe.assertScreen(ScreenTitle.categoryManage)
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()
    }

    /** 保存されていない入力は破棄する（画面定義書 §2）。開き直して中身まで見る。 */
    @Test
    fun 品目の編集シートの入力は戻ると破棄される() = app { probe ->
        openItemSheet()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).performTextReplacement("書き換えた名前")
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertTextEquals("書き換えた名前")

        pressSystemBack(probe.systemBack)

        onNodeWithText("書き換えた名前").assertDoesNotExist()
        openItemSheet()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertTextEquals("アイテム1")
    }

    @Test
    fun 登録シートの入力は戻ると破棄される() = app { probe ->
        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("書きかけ")
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertTextEquals("書きかけ")

        pressSystemBack(probe.systemBack)

        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertExists()
        onNodeWithText("書きかけ").assertDoesNotExist()
    }

    // ---- 02b（シートの上と、09 の画面の上に出る） ----

    /** 閉じるのはダイアログだけで、親のシートの入力は残る。 */
    @Test
    fun 登録シートの上の新規作成ダイアログはダイアログだけ閉じる() = app { probe ->
        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("書きかけ")
        onNodeWithTag(TestTags.ITEM_FORM_NEW_CATEGORY_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertTextEquals("書きかけ")
    }

    @Test
    fun 編集シートの上の新規作成ダイアログはダイアログだけ閉じる() = app { probe ->
        openItemSheet()
        onNodeWithTag(TestTags.ITEM_FORM_NEW_DESTINATION_CHIP).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()
    }

    @Test
    fun カテゴリの管理の新規作成ダイアログはダイアログだけ閉じる() = app { probe ->
        openCategoryManage(probe)
        onNodeWithTag(TestTags.MANAGE_ADD_ROW).performClick()
        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_FORM_DIALOG_NAME_FIELD).assertDoesNotExist()
        probe.assertScreen(ScreenTitle.categoryManage)
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()
    }

    // ---- 重なりを 1 枚ずつ剥がす ----

    @Test
    fun 品目の削除確認から戻りを重ねると1枚ずつ閉じる() = app { probe ->
        openItemDeleteDialog()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        assertOnPool(probe)
    }

    @Test
    fun カテゴリの削除確認から戻りを重ねると1枚ずつ閉じる() = app { probe ->
        openCategoryDeleteDialog(probe)

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()

        pressSystemBack(probe.systemBack)
        probe.assertScreen(ScreenTitle.setting)
    }

    @Test
    fun 新規作成ダイアログから戻りを重ねると1枚ずつ閉じる() = app { probe ->
        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.ITEM_FORM_NEW_CATEGORY_CHIP).performClick()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        assertOnPool(probe)
    }

    /** 閉じたハンドラが残っていると、2 回目の戻りをそれが食う。 */
    @Test
    fun シートを閉じたあとの戻りは下の画面が受ける() = app { probe ->
        openCategoryManage(probe)
        openCategorySheet()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
        probe.assertScreen(ScreenTitle.categoryManage)
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()

        pressSystemBack(probe.systemBack)
        probe.assertScreen(ScreenTitle.setting)
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertExists()
    }

    @Test
    fun 離脱確認は閉じても次の戻りでまた出る() = app { probe ->
        openShopping()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertExists()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertDoesNotExist()

        pressSystemBack(probe.systemBack)
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertExists()
    }

    // ---- 引きかけてやめる ----

    @Test
    fun シートは引きかけてやめると開いたまま() = app { probe ->
        openItemSheet()

        cancelSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()
    }

    @Test
    fun ダイアログは引きかけてやめると開いたまま() = app { probe ->
        openItemDeleteDialog()

        cancelSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).assertExists()
    }

    /** やめた直後の戻りが効かないと、指を離し直した 1 回が無反応になる。 */
    @Test
    fun 引きかけてやめたあとの戻りで閉じる() = app { probe ->
        openItemSheet()
        cancelSystemBack(probe.systemBack)

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        assertOnPool(probe)
    }

    @Test
    fun 画面は引きかけてやめると留まりそのあとの戻りで1段戻る() = app { probe ->
        openCategoryManage(probe)

        cancelSystemBack(probe.systemBack)

        probe.assertScreen(ScreenTitle.categoryManage)
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()

        pressSystemBack(probe.systemBack)

        probe.assertScreen(ScreenTitle.setting)
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertExists()
    }

    // ---- 進捗を挟まない戻り（Escape キーと VoiceOver のエスケープ） ----

    @Test
    fun 進捗なしの戻りでもシートだけ閉じる() = app { probe ->
        openCategoryManage(probe)
        openCategorySheet()

        pressBackWithoutProgress(probe.systemBack)

        onNodeWithTag(TestTags.MANAGE_SHEET_NAME_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.manageRow(id = 1)).assertExists()
    }

    @Test
    fun 進捗なしの戻りでもダイアログだけ閉じる() = app { probe ->
        openItemDeleteDialog()

        pressBackWithoutProgress(probe.systemBack)

        onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).assertDoesNotExist()
        onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertExists()
    }

    // ---- 画面 ----

    @Test
    fun 設定からは1段だけ戻ってプールに帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertExists()

        pressSystemBack(probe.systemBack)

        assertOnPool(probe)
    }

    @Test
    fun カテゴリの管理からは1段だけ戻って設定に帰る() = app { probe ->
        openCategoryManage(probe)

        pressSystemBack(probe.systemBack)

        probe.assertScreen(ScreenTitle.setting)
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertExists()
    }

    @Test
    fun ライセンスからは1段ずつプールまで帰る() = app { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        onNodeWithTag(TestTags.SETTING_ROW_LICENSE).performClick()
        probe.assertScreen(ScreenTitle.license)

        pressSystemBack(probe.systemBack)

        probe.assertScreen(ScreenTitle.setting)
        onNodeWithTag(TestTags.SETTING_ROW_LICENSE).assertExists()

        pressSystemBack(probe.systemBack)

        assertOnPool(probe)
    }

    /** 根には戻り先が無い。落ちず、プールが出たままで、そのあとのシートも戻りで閉じる。 */
    @Test
    fun プールで戻っても壊れない() = app { probe ->
        pressSystemBack(probe.systemBack)

        assertOnPool(probe)

        probe.tap(ReBuyAppBarIcon.ADD)
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertExists()

        pressSystemBack(probe.systemBack)

        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
    }
}
