package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel が画面（NavEntry）ごとにスコープされることを確かめる。
 *
 * Navigation 3 では `rememberViewModelStoreNavEntryDecorator` が効いていないと
 * `koinViewModel()` が Activity の `ViewModelStore` にフォールバックし、全画面の
 * ViewModel が Activity スコープに昇格する。そうなると画面を離れても一時状態が残り、
 * 戻ってきたときにダイアログが開いたままになる。
 *
 * ダイアログの開閉フラグは UiState が持つ（CLAUDE.md「アーキテクチャ / UI 層」）ので、
 * それを外から観測できる一時状態として使う。踏むのは 09 の破線行から開く 02b。
 *
 * 逆向き（entry が backstack に残っている間は ViewModel が保持されること）は、プール（01）の
 * カテゴリの絞り込みで見る。選択は `PoolViewModel` が持ち、プールの上に設定を積むと 01 は
 * composition から外れるが backstack には残る。entry の `ViewModelStore` が composition と
 * 一緒に捨てられると、戻ったときに選択が「すべて」へ戻る。**こちらは Activity スコープへの
 * 昇格では落ちない**（昇格しても保持はされる）ので、昇格は 1 本目が見る。
 */
class ViewModelScopeTest {

    /** シードで振られたカテゴリの id。DB を空にしても採番は戻らない */
    private var categoryId = 0

    /** 絞り込みのチップは、品目が付いているカテゴリにだけ出る */
    @get:Rule(order = 0)
    val appState = TestAppStateRule {
        categoryId = category("カテゴリ1")
        item(Item(name = "アイテム1", categoryId = categoryId))
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private val addDialogTitle =
        runBlocking { getString(Res.string.item_form_dialog_category_title) }

    /** 設定の行の文言。プールのアプリバーからカテゴリの管理は外れた（画面 01）。 */
    private val categoryManageLabel = runBlocking { getString(Res.string.setting_row_category_edit) }

    private fun openSetting() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.waitForIdle()
    }

    /** **設定の下から開く。** 戻る矢印で戻る先も設定なので、2 回目はここだけを踏む。 */
    private fun openCategoryManage() {
        composeRule.onNodeWithText(categoryManageLabel).performClick()
        composeRule.waitForIdle()
    }

    private fun tapBackArrow() {
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        composeRule.waitForIdle()
    }

    private fun openAddDialog() {
        composeRule.onNodeWithTag(TestTags.MANAGE_ADD_ROW).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(addDialogTitle).assertIsDisplayed()
    }

    @Test
    fun 画面を離れて戻るとダイアログは閉じている() {
        openSetting()
        openCategoryManage()
        openAddDialog()

        tapBackArrow()
        openCategoryManage()

        // 画面が出ていないことを「ダイアログが無い」と読み違えないための錨
        composeRule.onNodeWithTag(TestTags.MANAGE_ADD_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(addDialogTitle).assertDoesNotExist()
    }

    @Test
    fun 上に画面を積んで戻っても下の画面の絞り込みは残っている() {
        val chip = TestTags.poolCategoryChip(categoryId)
        composeRule.onNodeWithTag(chip).performClick()
        composeRule.onNodeWithTag(chip).assertIsSelected()

        openSetting()
        tapBackArrow()

        composeRule.onNodeWithTag(chip).assertIsSelected()
    }
}
