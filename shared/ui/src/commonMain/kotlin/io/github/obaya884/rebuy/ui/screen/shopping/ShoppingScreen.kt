package io.github.obaya884.rebuy.ui.screen.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.SystemBackHandler
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.tabularNumbers
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 買い物モード（画面 04）。開始シート（03）で行き先を選ぶと入る。
 *
 * **行タップはチェックの付け外しだけ**で、チェックしても行は動かない（01 と同じ作法）。
 * 「買い物を終了する」でチェック済みだけをプールへ戻し、**未チェックはカゴに残す**——
 * 買えなかったものを次の買い物へ持ち越せるように。
 *
 * ← とシステムバックは離脱確認を挟む。**チェックは DB にあるので離脱しても消えない**が、
 * 買い物の途中で誤って抜けると立ち止まることになるので、一度止める（画面 04）。
 */
@Composable
fun ShoppingScreen(
    route: Screen.Shopping,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    // 行き先はルートが持つ。**キーごと渡す**——`Int?` を parametersOf で渡すと、
    // 全件モード（null）が「引数が無い」と見分けられない（UiModule の定義側と対）
    val viewModel = koinViewModel<ShoppingViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsState()
    var isLeaveDialogOpen by remember { mutableStateOf(false) }

    val leave = { navigator.goBack() }

    SystemBackHandler { isLeaveDialogOpen = true }

    ReBuyAppScaffold(
        topBarTitle = when {
            uiState.isAllMode -> stringResource(Res.string.shopping_title_all)
            // 行き先を読み込むまでは出さない。**空にしておくほうが、全件モードの
            // タイトルが一瞬出て入れ替わるより誤解が少ない**
            else -> uiState.destinationName?.let { stringResource(Res.string.shopping_title, it) }.orEmpty()
        },
        topBarNavigationIcon = {
            IconButton(
                onClick = { isLeaveDialogOpen = true },
                modifier = Modifier.testTag(TestTags.BACK_BUTTON)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        topBarActions = {
            Text(
                text = stringResource(
                    Res.string.shopping_progress,
                    uiState.checkedCount,
                    uiState.totalCount
                ),
                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                color = ReBuyTheme.colors.muted,
                modifier = Modifier.padding(end = 16.dp).testTag(TestTags.SHOPPING_PROGRESS)
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.ofDestination, key = { it.id }) { item ->
                    ShoppingRow(item = item, onTap = { viewModel.toggleCheck(item) })
                }
                if (uiState.anywhere.isNotEmpty()) {
                    item { AnywhereSectionLabel() }
                    items(uiState.anywhere, key = { it.id }) { item ->
                        ShoppingRow(item = item, onTap = { viewModel.toggleCheck(item) })
                    }
                }
                // 暫定: 一覧末尾の「＋ 気づいたものを足す」（05 へ）は F-010
            }

            Button(
                onClick = { viewModel.finishShopping { navigator.popToRoot() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(TestTags.SHOPPING_FINISH_BUTTON)
            ) {
                Text(stringResource(Res.string.shopping_finish))
            }
        }
    }

    if (isLeaveDialogOpen) {
        LeaveDialog(
            onConfirm = {
                isLeaveDialogOpen = false
                leave()
            },
            onCancel = { isLeaveDialogOpen = false }
        )
    }
}

/** 一覧の 1 行。チェック済みは**取り消し線と ✓ の 2 通り**で分かるようにする。 */
@Composable
private fun ShoppingRow(item: Item, onTap: () -> Unit) {
    val isChecked = item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) ReBuyTheme.colors.accentSoft else ReBuyTheme.colors.card
        ),
        modifier = Modifier
            .fillMaxWidth()
            // clip を先に置く（リップルがカードの角丸からはみ出るため。01 と同じ）
            .clip(CardDefaults.shape)
            .clickable(role = Role.Checkbox, onClick = onTap)
            .testTag(TestTags.shoppingRow(item.id))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isChecked) ReBuyTheme.colors.muted else ReBuyTheme.colors.ink,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            if (isChecked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ReBuyTheme.colors.accent)
            }
        }
    }
}

/** 「どこでも買えるもの」の区切り。全件モードでは出ない（群が 1 つしかない）。 */
@Composable
private fun AnywhereSectionLabel() {
    Text(
        text = stringResource(Res.string.shopping_anywhere_section),
        style = MaterialTheme.typography.labelMedium,
        color = ReBuyTheme.colors.muted,
        modifier = Modifier.padding(top = 8.dp).testTag(TestTags.SHOPPING_ANYWHERE_SECTION)
    )
}

@Composable
private fun LeaveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.shopping_leave_dialog_title)) },
        text = { Text(stringResource(Res.string.shopping_leave_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.SHOPPING_LEAVE_CONFIRM)
            ) {
                Text(stringResource(Res.string.shopping_leave_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.shopping_leave_dialog_cancel))
            }
        }
    )
}
