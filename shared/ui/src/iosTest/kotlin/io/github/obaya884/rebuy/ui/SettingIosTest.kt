package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

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
        // 設定に着いたことを先に確かめる。ここが無いと、以下の「出さない行」の
        // 非存在は「そもそも設定を開けていない」でも通ってしまう
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals("設定")
        block()
    }

    /** **「行き先の管理」の行は F-013 でここに足す**（管理画面がまだ無い）。 */
    @Test
    fun 管理とテーマとライセンスの行が並ぶ() = setting {
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTING_ROW_LICENSE).assertIsDisplayed()
    }

    /**
     * 利用規約・プライバシーポリシー・問い合わせは**リリース前まで行ごと出さない**（画面 07）。
     *
     * **語を短く取って部分一致で見る。** 完全一致だと「利用規約について」のように
     * 1 字違う文言で復活したときに素通りする。
     */
    @Test
    fun リリース前まで出さない行は並べない() = setting {
        onNodeWithText("利用規約", substring = true).assertDoesNotExist()
        onNodeWithText("プライバシー", substring = true).assertDoesNotExist()
        onNodeWithText("問い合わせ", substring = true).assertDoesNotExist()
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

    /**
     * バージョンは**行より下**に出る（画面 07 の「最下部に」）。
     *
     * 固定しているのは書式「Re-Buy x.y.z」で、版そのものではない。`VERSION_NAME` を
     * 組み合わせに使うのは**自己参照にならない**から——書式は `setting_version` が持ち、
     * 版は `gradle.properties` から生成される別系統（テスト戦略定義書 §2.1）。
     */
    @Test
    fun 最下部にバージョンが出る() = setting {
        onNodeWithTag(TestTags.SETTING_VERSION).assertTextEquals("Re-Buy $VERSION_NAME")

        val license = onNodeWithTag(TestTags.SETTING_ROW_LICENSE).fetchSemanticsNode().positionInRoot.y
        val version = onNodeWithTag(TestTags.SETTING_VERSION).fetchSemanticsNode().positionInRoot.y
        assertTrue(license < version, "バージョンは行より下")
    }
}
