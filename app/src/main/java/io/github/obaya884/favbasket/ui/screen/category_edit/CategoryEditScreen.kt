package io.github.obaya884.favbasket.ui.screen.category_edit

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
fun CategoryEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<CategoryEditViewModel>()
    val categories by viewModel.categories.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    FavBasketAppScaffold(
        topBarTitle = "Category Edit",
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
        },
        topBarActions = {
            IconButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
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
                    onTapDelete = {
                        // TODO: with AlertDialog
                        viewModel.deleteCategory(category)
                    }
                )
            }
        }

        if (showDialog) {
            TextFieldDialog(
                title = "Add Category",
                onConfirm = {
                    viewModel.addCategory(it)
                    showDialog = false
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }
    }
}
