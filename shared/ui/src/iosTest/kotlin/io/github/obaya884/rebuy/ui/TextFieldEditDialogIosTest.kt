package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.ui.screen.TextFieldEditDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 改名ダイアログの「開いたときの値 → 打ち直し → 確定」（テスト戦略定義書 §6 が T-38 として
 * 挙げていた穴のうち、入力から `onConfirm` までの経路）。
 *
 * **F-003 でこの経路が主導線になった。** 弾かれるとダイアログが開いたままになるので、
 * 「エラーを見て名前を直して確定し直す」ときに必ずここを通る。
 *
 * ### 分かっていること
 *
 * **カーソルは先頭に置かれる**ので、消さずに打つと頭に文字が入る（実測）。改名は
 * 打ち直しが普通なので実害は小さいが、新しい編集シート（画面 06・09b）を作るときは
 * 末尾にカーソルを置くことを検討する。
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldEditDialogIosTest {

    @Test
    fun 入力した名前が保持されて確定に渡る() = runComposeUiTest {
        var confirmed: Pair<Int, String>? = null
        var dismissed = false
        setContent {
            TextFieldEditDialog(
                title = "名前の変更",
                editId = 7,
                editName = "アイテム1",
                error = null,
                onConfirm = { id, name -> confirmed = id to name },
                onDismiss = { dismissed = true }
            )
        }

        // 開いた時点で今の名前が入っている
        onNodeWithText("アイテム1").assertExists()

        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("アイテム2")
        onNodeWithText("変更").performClick()

        assertEquals(7 to "アイテム2", confirmed)
        // 閉じるのは ViewModel。ダイアログは確定で閉じない（画面定義書 §2）
        assertFalse(dismissed)
    }
}
