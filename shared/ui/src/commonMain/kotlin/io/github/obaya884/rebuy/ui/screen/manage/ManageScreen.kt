package io.github.obaya884.rebuy.ui.screen.manage

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.DashedAddRow
import io.github.obaya884.rebuy.ui.screen.NameTarget
import io.github.obaya884.rebuy.ui.screen.NewNameDialog
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.ReBuyRowCard
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * カテゴリの管理／行き先の管理（画面 09）。設定（07）から開く。
 *
 * **2 つは同型の画面**で、向きはルートが持つ [Screen.Manage.target] だけで決まる。
 *
 * 行はハンドル ≡ を掴んで並び替える。**長押しは編集シート（09b）に取られている**ので、
 * ドラッグはハンドルからしか始まらない。末尾の破線行から 02b を開いて末尾に足す。
 */
@Composable
fun ManageScreen(
    route: Screen.Manage,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = koinViewModel<ManageViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsState()

    // ドラッグ中の画素の値は画面の中だけで持つ。どこへ落ちるかの判断は ViewModel
    var dragPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    // 1 つ隣へ落ちるのに指が進む距離は**行の高さ＋行間**。高さだけで割ると動かすほど行き過ぎる
    val rowPitchPx = rowHeightPx + with(LocalDensity.current) { ROW_SPACING.toPx() }

    ReBuyAppScaffold(
        topBarTitle = stringResource(
            when (route.target) {
                NameTarget.CATEGORY -> Res.string.manage_category_title
                NameTarget.DESTINATION -> Res.string.manage_destination_title
            }
        ),
        topBarNavigationIcon = {
            IconButton(
                modifier = Modifier.testTag(TestTags.BACK_BUTTON),
                onClick = { navigator.goBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            uiState.rows.forEachIndexed { index, record ->
                ManageRow(
                    record = record,
                    isDragging = record.id == uiState.draggingId,
                    // 落とし先を当てた並びで描かれるぶんを引く。引かないと落とし先が
                    // 変わるたびに行が 1 行ぶん飛び出す
                    dragPx = dragPx - uiState.draggingRowShift * rowPitchPx,
                    onMeasured = { rowHeightPx = it },
                    onLongPress = { viewModel.startEditing(record) },
                    onDragStart = {
                        dragPx = 0f
                        viewModel.startDrag(index)
                    },
                    onDrag = { delta ->
                        dragPx += delta
                        viewModel.dragBy(dragPx, rowPitchPx)
                    },
                    onDragEnd = {
                        dragPx = 0f
                        viewModel.endDrag()
                    }
                )
            }

            DashedAddRow(
                label = stringResource(
                    when (route.target) {
                        NameTarget.CATEGORY -> Res.string.manage_add_category
                        NameTarget.DESTINATION -> Res.string.manage_add_destination
                    }
                ),
                testTag = TestTags.MANAGE_ADD_ROW,
                onTap = { viewModel.showAddDialog() }
            )
        }
    }

    uiState.addDialog?.let { dialog ->
        NewNameDialog(
            state = dialog,
            onNameChange = viewModel::changeNewName,
            onCreate = viewModel::createNewName,
            onDismiss = viewModel::dismissAddDialog
        )
    }

    uiState.editing?.let { editing ->
        ManageEditSheet(
            editing = editing,
            nameError = uiState.nameError,
            target = route.target,
            affectedItemCount = uiState.affectedItemCount,
            onNameChange = viewModel::changeName,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onDismiss = viewModel::dismissEditing
        )
    }
}

/**
 * 1 行。**ハンドルはドラッグ、名前は長押し**と受け口を分ける。
 *
 * 面そのものには何も付けない。**カード全体で長押しを取ると、ハンドルを掴んだまま
 * 止めた瞬間に編集シートが開く**——`draggable` はタッチスロップを超えるまで何も
 * 消費しないので、その間に親の長押しが先に成立してしまう（実測）。
 *
 * @param onMeasured 行の高さ。落とし先の計算に要る（画面 09 は行の高さが一定）
 */
@Composable
private fun ManageRow(
    record: ManagedRecord,
    isDragging: Boolean,
    dragPx: Float,
    onMeasured: (Float) -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    ReBuyRowCard(
        highlighted = isDragging,
        onTap = null,
        testTag = TestTags.manageRow(record.id),
        modifier = Modifier
            .onSizeChanged { onMeasured(it.height.toFloat()) }
            // 掴んでいる行だけ指に付いてくる。他の行は並びが入れ替わって見える
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = if (isDragging) dragPx else 0f }
    ) {
        Icon(
            Icons.Default.Menu,
            contentDescription = null,
            tint = ReBuyTheme.colors.muted,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .testTag(TestTags.manageHandle(record.id))
                // 縦のドラッグだけを取る。親のスクロールには渡らない
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { onDrag(it) },
                    onDragStarted = { onDragStart() },
                    onDragStopped = { onDragEnd() }
                )
        )
        Text(
            text = record.name,
            style = MaterialTheme.typography.bodyLarge,
            color = ReBuyTheme.colors.ink,
            // 行の高さは一定。入りきらない名前は末尾を省略する（画面 09）
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                // 長押しだけを取る。**タップには意味が無い**ので clickable にはしない
                // （押せそうに見えて何も起きない行になる）
                .pointerInput(record.id) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
                // 並びを数えるための共通タグ。名前を読むので Text に付ける
                .testTag(TestTags.MANAGE_ROW_NAME)
        )
    }
}

/** 行の間隔。落とし先の計算にも使うので 1 か所から引く。 */
private val ROW_SPACING = 8.dp
