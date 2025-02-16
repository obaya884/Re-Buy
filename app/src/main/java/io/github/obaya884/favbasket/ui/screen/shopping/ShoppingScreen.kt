package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemStatus
import io.github.obaya884.favbasket.ui.BottomNavigationBar
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen
import io.github.obaya884.favbasket.ui.navigateAsRoot

@Composable
fun ShoppingScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<ShoppingViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    FavBasketAppScaffold(
        topBarTitle = stringResource(id = R.string.shopping_title),
        bottomBar = {
            BottomNavigationBar(navController)
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    uiState.inShoppingListItems,
                    key = { item -> item.id }
                ) { item ->
                    ShoppingItem(
                        item,
                    ) {
                        if (it.status == ItemStatus.CHECKED_IN_SHOPPING_LIST) {
                            viewModel.unMarkScheduledBought(item)
                        } else {
                            viewModel.markScheduledBought(item)
                        }
                    }
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                enabled = uiState.isExistCheckedInShoppingListItems,
                onClick = {
                    viewModel.showFinishShoppingAlertDialog()
                }
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(id = R.string.shopping_bottom_button)
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState.isShowFinishShoppingAlertDialog) {
            FinishShoppingAlertDialog(
                onDismiss = {
                    viewModel.hideFinishShoppingAlertDialog()
                },
                onTapConfirm = {
                    viewModel.hideFinishShoppingAlertDialog()
                    viewModel.changeBoughtConfirm {
                        navController.navigateAsRoot(Screen.Home)
                    }
                },
                onTapCancel = {
                    viewModel.hideFinishShoppingAlertDialog()
                }
            )
        }
    }
}

@Composable
fun FinishShoppingAlertDialog(
    onDismiss: () -> Unit,
    onTapConfirm: () -> Unit,
    onTapCancel: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Info, contentDescription = "")
        },
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(
                text = stringResource(
                    id = R.string.shopping_finish_alert_dialog_title
                )
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.shopping_finish_alert_dialog_message
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onTapConfirm()
                }
            ) {
                Text(
                    stringResource(
                        id = R.string.shopping_finish_alert_dialog_positive_button
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onTapCancel()
                }
            ) {
                Text(
                    stringResource(
                        id = R.string.shopping_finish_alert_dialog_negative_button
                    )
                )
            }
        }
    )
}

@Composable
fun ShoppingItem(
    item: Item,
    onCheckedChange: (Item) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                role = Role.Checkbox,
                onClick = {
                    onCheckedChange(item)
                }
            )
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Checkbox(
            modifier = Modifier.padding(end = 16.dp),
            checked = item.status == ItemStatus.CHECKED_IN_SHOPPING_LIST,
            onCheckedChange = null
        )
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )
    }
}
