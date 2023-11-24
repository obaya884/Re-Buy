package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.EditScreenItem
import io.github.obaya884.favbasket.ui.shared.TextFieldAddDialog
import io.github.obaya884.favbasket.ui.shared.TextFieldEditDialog

@Composable
fun ItemEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<ItemEditViewModel>()
    val items by viewModel.items.collectAsState()

    var showItemAddDialog by remember { mutableStateOf(false) }
    var showItemEditDialog by remember { mutableStateOf(false) }
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
            items(items, key = { item -> item.id }) { item ->
                EditScreenItem(
                    name = item.name,
                    onTapCategory = {
                        // TODO: implement the category edit feature
                    },
                    onTapEdit = {
                        showItemEditDialog = true
                        editItem = item
                    },
                    onTapDelete = {
                        // TODO: with AlertDialog
                        viewModel.deleteItem(item)
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
    }
}
