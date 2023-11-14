package io.github.obaya884.favbasket.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable

@Composable
fun FavBasketAppScaffold(
    topBarTitle: String,
    topBarNavigationIcon: @Composable (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = topBarNavigationIcon,
                actions = topBarActions
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
