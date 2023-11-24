package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemWithCategory
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.TextFieldAddDialog
import io.github.obaya884.favbasket.ui.shared.TextFieldEditDialog

@Composable
fun ItemEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<ItemEditViewModel>()
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showItemAddDialog by remember { mutableStateOf(false) }
    var showItemEditDialog by remember { mutableStateOf(false) }
    var showItemDeleteDialog by remember { mutableStateOf(false) }
    // TODO: Data層が染み出しすぎてる。idとnameだけ保持できれば良いのでここでItemを使う必要はない。
    var editItem by remember { mutableStateOf(Item(0, "")) }

    FavBasketAppScaffold(
        topBarTitle = "Item Edit",
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.size(68.dp),
                onClick = {
                    showItemAddDialog = true
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
            items(items, key = { item -> item.item.id }) { item ->
                ItemScreenRow(
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

        if (showItemAddDialog) {
            // TODO: try to use ModalBottomSheet composable for aiming a more current UX.
            TextFieldAddDialog(
                title = "Add Item",
                onConfirm = {
                    viewModel.addItem(it)
                    showItemAddDialog = false
                },
                onDismiss = {
                    showItemAddDialog = false
                }
            )
        }

        if (showItemEditDialog) {
            TextFieldEditDialog(
                title = "Edit Item",
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
                // material3にするとiconも使える
                onDismissRequest = {
                    showItemDeleteDialog = false
                },
                title = { Text(text = "Delete Item") },
                text = { /*TODO*/ },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteItem(editItem)
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

@Composable
fun ItemScreenRow(
    item: ItemWithCategory,
    categories: List<Category>,
    onTapEditIcon: () -> Unit,
    onTapDeleteIcon: () -> Unit,
    onSelectCategory: (Int) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = item.item.name,
                textAlign = TextAlign.Start
            )
            Text(
                modifier = Modifier.weight(1f),
                text = item.category?.name ?: "",
                textAlign = TextAlign.Start
            )
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    showDropdownMenu = true
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.icon_folder),
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
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapEditIcon()
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
            }
            IconButton(
                modifier = Modifier.align(Alignment.Bottom),
                onClick = {
                    onTapDeleteIcon()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // TODO: implement the divider with the color of the theme
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .border(1.dp, Color.LightGray)
            )
        }
    }
}

