package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.screen.TextFieldAddDialog
import io.github.obaya884.rebuy.ui.screen.TextFieldEditDialog
import io.github.obaya884.rebuy.ui.screen.NewNameDialog
import io.github.obaya884.rebuy.ui.screen.NewNameDialogState
import io.github.obaya884.rebuy.ui.screen.NewNameTarget
import kotlin.test.Test

/**
 * 名前を入力する 3 つのダイアログが、弾かれた理由を**入力欄の下に渡している**こと
 * （画面定義書 §2）。
 *
 * `NameTextFieldIosTest` は入力欄そのものを描くので、**ダイアログ側の `error = error` を
 * `null` に落とす変異はそこでは捕まらない**（実測）。ViewModel が理由を立てることと、
 * それが画面に出ることの間を繋ぐのがこのテスト。
 *
 * 期待値はリテラルで書く（テスト戦略定義書 §2.1）。
 */
@OptIn(ExperimentalTestApi::class)
class NameDialogErrorIosTest {

    @Test
    fun 追加ダイアログが理由を出す() = runComposeUiTest {
        setContent {
            TextFieldAddDialog(
                title = "カテゴリーの追加",
                error = NameError.DUPLICATE,
                onConfirm = {},
                onDismiss = {}
            )
        }

        onNodeWithText("同じ名前がすでにあります").assertIsDisplayed()
    }

    @Test
    fun 改名ダイアログが理由を出す() = runComposeUiTest {
        setContent {
            TextFieldEditDialog(
                title = "名前の変更",
                editId = 1,
                editName = "カテゴリー1",
                error = NameError.BLANK,
                onConfirm = { _, _ -> },
                onDismiss = {}
            )
        }

        onNodeWithText("名前を入力してください").assertIsDisplayed()
    }

    /** 02b の新規作成ダイアログ。登録シートと品目編集シートが同じものを内包する。 */
    @Test
    fun 新規作成ダイアログが理由を出す() = runComposeUiTest {
        setContent {
            NewNameDialog(
                state = NewNameDialogState(
                    target = NewNameTarget.CATEGORY,
                    error = NameError.TOO_LONG
                ),
                onNameChange = {},
                onCreate = {},
                onDismiss = {}
            )
        }

        onNodeWithText("30 文字以内で入力してください").assertIsDisplayed()
    }
}
