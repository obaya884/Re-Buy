package io.github.obaya884.favbasket.ui.screen.license

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.obaya884.favbasket.ui.screen.FavBasketAppScaffold

@Composable
fun LicenseScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    FavBasketAppScaffold(
        topBarTitle = "ライセンス",
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        LibrariesContainer(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
