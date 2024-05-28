package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.TextFieldEditDialog

@Composable
fun ItemEditScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<ItemEditViewModel>()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // TODO: UiModelに入れる
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showItemAddDialog by remember { mutableStateOf(false) }
    var showItemEditDialog by remember { mutableStateOf(false) }
    var showItemDeleteDialog by remember { mutableStateOf(false) }
    //TODO: ここ最初はnullにしておくべき
    var editItem by remember { mutableStateOf(Item(0, "")) }

    FavBasketAppScaffold(
        topBarTitle = stringResource(id = R.string.item_edit_title),
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Localized description"
                )
            }
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showItemAddDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Text(
                text = "合計アイテム数：${items.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items,
                    key = { item -> item.item.id }
                ) { item ->
                    ItemEditListRow(
                        item = item,
                        categories = categories,
                        onTapEditIcon = {
                            showItemEditDialog = true
                            editItem = item.item
                        },
                        onTapDeleteIcon = {
                            showItemDeleteDialog = true
                            editItem = item.item
                        },
                        onSelectCategory = { categoryId ->
                            viewModel.editItemCategory(item.item.id, categoryId)
                        }
                    )
                }
            }
        }

        if (showItemAddDialog) {
            ItemAddDialog(
                title = stringResource(id = R.string.item_edit_add_dialog_title),
                categories = categories,
                onConfirm = { name, category ->
                    viewModel.addItem(name, category.id)
                    showItemAddDialog = false
                    // TODO: 追加したら最上部にスクロールしたい
//                    coroutineScope.launch {
//                        listState.animateScrollToItem(0)
//                    }
                },
                onDismiss = {
                    showItemAddDialog = false
                }
            )
        }

        if (showItemEditDialog) {
            TextFieldEditDialog(
                title = stringResource(id = R.string.item_edit_edit_dialog_title),
                editId = editItem.id,
                editName = editItem.name,
                onConfirm = { id, name ->
                    viewModel.editItemName(id, name)
                    showItemEditDialog = false
                },
                onDismiss = {
                    showItemEditDialog = false
                }
            )
        }

        if (showItemDeleteDialog) {
            AlertDialog(
                icon = {
                    Icon(Icons.Default.Delete, contentDescription = "")
                },
                onDismissRequest = {
                    showItemDeleteDialog = false
                },
                title = { Text(text = stringResource(id = R.string.item_edit_delete_dialog_title)) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.item_edit_delete_dialog_message,
                            editItem.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteItem(editItem)
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.item_edit_delete_dialog_positive_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.item_edit_delete_dialog_negative_button))
                    }
                }
            )
        }
    }
}

@Composable
fun ItemEditListRow(
    item: ItemWithCategory,
    categories: List<Category>,
    onTapEditIcon: () -> Unit,
    onTapDeleteIcon: () -> Unit,
    onSelectCategory: (Int) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
                        painterResource(id = R.drawable.icon_category),
                        contentDescription = "Edit Category"
                    )
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category.name) },
                                onClick = {
                                    onSelectCategory(category.id)
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
                        contentDescription = "Edit Item"
                    )
                }
                IconButton(
                    onClick = {
                        onTapDeleteIcon()
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Item"
                    )
                }
            }
        }
        Divider(
            modifier = Modifier.padding(top = 4.dp),
            color = Color.LightGray,
            thickness = 1.dp
        )
    }
}

@Composable
fun ItemAddDialog(
    title: String,
    categories: List<Category>,
    onConfirm: (String, Category) -> Unit,
    onDismiss: () -> Unit
) {
    var inputString by remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf(Category(0, "未設定")) }
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
                        text = selectedCategory.value.name,
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
                                text = { Text(text = category.name) },
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
                TextField(
                    value = inputString,
                    onValueChange = { inputString = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                            stringResource(id = R.string.text_field_add_dialog_negative_button)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(inputString, selectedCategory.value)
                        }
                    ) {
                        Text(
                            stringResource(id = R.string.text_field_add_dialog_positive_button)
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
        title = stringResource(id = R.string.item_edit_add_dialog_title),
        categories = listOf(
            Category(1, "カテゴリー1"),
            Category(2, "カテゴリー2"),
            Category(3, "カテゴリー3")
        ),
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}
