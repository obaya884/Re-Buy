package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.obaya884.rebuy.ui.ReBuyApp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
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

    private val homeTitle = string(Res.string.home_title)
    private val shoppingTitle = string(Res.string.shopping_title)
    private val settingTitle = string(Res.string.setting_title)

    /** ライセンス画面のタイトルは実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"

    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    @Test
    fun 復元後も同じ画面にいる() {
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        restorationTester.emulateSavedInstanceStateRestore()

        assertCurrentScreenIs(licenseLabel)
    }

    @Test
    fun 復元後も同じタブにいる() {
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.bottomNavItem(BottomNavigationItem.Shopping))
            .performClick()
        assertCurrentScreenIs(shoppingTitle)

        restorationTester.emulateSavedInstanceStateRestore()

        assertCurrentScreenIs(shoppingTitle)
    }

    @Test
    fun 復元後もスタックの深さが保たれる() {
        // 上 2 件は「いちばん上のルート」しか見ない。積んである途中のルートまで
        // 復元されているかは、戻ってみないと分からない
        restorationTester.setContent { ReBuyApp() }

        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        // ホーム → 設定 → ライセンスの 3 段が残っていれば、戻るたびに 1 段ずつ下りる
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        assertCurrentScreenIs(homeTitle)
    }
}
