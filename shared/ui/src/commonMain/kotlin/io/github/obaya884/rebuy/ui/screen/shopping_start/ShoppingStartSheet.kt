package io.github.obaya884.rebuy.ui.screen.shopping_start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.tabularNumbers
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 買い物開始シート（画面 03）。プール（01）の「買い物を始める」から開く。
 *
 * **行き先を選ぶことが買い物の開始**なので、確定ボタンを置かず行タップで入る。
 * どこでも買えるものは独立した行にせず、各行の件数に「＋m」として足す（画面 03）。
 *
 * 04 買い物モードは F-009。それまで行タップは旧画面へ暫定で繋いである。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingStartSheet(onEnterShopping: (destinationId: Int?) -> Unit, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<ShoppingStartViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.shopping_start_title),
                style = MaterialTheme.typography.titleMedium,
                color = ReBuyTheme.colors.ink
            )
            Text(
                text = stringResource(Res.string.shopping_start_message),
                style = MaterialTheme.typography.bodyMedium,
                color = ReBuyTheme.colors.muted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.isAllMode) {
                // 行き先付きが 1 件も無いときは、内訳の代わりにこの 1 行だけ
                StartRow(
                    title = stringResource(Res.string.shopping_start_all, uiState.basketCount),
                    preview = null,
                    count = null,
                    testTag = TestTags.SHOPPING_START_ALL_ROW,
                    onClick = { onEnterShopping(null) }
                )
            } else {
                uiState.rows.forEach { row ->
                    StartRow(
                        title = row.name,
                        preview = row.preview.joinToString(PREVIEW_SEPARATOR),
                        count = if (row.anywhereCount > 0) {
                            stringResource(
                                Res.string.shopping_start_count_with_anywhere,
                                row.count,
                                row.anywhereCount
                            )
                        } else {
                            stringResource(Res.string.shopping_start_count, row.count)
                        },
                        testTag = TestTags.shoppingStartRow(row.destinationId),
                        onClick = { onEnterShopping(row.destinationId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartRow(
    title: String,
    preview: String?,
    count: String?,
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
            preview?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = ReBuyTheme.colors.muted
                )
            }
        }
        count?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                color = ReBuyTheme.colors.muted
            )
        }
    }
}

/** プレビューの区切り。「牛乳・食パン」のように**中黒でつなぐ**（画面 03）。 */
private const val PREVIEW_SEPARATOR = "・"
