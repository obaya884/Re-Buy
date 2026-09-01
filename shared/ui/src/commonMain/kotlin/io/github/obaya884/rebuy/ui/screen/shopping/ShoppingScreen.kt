package io.github.obaya884.rebuy.ui.screen.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.add_noticed.AddNoticedSheet
import io.github.obaya884.rebuy.ui.screen.ReBuyRowCard
import io.github.obaya884.rebuy.ui.screen.SectionLabel
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
 * 一覧の作り方と「終了」で何を戻すかは `ShoppingScreenUiState`。
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
    var isAddNoticedSheetOpen by remember { mutableStateOf(false) }
    // 他の行き先へ足したことの通知。**画面に変化が見えない操作**なのでスナックバー（§2）。
    // **連番を添える**——同じ行き先へ続けて足したとき、文言が同じでも 2 通目を取りこぼさない
    var notice by remember { mutableStateOf<ElsewhereNotice?>(null) }

    val noticeMessage = notice
        ?.let { stringResource(Res.string.add_noticed_added_elsewhere, it.destinationName) }
    LaunchedEffect(notice) {
        noticeMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // **05 が開いている間は受けない。** バックはシートを閉じるだけ（画面 04）
    SystemBackHandler(enabled = !isAddNoticedSheetOpen) { isLeaveDialogOpen = true }

    ReBuyAppScaffold(
        topBarTitle = shoppingTitle(uiState),
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
                shoppingRows(uiState.destinationItems, viewModel::toggleCheck)
                if (uiState.anywhereItems.isNotEmpty()) {
                    item {
                        SectionLabel(
                            text = stringResource(Res.string.shopping_anywhere_section),
                            testTag = TestTags.SHOPPING_ANYWHERE_SECTION
                        )
                    }
                    shoppingRows(uiState.anywhereItems, viewModel::toggleCheck)
                }
                item { AddNoticedRow(onTap = { isAddNoticedSheetOpen = true }) }
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

    if (isAddNoticedSheetOpen) {
        AddNoticedSheet(
            route = route,
            onAddedElsewhere = { name ->
                notice = ElsewhereNotice((notice?.serial ?: 0) + 1, name)
            },
            onDismiss = { isAddNoticedSheetOpen = false }
        )
    }

    if (isLeaveDialogOpen) {
        LeaveDialog(
            onConfirm = {
                isLeaveDialogOpen = false
                // 04 の上に積まれるのは 05（シート＝ルートではない）だけなので 1 段で足りる
                navigator.goBack()
            },
            onCancel = { isLeaveDialogOpen = false }
        )
    }
}

/**
 * アプリバーのタイトル。全件モードは行き先を持たないので「買い物中」。
 *
 * **行き先を読み込むまでは出さない**——空にしておくほうが、全件モードのタイトルが
 * 一瞬出て入れ替わるより誤解が少ない。
 */
@Composable
private fun shoppingTitle(uiState: ShoppingScreenUiState): String = when {
    uiState.isAllMode -> stringResource(Res.string.shopping_title_all)
    else -> uiState.destinationName?.let { stringResource(Res.string.shopping_title, it) }.orEmpty()
}

private fun LazyListScope.shoppingRows(items: List<Item>, onTap: (Item) -> Unit) {
    items(items, key = { it.id }) { item ->
        ShoppingRow(item = item, onTap = { onTap(item) })
    }
}

/** 一覧の 1 行。チェック済みは**取り消し線と ✓ の 2 通り**で分かるようにする。 */
@Composable
private fun ShoppingRow(item: Item, onTap: () -> Unit) {
    val isChecked = item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST
    ReBuyRowCard(
        highlighted = isChecked,
        onTap = onTap,
        testTag = TestTags.shoppingRow(item.id)
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

/**
 * 一覧の末尾の「＋ 気づいたものを足す」（→ 05）。
 *
 * **破線で囲う**（画面 04）——品目の行と同じ面に見えると、押すとチェックが付くものに見える。
 */
@Composable
private fun AddNoticedRow(onTap: () -> Unit) {
    val outline = ReBuyTheme.colors.muted
    Text(
        text = stringResource(Res.string.shopping_add_noticed),
        style = MaterialTheme.typography.bodyLarge,
        color = outline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            // リップルと破線の角丸を 1 か所から引く。別々に書くと枠と波紋の形がずれる
            .clip(RoundedCornerShape(ADD_NOTICED_CORNER))
            .clickable(role = Role.Button, onClick = onTap)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx())
                        )
                    ),
                    cornerRadius = CornerRadius(ADD_NOTICED_CORNER.toPx())
                )
            }
            .padding(vertical = 16.dp)
            .testTag(TestTags.SHOPPING_ADD_NOTICED_ROW)
    )
}

/** 破線の刻みと角丸。密度で見え方が変わらないよう dp で持つ。 */
private val DASH_ON = 6.dp
private val DASH_OFF = 4.dp
private val ADD_NOTICED_CORNER = 12.dp

/** 他の行き先へ足したことの通知。**同じ行き先が続いても別物として扱う**ための連番を持つ。 */
private data class ElsewhereNotice(val serial: Int, val destinationName: String)

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
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(TestTags.SHOPPING_LEAVE_CANCEL)
            ) {
                Text(stringResource(Res.string.shopping_leave_dialog_cancel))
            }
        }
    )
}
