package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
