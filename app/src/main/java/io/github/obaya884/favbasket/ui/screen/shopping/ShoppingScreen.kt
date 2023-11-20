package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold

@Composable
fun ShoppingScreen(navController: NavController) {
    val viewModel = hiltViewModel<ShoppingViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    FavBasketAppScaffold(
        topBarTitle = "Shopping",
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    // TODO: implement the confirmation UX with AlertDialog to prevent the screen is back immediately and the item status isn't changed.
                    navController.navigateUp()
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
        }
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
            Button(
                // TODO: 良い感じに浮いてる見た目にする
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = {
                    // TODO: 確認UIはさむ
                    viewModel.changeBoughtConfirm()
                    // TODO: 単純に戻るとボトムシートが表示されたままなのでpopBackRootみたいな拡張をNavControllerに作る
                    navController.navigateUp()
                }
            ) {
                Text(
                    text = "買い物を終わる"
                )
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
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = scheduledBoughtItemIds.contains(item.id),
            onCheckedChange = null
        )
    }
}
