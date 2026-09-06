package io.github.obaya884.rebuy.ui.screen.shopping_start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.screen.ReBuyBottomSheet
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.tabularNumbers
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 買い物開始シート（画面 03）。プール（01）の「買い物を始める」から開く。
 *
 * **行き先を選ぶことが買い物の開始**なので、確定ボタンを置かず行タップで入る。
 * 内訳の作り方は `ShoppingStartSheetUiState`。
 */
@Composable
fun ShoppingStartSheet(onEnterShopping: (destinationId: Int?) -> Unit, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<ShoppingStartViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    ReBuyBottomSheet(
        title = stringResource(Res.string.shopping_start_title),
        onDismiss = onDismiss,
        spacing = 8.dp
    ) {
        Text(
            text = stringResource(Res.string.shopping_start_message),
            style = MaterialTheme.typography.bodyMedium,
            color = ReBuyTheme.colors.muted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        uiState.rows.forEach { row ->
            StartRow(
                title = row.name,
                preview = row.preview.joinToString(PREVIEW_SEPARATOR),
                count = if (uiState.hasAnywhere) {
                    stringResource(
                        Res.string.shopping_start_count_with_anywhere,
                        row.count,
                        uiState.anywhereCount
                    )
                } else {
                    stringResource(Res.string.shopping_start_count, row.count)
                },
                testTag = TestTags.shoppingStartRow(row.destinationId),
                onClick = { onEnterShopping(row.destinationId) }
            )
        }

        if (uiState.hasAnywhere) {
            // 「＋m」が何なのかは行の中では言えないので、内訳の下でまとめて言う（画面 03）
            Text(
                text = stringResource(
                    Res.string.shopping_start_anywhere_note,
                    uiState.anywhereCount
                ),
                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                color = ReBuyTheme.colors.muted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StartRow(
    title: String,
    preview: String,
    count: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ReBuyTheme.colors.ink
            )
            Text(
                text = preview,
                style = MaterialTheme.typography.labelMedium,
                color = ReBuyTheme.colors.muted
            )
        }
        // 件数は行をまたいで右端で縦に並ぶので等幅にする（画面定義書 §5）
        Text(
            text = count,
            style = MaterialTheme.typography.labelMedium.tabularNumbers(),
            color = ReBuyTheme.colors.muted
        )
    }
}

/** プレビューの区切り。「牛乳・食パン」のように**中黒でつなぐ**（画面 03）。 */
private const val PREVIEW_SEPARATOR = "・"
