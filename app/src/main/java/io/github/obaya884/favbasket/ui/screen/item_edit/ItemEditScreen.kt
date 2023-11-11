package io.github.obaya884.favbasket.ui.screen.item_edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold

@Composable
fun ItemEditScreen(
    navController: NavController
) {
    val viewModel = hiltViewModel<ItemEditViewModel>()
    val items by viewModel.items.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    FavBasketAppScaffold(
        navController = navController,
        topBarTitle = "Item Edit",
        topBarActions = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(items) { item ->
                Text(
                    text = item.name,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 200.dp)
                    ) {
                        Text(
                            text = "Add Item",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        TextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Item Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                viewModel.addItem(newItemName)
                                newItemName = ""
                                showDialog = false
                            }) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

