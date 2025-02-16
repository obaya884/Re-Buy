package io.github.obaya884.favbasket.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavBasketAppScaffold(
    topBarTitle: String,
    topBarNavigationIcon: @Composable () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit = { },
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = topBarNavigationIcon,
                actions = topBarActions
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition
    ) { innerPadding ->
        content(innerPadding)
    }
}
