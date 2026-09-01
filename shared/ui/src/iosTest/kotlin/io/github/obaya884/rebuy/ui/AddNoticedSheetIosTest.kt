package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.test.Test

/**
 * 気づいたものを足すシート（画面 05）の**画面段**。
 *
 * `AddNoticedViewModelTest` が見るのは中身と書き込みまでで、**04 の破線行から開くこと**、
 * **タップで閉じて今の一覧に現れること**、**他の行き先へ足したときのスナックバー**は
 * ここでしか見られない。
 *
 * 文言はリテラルで持つ（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class AddNoticedSheetIosTest {

    private val inBasket = ItemStatus.IN_SHOPPING_LIST

    /** 行き先 1 で買い物中。カゴに 1 件、未追加が今の店・他の店・どこでもに 1 件ずつ。 */
    private val seedShopping: FakeDatabase.() -> Unit = {
        seed(
            items = listOf(
                item(1, status = inBasket, destinationId = 1, name = "カゴの品"),
                item(2, destinationId = 1, name = "こめ"),
                item(3, destinationId = 2, name = "こむぎ"),
                item(4, name = "こおり")
            ),
            destinations = listOf(destination(1), destination(2, name = "行き先B"))
        )
    }

    /** 04 まで入って、破線行から 05 を開く。 */
    private fun sheet(
        prepare: FakeDatabase.() -> Unit = seedShopping,
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        startTestKoin(prepare)
        setContent { ReBuyApp() }
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()
        onNodeWithTag(TestTags.SHOPPING_ADD_NOTICED_ROW).performClick()
        block()
    }

    @Test
    fun 破線行から開くと未追加のものが並ぶ() = sheet {
        onNodeWithText("気づいたものを足す").assertExists()
        onNodeWithTag(TestTags.ADD_NOTICED_SECTION_UNADDED).assertExists()
        onNodeWithTag(TestTags.addNoticedRow(itemId = 2)).assertExists()
        // 他の行き先の枠は検索中だけ
        onNodeWithTag(TestTags.ADD_NOTICED_SECTION_ELSEWHERE).assertDoesNotExist()
    }

    /** 検索欄は自動フォーカス（画面 05）。打つと当たりが仕分けられる。 */
    @Test
    fun 打つと他の行き先の枠が出る() = sheet {
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).performTextInput("こむぎ")

        onNodeWithTag(TestTags.ADD_NOTICED_SECTION_ELSEWHERE).assertExists()
        // どの店のものかを添える（画面 05）
        onNodeWithTag(TestTags.addNoticedRow(itemId = 3)).assertTextEquals("こむぎ", "🏬 行き先B")
    }

    /** 今の行き先のものを足すと閉じて、**04 の一覧に現れる**（画面 05）。 */
    @Test
    fun 今の行き先のものを足すと一覧に現れる() = sheet {
        onNodeWithTag(TestTags.addNoticedRow(itemId = 2)).performClick()

        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertDoesNotExist()
        onNodeWithTag(TestTags.shoppingRow(itemId = 2)).assertExists()
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("0 / 2")
    }

    /**
     * 他の行き先のものは**今の一覧に現れない**ので、スナックバーで知らせる（画面 05・§2）。
     */
    @Test
    fun 他の行き先のものを足すとスナックバーで知らせる() = sheet {
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).performTextInput("こむぎ")
        onNodeWithTag(TestTags.addNoticedRow(itemId = 3)).performClick()

        onNodeWithText("行き先Bに追加しました").assertExists()
        onNodeWithTag(TestTags.shoppingRow(itemId = 3)).assertDoesNotExist()
    }

    /** 追加済みの行はタップしても閉じない（画面 05）。 */
    @Test
    fun 追加済みの行はタップできない() = sheet {
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).performTextInput("カゴの品")
        onNodeWithTag(TestTags.addNoticedRow(itemId = 1)).assertTextEquals("カゴの品", "追加済み")

        onNodeWithTag(TestTags.addNoticedRow(itemId = 1)).performClick()

        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertExists()
    }

    /** 「＋ この名前で登録する」で登録し、即カゴ入りして閉じる（画面 05）。 */
    @Test
    fun この名前で登録するとカゴに入って閉じる() = sheet {
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).performTextInput("あたらしい品")
        onNodeWithTag(TestTags.ADD_NOTICED_REGISTER).performClick()

        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertDoesNotExist()
        onNodeWithText("あたらしい品").assertExists()
        onNodeWithTag(TestTags.SHOPPING_PROGRESS).assertTextEquals("0 / 2")
    }

    /** 弾かれたらシートは閉じず、理由が検索欄の下に出る（画面定義書 §2）。 */
    @Test
    fun 同じ名前では弾かれて閉じない() = sheet {
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).performTextInput("こめ")
        onNodeWithTag(TestTags.ADD_NOTICED_REGISTER).performClick()

        onNodeWithText("同じ名前がすでにあります").assertExists()
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertExists()
    }

    /**
     * **2 回目も開けること。** ViewModel は 04 の entry に属してシートより長生きするので、
     * 閉じる合図を捨て損ねると二度と開かない（`RegisterViewModel` で実測）。
     */
    @Test
    fun 一度足したあとでもう一度開ける() = sheet {
        onNodeWithTag(TestTags.addNoticedRow(itemId = 2)).performClick()
        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertDoesNotExist()

        onNodeWithTag(TestTags.SHOPPING_ADD_NOTICED_ROW).performClick()

        onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertExists()
        // 打ちかけの検索語も残らない（画面定義書 §2）
        onNodeWithTag(TestTags.ADD_NOTICED_SECTION_ELSEWHERE).assertDoesNotExist()
    }
}
