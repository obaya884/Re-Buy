package io.github.obaya884.favbasket.ui.screen.category_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.TextFieldAddDialog
import io.github.obaya884.favbasket.ui.shared.TextFieldEditDialog

@Composable
fun CategoryEditScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<CategoryEditViewModel>()
    val categories by viewModel.categories.collectAsState()

    var showCategoryAddDialog by remember { mutableStateOf(false) }
    var showCategoryEditDialog by remember { mutableStateOf(false) }
    var showItemDeleteDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf(Category(0, "")) }

    FavBasketAppScaffold(
        topBarTitle = stringResource(id = R.string.category_edit_title),
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
                    showCategoryAddDialog = true
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
                text = "合計カテゴリ数：${categories.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    categories,
                    key = { category -> category.id }
                ) { category ->
                    CategoryEditListRow(
                        name = category.name,
                        onTapEdit = {
                            showCategoryEditDialog = true
                            editCategory = category
                        },
                        onTapDelete = {
                            showItemDeleteDialog = true
                            editCategory = category
                        }
                    )
                }
            }
        }

        if (showCategoryAddDialog) {
            TextFieldAddDialog(
                title = stringResource(id = R.string.category_edit_add_dialog_title),
                onConfirm = {
                    viewModel.addCategory(it)
                    showCategoryAddDialog = false
                },
                onDismiss = {
                    showCategoryAddDialog = false
                }
            )
        }

        if (showCategoryEditDialog) {
            TextFieldEditDialog(
                title = stringResource(id = R.string.category_edit_edit_dialog_title),
                editId = editCategory.id,
                editName = editCategory.name,
                onConfirm = { id, name ->
                    viewModel.editCategoryName(id, name)
                    showCategoryEditDialog = false
                },
                onDismiss = {
                    showCategoryEditDialog = false
                }
            )
        }

        if (showItemDeleteDialog) {
            AlertDialog(
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = ""
                    )
                },
                onDismissRequest = {
                    showItemDeleteDialog = false
                },
                title = {
                    Text(text = stringResource(id = R.string.category_edit_delete_dialog_title))
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.category_edit_delete_dialog_message,
                            editCategory.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(editCategory)
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.category_edit_delete_dialog_positive_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.category_edit_delete_dialog_negative_button))
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryEditListRow(
    name: String,
    onTapEdit: () -> Unit,
    onTapDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                textAlign = TextAlign.Start
            )
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapEdit()
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
            }
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapDelete()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
            }
        }
        Divider(
            color = Color.LightGray,
            thickness = 1.dp
        )
    }
}
