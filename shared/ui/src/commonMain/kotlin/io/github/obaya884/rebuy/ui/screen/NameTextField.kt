package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.domain.NameRule
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.name_error_blank
import io.github.obaya884.rebuy.ui.resources.name_error_duplicate
import io.github.obaya884.rebuy.ui.resources.name_error_too_long
import org.jetbrains.compose.resources.stringResource

/**
 * 名前の入力欄（画面定義書 §2「名前入力の共通仕様」）。品目・カテゴリー・行き先で共通。
 *
 * **UI 側の網は 2 つ。** 上限を超える入力を打ち切ることと、確定時に弾かれた理由を
 * 入力欄の下に出すこと。データ層の網は `NameRule` と Repository が持つ。
 *
 * **[modifier] は入力欄そのものに付く。** 呼び出し側が渡すのはテストタグで、外枠に付けても
 * 届かない（テストから入力できない。実測。かつてここへフォーカス要求も渡していて、
 * 自動フォーカスが効かない形で踏んだ）。エラー文言は入力欄の下に積む。
 */
@Composable
fun NameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: NameError?,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    Column {
        TextField(
            value = value,
            // 31 文字目以降は入力できない。**打ち切りであって、エラーにはしない**
            onValueChange = { onValueChange(NameRule.truncate(it)) },
            singleLine = true,
            isError = error != null,
            placeholder = placeholder?.let { { Text(it) } },
            modifier = modifier.fillMaxWidth()
        )
        nameErrorMessage(error)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 弾かれた理由に対応する文言。**どの理由にも文言を置く**（画面定義書 §2）——
 * 起きない想定の理由でも、文言が無いと赤枠だけが出て理由の分からない画面になる。
 */
@Composable
private fun nameErrorMessage(error: NameError?): String? = when (error) {
    null -> null
    NameError.BLANK -> stringResource(Res.string.name_error_blank)
    NameError.DUPLICATE -> stringResource(Res.string.name_error_duplicate)
    NameError.TOO_LONG -> stringResource(Res.string.name_error_too_long)
}
