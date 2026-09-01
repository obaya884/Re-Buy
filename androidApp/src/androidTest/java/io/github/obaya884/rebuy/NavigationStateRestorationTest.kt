package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.obaya884.rebuy.ui.ReBuyApp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test

/**
 * プロセス death からの復元で、いた画面とタブが元に戻ることを固定する。
 *
 * backstack の保存・復元は T-31 ステップ 13 で経路が変わった——Android 専用の reflection
 * （`NavKeySerializer` と 1 引数版の `rememberNavBackStack`）から、`SavedStateConfiguration` に
 * サブクラスを明示登録する形へ。**この載せ替えを通るテストが 1 件も無かった。**
 *
 * `NavigatorTest` は `NavigationState` を直接組み立てるので `rememberNavigationState` を呼ばず、
 * `ScreenSerializationTest` は登録の中身だけを見て呼び出し側との配線を見ない。
 * ここは `ReBuyApp()` をそのまま描いて `rememberSerializable` の Saver を実際に走らせるので、
 * **符号化と復号の両方**と、`screenSavedStateConfiguration` が実際に配線されていることが入る。
 */
class NavigationStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester = StateRestorationTester(composeRule)

    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private val poolTitle = string(Res.string.pool_title)
    private val settingTitle = string(Res.string.setting_title)

    /** ライセンス画面のタイトルは実装側がハードコードなので、ここでも文字列で持つ。 */
    private val licenseTitle = "ライセンス"
    private val licenseLabel = string(Res.string.setting_row_license)
    private val shoppingTitleAll = string(Res.string.shopping_title_all)

    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    @Test
    fun 復元後も同じ画面にいる() {
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseTitle)

        restorationTester.emulateSavedInstanceStateRestore()

        assertCurrentScreenIs(licenseTitle)
    }

    /**
     * 開いていた画面が復元されること。**プールにタブは無い**ので、設定を開いた状態で見る。
     */
    @Test
    fun 復元後も開いていた画面にいる() {
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        restorationTester.emulateSavedInstanceStateRestore()

        assertCurrentScreenIs(settingTitle)
    }

    /**
     * **`data object` ではないルートも保存・復元できること**（`Screen.Shopping(destinationId)`）。
     *
     * 上の 3 件はすべて `data object` のルートしか踏まないので、引数付きのルートで
     * 保存が落ちても全件緑になる。**引数の値まで見ているのは
     * `ScreenSerializationTest`（androidHostTest）**。行き先付きの 04 の復元は、
     * 実機の DB に行き先を作って消す手数が要るので置いていない（テスト戦略定義書 §6）。
     */
    @Test
    fun 引数を持つルートも保存復元できる() {
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("復元の確認用")
        composeRule.onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("復元の確認用").performClick()
        composeRule.onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.SHOPPING_START_ALL_ROW).performClick()
        try {
            assertCurrentScreenIs(shoppingTitleAll)

            restorationTester.emulateSavedInstanceStateRestore()

            assertCurrentScreenIs(shoppingTitleAll)
        } finally {
            // 実機の DB に残すと、次の実行が重複名で弾かれて別の理由で落ち続ける
            composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
            composeRule.onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).performClick()
            composeRule.onNodeWithText("復元の確認用").performTouchInput { longClick() }
            composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun 復元後もスタックの深さが保たれる() {
        // 上 2 件は「いちばん上のルート」しか見ない。積んである途中のルートまで
        // 復元されているかは、戻ってみないと分からない
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        // プール → 設定 → ライセンスの 3 段が残っていれば、戻るたびに 1 段ずつ下りる
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        assertCurrentScreenIs(poolTitle)
    }
}
