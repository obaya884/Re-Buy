package io.github.obaya884.favbasket.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun FavBasketAppScaffold(
    navController: NavController,
    topBarTitle: String,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    // TODO: this place is for displaying the back button.
                    IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                },
                // TODO: this place is for displaying the setting button.
                actions = topBarActions
            )
        },
        drawerContent = {
            FavBasketAppDrawer(
                navController = navController,
                scaffoldState = scaffoldState,
                scope = scope
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}
