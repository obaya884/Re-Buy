package io.github.obaya884.rebuy.ui.screen.add_noticed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.isInBasket
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.NameTextField
import io.github.obaya884.rebuy.ui.screen.ReBuyBottomSheet
import io.github.obaya884.rebuy.ui.screen.ReBuyRowCard
import io.github.obaya884.rebuy.ui.screen.SectionLabel
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 気づいたものを足すシート（画面 05）。買い物モード（04）の一覧の末尾から開く。
 *
 * **どの経路も 1 件で閉じる。** 検索欄は名前入力欄と同じ作法（打ち切り・エラーは下に赤字）で、
 * そのまま「＋ この名前で登録する」の入力にもなる（画面定義書 §2）。
 *
 * @param onAddedElsewhere 他の行き先へ足したときに、その行き先名で呼ばれる。
 *   **今の一覧には現れない**ので、呼び出し側がスナックバーで知らせる（画面 05）
 */
@Composable
fun AddNoticedSheet(
    route: Screen.Shopping,
    onAddedElsewhere: (destinationName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<AddNoticedViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsState()
    val closeRequest by viewModel.closeRequest.collectAsState()

    /** 閉じる道はすべてここを通す（`AddNoticedViewModel` の KDoc）。 */
    val dismiss = {
        viewModel.reset()
        onDismiss()
    }

    LaunchedEffect(closeRequest) {
        if (closeRequest.count > 0) {
            closeRequest.addedElsewhere?.let(onAddedElsewhere)
            dismiss()
        }
    }

    ReBuyBottomSheet(
        title = stringResource(Res.string.add_noticed_title),
        onDismiss = dismiss,
        spacing = 8.dp
    ) {
        NameTextField(
            value = uiState.query,
            onValueChange = viewModel::changeQuery,
            error = uiState.nameError,
            placeholder = stringResource(Res.string.add_noticed_search_placeholder),
            modifier = Modifier.testTag(TestTags.ADD_NOTICED_SEARCH_FIELD)
        )

        if (uiState.isUnaddedSectionVisible) {
            SectionLabel(
                text = stringResource(Res.string.add_noticed_section_unadded),
                testTag = TestTags.ADD_NOTICED_SECTION_UNADDED
            )
            uiState.hereItems.forEach { NoticedRow(it, onTap = viewModel::add) }
            if (uiState.anywhereItems.isNotEmpty()) {
                SectionLabel(
                    text = stringResource(Res.string.shopping_anywhere_section),
                    testTag = TestTags.ADD_NOTICED_SECTION_ANYWHERE
                )
                uiState.anywhereItems.forEach { NoticedRow(it, onTap = viewModel::add) }
            }
        }

        if (uiState.elsewhereRows.isNotEmpty()) {
            SectionLabel(
                text = stringResource(Res.string.add_noticed_section_elsewhere),
                testTag = TestTags.ADD_NOTICED_SECTION_ELSEWHERE
            )
            uiState.elsewhereRows.forEach { row ->
                NoticedRow(row.item, onTap = viewModel::add, destinationName = row.destinationName)
            }
        }

        if (uiState.canRegisterQuery) {
            TextButton(
                onClick = viewModel::registerQuery,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ADD_NOTICED_REGISTER)
            ) {
                Text(stringResource(Res.string.add_noticed_register))
            }
        }
    }
}

/**
 * 1 行。**追加済みはタップできない**（画面 05）——押しても何も起きない行に見せるより、
 * 「追加済み」と添えて理由を出す。
 *
 * @param destinationName 「他の行き先から」の行だけ添える
 */
@Composable
private fun NoticedRow(item: Item, onTap: (Item) -> Unit, destinationName: String? = null) {
    val isAdded = item.isInBasket
    ReBuyRowCard(
        highlighted = isAdded,
        onTap = { onTap(item) },
        testTag = TestTags.addNoticedRow(item.id),
        // 選んで閉じるだけで、✓ を付け外しする行ではない
        role = Role.Button,
        enabled = !isAdded
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAdded) ReBuyTheme.colors.muted else ReBuyTheme.colors.ink
            )
            destinationName?.let {
                Text(
                    // 🏬 は表示のときに前置する。名前の一部ではない（画面 01 と同じ）
                    text = stringResource(Res.string.pool_destination_prefix, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = ReBuyTheme.colors.muted
                )
            }
        }
        if (isAdded) {
            Text(
                text = stringResource(Res.string.add_noticed_already_added),
                style = MaterialTheme.typography.labelMedium,
                color = ReBuyTheme.colors.muted
            )
        }
    }
}
