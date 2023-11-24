package io.github.obaya884.favbasket.ui.screen.category_edit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.EditScreenItem
import io.github.obaya884.favbasket.ui.shared.TextFieldAddDialog
import io.github.obaya884.favbasket.ui.shared.TextFieldEditDialog

@Composable
fun CategoryEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<CategoryEditViewModel>()
    val categories by viewModel.categories.collectAsState()

    var showCategoryAddDialog by remember { mutableStateOf(false) }
    var showCategoryEditDialog by remember { mutableStateOf(false) }
    var showItemDeleteDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf(Category(0, "")) }

    FavBasketAppScaffold(
        topBarTitle = "Category Edit",
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Localized description"
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.size(68.dp),
                onClick = {
                    showCategoryAddDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Item",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(categories, key = { category -> category.id }) { category ->
                EditScreenItem(
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

        if (showCategoryAddDialog) {
            TextFieldAddDialog(
                title = "Add Category",
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
                title = "Edit Category",
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
                // material3にするとiconも使える
                onDismissRequest = {
                    showItemDeleteDialog = false
                },
                title = { Text(text = "Delete Category") },
                text = { /*TODO*/ },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(editCategory)
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showItemDeleteDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
