package io.github.obaya884.rebuy.ui.screen.category_edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.rebuy.R
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.TextFieldAddDialog
import io.github.obaya884.rebuy.ui.screen.TextFieldEditDialog

@Composable
fun CategoryEditScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<CategoryEditViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val editingCategory = uiState.editingCategory

    ReBuyAppScaffold(
        topBarTitle = stringResource(id = R.string.category_edit_title),
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
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
                    viewModel.showCategoryAddDialog()
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
        if (uiState.categories.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Text(
                    text = "全${uiState.categories.size}件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        uiState.categories,
                        key = { category -> category.id }
                    ) { category ->
                        CategoryEditListRow(
                            name = category.name,
                            onTapEdit = {
                                viewModel.showCategoryEditDialog()
                                viewModel.setEditingCategory(category)
                            },
                            onTapDelete = {
                                viewModel.showCategoryDeleteDialog()
                                viewModel.setEditingCategory(category)
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
                    text = stringResource(id = R.string.category_edit_empty_message),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (uiState.isShowCategoryAddDialog) {
            TextFieldAddDialog(
                title = stringResource(id = R.string.category_edit_add_dialog_title),
                onConfirm = {
                    viewModel.addCategory(it)
                    viewModel.hideCategoryAddDialog()
                },
                onDismiss = {
                    viewModel.hideCategoryAddDialog()
                }
            )
        }

        if (uiState.isShowCategoryEditDialog && editingCategory != null) {
            TextFieldEditDialog(
                title = stringResource(id = R.string.category_edit_edit_dialog_title),
                editId = editingCategory.id,
                editName = editingCategory.name,
                onConfirm = { id, name ->
                    viewModel.editCategoryName(id, name)
                    viewModel.hideCategoryEditDialog()
                },
                onDismiss = {
                    viewModel.hideCategoryEditDialog()
                }
            )
        }

        if (uiState.isShowCategoryDeleteDialog && editingCategory != null) {
            AlertDialog(
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null
                    )
                },
                onDismissRequest = {
                    viewModel.hideCategoryDeleteDialog()
                },
                title = {
                    Text(text = stringResource(id = R.string.category_edit_delete_dialog_title))
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.category_edit_delete_dialog_message,
                            editingCategory.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory()
                            viewModel.hideCategoryDeleteDialog()
                        }
                    ) {
                        Text(stringResource(id = R.string.category_edit_delete_dialog_positive_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideCategoryDeleteDialog()
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                Icon(Icons.Default.Edit, contentDescription = null)
            }
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapDelete()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp
        )
    }
}
