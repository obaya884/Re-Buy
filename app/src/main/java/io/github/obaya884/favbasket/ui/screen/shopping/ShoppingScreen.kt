package io.github.obaya884.favbasket.ui.screen.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold

@Composable
fun ShoppingScreen(navController: NavController) {
    FavBasketAppScaffold(
        topBarTitle = "Shopping",
        topBarNavigationIcon = {
            IconButton(
                onClick = {
                    // TODO: implement the confirmation UX to prevent the screen is back immediately and the item status isn't changed.
                    navController.navigateUp()
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Localized description")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Shopping")
        }

    }
}
