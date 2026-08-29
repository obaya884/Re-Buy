package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.obaya884.rebuy.ui.activity.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 画面遷移の特性テスト。
 *
 * Navigation 3 への移行（T-18）で挙動が変わっていないことを確かめるために、移行前の挙動を写し取っている。
 * **移行後もこのファイルを 1 行も変えずに緑であること**が移行の合否判定になるので、
 * 移行で赤くなった場合に直すのはテストではなく実装。
 *
 * DB の中身に依存する遷移（買い物終了でホームへ戻る）は、データを用意する必要があるためここでは扱わない。
 */
@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /** 現在表示されている画面を TopAppBar のタイトルで判定する。 */
    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag("top_app_bar_title").assertTextEquals(title)
    }

    private fun pressBack() {
        Espresso.pressBack()
        composeRule.waitForIdle()
    }

    @Test
    fun 起動直後はホームが表示される() {
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ホームから設定へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_settings_button").performClick()
        assertCurrentScreenIs("設定")

        pressBack()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun 設定からライセンスへ遷移して端末の戻るで設定に帰る() {
        composeRule.onNodeWithTag("home_settings_button").performClick()
        composeRule.onNodeWithText("ライセンス").performClick()
        assertCurrentScreenIs("ライセンス")

        pressBack()
        assertCurrentScreenIs("設定")
    }

    @Test
    fun ホームからアイテム一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_item_edit_button").performClick()
        assertCurrentScreenIs("アイテム")

        pressBack()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ホームからカテゴリー一覧へ遷移して端末の戻るでホームに帰る() {
        composeRule.onNodeWithTag("home_category_edit_button").performClick()
        assertCurrentScreenIs("カテゴリー")

        pressBack()
        assertCurrentScreenIs("ホーム")
    }

    @Test
    fun ボトムナビでホームと買い物を往復できる() {
        composeRule.onNodeWithText("買い物").performClick()
        assertCurrentScreenIs("買い物")

        composeRule.onNodeWithText("ホーム").performClick()
        assertCurrentScreenIs("ホーム")
    }
}
