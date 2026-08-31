package io.github.obaya884.rebuy.ui.screen.item_edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.item.ItemWithCategory
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.NameTextField
import io.github.obaya884.rebuy.ui.screen.TextFieldEditDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ItemEditScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = koinViewModel<ItemEditViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val nameError by viewModel.nameError.collectAsState()

    val listState = rememberLazyListState()

    ReBuyAppScaffold(
        topBarTitle = stringResource(Res.string.item_edit_title),
        topBarNavigationIcon = {
            IconButton(
                modifier = Modifier.testTag(TestTags.BACK_BUTTON),
                onClick = { navigator.goBack() }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showItemAddDialog()
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        if (uiState.items.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Text(
                    text = "全${uiState.items.size}件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        uiState.items,
                        key = { item -> item.item.id }
                    ) { item ->
                        ItemEditListRow(
                            item = item,
                            categories = uiState.categories,
                            onTapEditIcon = {
                                viewModel.showItemEditDialog()
                                viewModel.setEditingItem(item.item)
                            },
                            onTapDeleteIcon = {
                                viewModel.showItemDeleteDialog()
                                viewModel.setEditingItem(item.item)
                            },
                            onSelectCategory = { categoryId ->
                                viewModel.editItemCategory(item.item.id, categoryId)
                            }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(Res.string.item_edit_empty_message),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (uiState.isShowItemAddDialog) {
            ItemAddDialog(
                title = stringResource(Res.string.item_edit_add_dialog_title),
                categories = uiState.categories,
                error = nameError,
                // 閉じるのは ViewModel。弾かれたらダイアログは開いたまま（画面定義書 §2）
                onConfirm = { name, category ->
                    viewModel.addItem(name, category?.id)
                },
                onDismiss = {
                    viewModel.hideItemAddDialog()
                }
            )
        }

        if (uiState.isShowItemEditDialog && uiState.editingItem != null) {
            TextFieldEditDialog(
                title = stringResource(Res.string.item_edit_edit_dialog_title),
                editId = uiState.editingItem!!.id,
                editName = uiState.editingItem!!.name,
                error = nameError,
                onConfirm = { id, name ->
                    viewModel.editItemName(id, name)
                },
                onDismiss = {
                    viewModel.hideItemEditDialog()
                }
            )
        }

        if (uiState.isShowItemDeleteDialog && uiState.editingItem != null) {
            AlertDialog(
                icon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                },
                onDismissRequest = {
                    viewModel.hideItemDeleteDialog()
                },
                title = { Text(text = stringResource(Res.string.item_edit_delete_dialog_title)) },
                text = {
                    Text(
                        text = stringResource(
                            Res.string.item_edit_delete_dialog_message,
                            uiState.editingItem!!.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteItem()
                            viewModel.hideItemDeleteDialog()
                        }
                    ) {
                        Text(stringResource(Res.string.item_edit_delete_dialog_positive_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideItemDeleteDialog()
                        }
                    ) {
                        Text(stringResource(Res.string.item_edit_delete_dialog_negative_button))
                    }
                }
            )
        }
    }
}

@Composable
fun ItemEditListRow(
    item: ItemWithCategory,
    categories: List<Category?>,
    onTapEditIcon: () -> Unit,
    onTapDeleteIcon: () -> Unit,
    onSelectCategory: (Int?) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                item.category?.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    text = item.item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        showDropdownMenu = true
                    }
                ) {
                    Icon(
                        painterResource(Res.drawable.icon_category),
                        contentDescription = null
                    )
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category?.name ?: "未設定") },
                                onClick = {
                                    onSelectCategory(category?.id)
                                    showDropdownMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        onTapEditIcon()
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = {
                        onTapDeleteIcon()
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            }
        }
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp
        )
    }
}

@Composable
fun ItemAddDialog(
    title: String,
    categories: List<Category?>,
    error: NameError?,
    onConfirm: (String, Category?) -> Unit,
    onDismiss: () -> Unit
) {
    var inputString by remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf<Category?>(null) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .defaultMinSize(minWidth = 200.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "カテゴリー",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Box {
                    Text(
                        text = selectedCategory.value?.name ?: "未設定",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable { showDropdownMenu = true }
                    )
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category?.name ?: "未設定") },
                                onClick = {
                                    selectedCategory.value = category
                                    showDropdownMenu = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "アイテム名",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                NameTextField(
                    value = inputString,
                    onValueChange = { inputString = it },
                    error = error
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                        }
                    ) {
                        Text(
                            stringResource(Res.string.text_field_add_dialog_negative_button)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(inputString, selectedCategory.value)
                        }
                    ) {
                        Text(
                            stringResource(Res.string.text_field_add_dialog_positive_button)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ItemAddDialogPreview() {
    ItemAddDialog(
        title = stringResource(Res.string.item_edit_add_dialog_title),
        error = null,
        categories = listOf(
            Category(1, "カテゴリー1"),
            Category(2, "カテゴリー2"),
            Category(3, "カテゴリー3")
        ),
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}
