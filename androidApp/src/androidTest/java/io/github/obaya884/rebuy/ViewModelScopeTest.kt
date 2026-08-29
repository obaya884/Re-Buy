package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.obaya884.rebuy.ui.R
import io.github.obaya884.rebuy.ui.TestTags
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
 * それを外から観測できる唯一の一時状態として使う。
 *
 * 逆向き（entry が backstack に残っている間は ViewModel が保持されること）は、
 * 買い物画面の終了確認ダイアログを開くのにチェック済みの品目が要るため、
 * DB を差し替えられるようになってから書く（技術改善バックログ T-21）。
 */
class ViewModelScopeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val addDialogTitle
        get() = composeRule.activity.getString(R.string.category_edit_add_dialog_title)

    private fun openCategoryEdit() {
        composeRule.onNodeWithTag(TestTags.HOME_CATEGORY_EDIT_BUTTON).performClick()
        composeRule.waitForIdle()
    }

    private fun tapBackArrow() {
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun 画面を離れて戻るとダイアログは閉じている() {
        openCategoryEdit()
        composeRule.onNodeWithTag(TestTags.CATEGORY_EDIT_ADD_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(addDialogTitle).assertIsDisplayed()

        tapBackArrow()
        openCategoryEdit()

        // 画面が出ていないことを「ダイアログが無い」と読み違えないための錨
        composeRule.onNodeWithTag(TestTags.CATEGORY_EDIT_ADD_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(addDialogTitle).assertDoesNotExist()
    }
}
