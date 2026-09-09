package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ReBuyAppScaffold(
    appBar: ReBuyAppBarState,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = { ReBuyTopAppBar(appBar) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        content(innerPadding)
    }
}
