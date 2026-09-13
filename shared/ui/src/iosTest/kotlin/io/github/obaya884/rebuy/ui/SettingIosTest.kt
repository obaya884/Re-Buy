package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** **本番 iOS の構成**で 07 を開く（[runIosApp]）。⚙ は外枠にあるので [IosAppProbe] から押す。 */
    private fun setting(block: ComposeUiTest.(IosAppProbe) -> Unit) = runIosApp { probe ->
        probe.tap(ReBuyAppBarIcon.SETTINGS)
        // 設定に着いたことを先に確かめる。ここが無いと、以下の「出さない行」の
        // 非存在は「そもそも設定を開けていない」でも通ってしまう
        probe.assertScreen("設定")
        block(probe)
    }

    /** 条項の 4 行が**この順で**並ぶ（画面 07）。存在だけ見ると入れ替えても緑になる。 */
    @Test
    fun 管理とテーマとライセンスの行がこの順で並ぶ() = setting {
        val order = listOf(
            TestTags.SETTING_ROW_CATEGORY_EDIT,
            TestTags.SETTING_ROW_DESTINATION_MANAGE,
            TestTags.SETTING_ROW_THEME,
            TestTags.SETTING_ROW_LICENSE
        )
        order.forEach { onNodeWithTag(it).assertIsDisplayed() }

        val sorted = order.sortedBy { onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y }
        assertEquals(order, sorted)
    }

    /** 行の文言。タグだけで掴むと、別の文言を渡す変異が素通りする。 */
    @Test
    fun 管理の2行は行き先とカテゴリを名乗る() = setting {
        onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).assertTextEquals("カテゴリの管理")
        onNodeWithTag(TestTags.SETTING_ROW_DESTINATION_MANAGE).assertTextEquals("行き先の管理")
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
    fun テーマを変えると行の表示も変わる() = setting { probe ->
        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertTextEquals("テーマ", "藍")

        onNodeWithTag(TestTags.SETTING_ROW_THEME).performClick()
        onNodeWithText("柿").performClick()
        probe.back()

        onNodeWithTag(TestTags.SETTING_ROW_THEME).assertTextEquals("テーマ", "柿")
    }

    /**
     * バージョンは**画面の下端に固定**される（画面 07）。行のすぐ下ではないので、
     * 最後の行との間が空く。
     *
     * 固定しているのは書式「Re-Buy x.y.z」で、版そのものではない。`VERSION_NAME` を
     * 組み合わせに使うのは**自己参照にならない**から——書式は `setting_version` が持ち、
     * 版は `gradle.properties` から生成される別系統（テスト戦略定義書 §2.1）。
     */
    @Test
    fun 最下部にバージョンが出る() = setting {
        onNodeWithTag(TestTags.SETTING_VERSION).assertTextEquals("Re-Buy $VERSION_NAME")

        val licenseNode = onNodeWithTag(TestTags.SETTING_ROW_LICENSE).fetchSemanticsNode()
        val versionNode = onNodeWithTag(TestTags.SETTING_VERSION).fetchSemanticsNode()
        val gap = versionNode.positionInRoot.y - (licenseNode.positionInRoot.y + licenseNode.size.height)

        // **最後の行の直下ではない。** 行の直下に置く実装だと隙間はほぼ 0 になる。
        // **行の高さを基準にする**——絶対値だと、外枠が被さって本文が下がったときに
        // 余裕が黙って目減りする（段 4 の Step 5 で実際に 44dp 縮んだ）
        assertTrue(
            gap > licenseNode.size.height,
            "バージョンは画面の下端に固定される（隙間 $gap・行の高さ ${licenseNode.size.height}）"
        )
    }
}
