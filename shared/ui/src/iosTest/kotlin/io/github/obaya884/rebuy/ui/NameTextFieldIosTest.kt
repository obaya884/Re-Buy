package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.screen.NameTextField
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 名前入力欄の UI 側の網（画面定義書 §2）。**打ち切りとエラー表示はここでしか見られない。**
 *
 * 打ち切りが効かないと 31 文字目以降が入力でき、データ層が `TOO_LONG` で弾く——
 * その文言は画面定義書に無いので、**利用者には何も出ないまま確定できない**状態になる。
 *
 * **`commonTest` には置けない**（画面を動かすテストの置き場所はテスト戦略定義書 §1）。
 */
@OptIn(ExperimentalTestApi::class)
class NameTextFieldIosTest {

    /**
     * 入力の状態は `mutableStateOf` で持つ。**素の `var` だと入力欄が再構成されず、
     * 1 文字ずつ空文字へ書き戻されて打ち切りを観測できない**（実測）。
     *
     * 入力欄は `hasSetTextAction()` で引く。`NameTextField` の `modifier` は外枠の
     * `Column` に付くので、テストタグでは**入力を受けないノード**に当たる（実測）。
     */
    private fun ComposeUiTest.typeInto(text: String): MutableState<String> {
        val name = mutableStateOf("")
        setContent {
            NameTextField(
                value = name.value,
                onValueChange = { name.value = it },
                error = null
            )
        }

        onNode(hasSetTextAction()).performTextInput(text)
        return name
    }

    /** 期待値は長さではなく文字列で見る。**どちら側を落としたか**まで固定するため。 */
    @Test
    fun 上限までは入力できる() = runComposeUiTest {
        val name = "あいうえおかきくけこ".repeat(3)

        assertEquals(name, typeInto(name).value)
    }

    /** 31 文字目以降は**打ち切り**。エラーにはしない。 */
    @Test
    fun 上限を超えた入力は打ち切られる() = runComposeUiTest {
        val typed = "あいうえおかきくけこ".repeat(4)

        assertEquals("あいうえおかきくけこ".repeat(3), typeInto(typed).value)
    }

    /** 期待値はリテラルで書く（テスト戦略定義書 §2.1）。リソースから引くと自己参照になる。 */
    @Test
    fun 弾かれた理由が入力欄の下に出る() = runComposeUiTest {
        setContent {
            NameTextField(value = "", onValueChange = {}, error = NameError.DUPLICATE)
        }

        onNodeWithText("同じ名前がすでにあります").assertIsDisplayed()
        onNodeWithText("名前を入力してください").assertDoesNotExist()
    }

    @Test
    fun 空のエラーにも文言が出る() = runComposeUiTest {
        setContent {
            NameTextField(value = "", onValueChange = {}, error = NameError.BLANK)
        }

        onNodeWithText("名前を入力してください").assertIsDisplayed()
        onNodeWithText("同じ名前がすでにあります").assertDoesNotExist()
    }

    @Test
    fun 弾かれていなければ文言は出ない() = runComposeUiTest {
        setContent {
            NameTextField(value = "アイテム1", onValueChange = {}, error = null)
        }

        onNodeWithText("名前を入力してください").assertDoesNotExist()
        onNodeWithText("同じ名前がすでにあります").assertDoesNotExist()
        onNodeWithText("30 文字以内で入力してください").assertDoesNotExist()
    }

    /**
     * **打ち切りがあるので通常は届かない理由にも文言を出す**（画面定義書 §2）。
     * 文言が無いと、赤枠だけが出て理由の分からない画面になる。
     */
    @Test
    fun 上限超えのエラーにも文言が出る() = runComposeUiTest {
        setContent {
            NameTextField(value = "", onValueChange = {}, error = NameError.TOO_LONG)
        }

        onNodeWithText("30 文字以内で入力してください").assertIsDisplayed()
    }
}
