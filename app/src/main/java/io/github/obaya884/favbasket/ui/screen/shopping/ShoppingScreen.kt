package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold

@Composable
fun ShoppingScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = hiltViewModel<ShoppingViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    FavBasketAppScaffold(
        topBarTitle = stringResource(id = R.string.shopping_title),
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    // TODO: implement the confirmation UX with AlertDialog to prevent the screen is back immediately and the item status isn't changed.
                    navController.navigateUp()
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
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
                    uiState.inBasketItems,
                    key = { item -> item.id }
                ) { item ->
                    ShoppingItem(
                        item,
                        uiState.scheduledBoughtItemIds,
                    ) { isBought ->
                        if (isBought) {
                            viewModel.unMarkScheduledBought(item.id)
                        } else {
                            viewModel.markScheduledBought(item.id)
                        }
                    }
                }
            }
            if (uiState.scheduledBoughtItemIds.isNotEmpty()) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    onClick = {
                        // TODO: 確認UIはさむ
                        viewModel.changeBoughtConfirm {
                            // TODO: 単純に戻るとボトムシートが表示されたままなのでpopBackRootみたいな拡張をNavControllerに作る
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringResource(id = R.string.shopping_bottom_button)
                    )
                }
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
    }
}

@Composable
fun ShoppingItem(
    item: Item,
    scheduledBoughtItemIds: List<Int>,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                role = Role.Checkbox,
                onClick = {
                    onCheckedChange(scheduledBoughtItemIds.contains(item.id))
                }
            )
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Checkbox(
            modifier = Modifier.padding(end = 16.dp),
            checked = scheduledBoughtItemIds.contains(item.id),
            onCheckedChange = null
        )
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )
    }
}
