package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * 設定（画面 07）の**画面段**。
 *
 * 見るのは 3 つ。**出す行と出さない行**、**テーマ行の右端に今の選択が出ること**、
 * そして**バージョンが出ること**。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。**テーマの既定は藍**なので、
 * 選択に依存するテストは事前状態を自分で確かめる（`IosTestKoin` の KDoc）。
 */
@OptIn(ExperimentalTestApi::class)
class SettingIosTest {

    private fun setting(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        startTestKoin()
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        block()
    }

    @Test
    fun 管理とテーマとライセンスの行が並ぶ() = setting {
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertExists()
        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertExists()
        onNodeWithTag(TestTags.SETTING_ROW_LICENSE).assertExists()
    }

    /** 利用規約・プライバシーポリシー・問い合わせは**リリース前まで行ごと出さない**（画面 07）。 */
    @Test
    fun リリース前まで出さない行は並べない() = setting {
        onNodeWithText("利用規約").assertDoesNotExist()
        onNodeWithText("プライバシーポリシー").assertDoesNotExist()
        onNodeWithText("お問い合わせ・機能リクエスト").assertDoesNotExist()
    }

    /** 開かなくても今どれを選んでいるか分かる（画面 07）。既定は藍。 */
    @Test
    fun テーマ行の右端に今のテーマ名が出る() = setting {
        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertTextEquals("テーマ", "藍")
    }

    /** 08 で選び直すと、戻った 07 の行にも追随する。 */
    @Test
    fun テーマを変えると行の表示も変わる() = setting {
        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertTextEquals("テーマ", "藍")

        onNodeWithTag(TestTags.SETTING_ROW_THEME).performClick()
        onNodeWithText("柿").performClick()
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()

        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertTextEquals("テーマ", "柿")
    }

    @Test
    fun 最下部にバージョンが出る() = setting {
        onNodeWithTag(TestTags.SETTING_VERSION).assertTextEquals("Re-Buy $VERSION_NAME_FOR_TEST")
    }

    private companion object {
        /**
         * 実装が読むのは生成された `VERSION_NAME`（`gradle.properties` 由来）。
         * **テスト側は生成物を参照せずリテラルで持つ**（テスト戦略定義書 §2.1）ので、
         * 版を上げたらここも直す。
         */
        const val VERSION_NAME_FOR_TEST = "0.0.1"
    }
}
