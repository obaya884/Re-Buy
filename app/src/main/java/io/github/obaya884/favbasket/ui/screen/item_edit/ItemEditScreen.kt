package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.shared.EditScreenItem
import io.github.obaya884.favbasket.ui.shared.TextFieldDialog

@Composable
fun ItemEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<ItemEditViewModel>()
    val items by viewModel.items.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    FavBasketAppScaffold(
        topBarTitle = "Item Edit",
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
        },
        topBarActions = {
            // TODO: move to floating action button
            IconButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
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
                    onTapEdit = {},
                    onTapDelete = {
                        // TODO: with AlertDialog
                        viewModel.deleteItem(item)
                    }
                )
            }
        }

        if (showDialog) {
            // TODO: try to use ModalBottomSheet composable for aiming a more current UX.
            TextFieldDialog(
                title = "Add Item",
                onConfirm = {
                    viewModel.addItem(it)
                    showDialog = false
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }
    }
}

