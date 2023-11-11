package io.github.obaya884.favbasket.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.obaya884.favbasket.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.favbasket.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.favbasket.ui.screen.main.MainScreen
import io.github.obaya884.favbasket.ui.theme.FavBasketTheme

@Composable
fun FavBasketApp() {
    val navController = rememberNavController()
    FavBasketTheme {
        NavHost(navController, startDestination = "main") {
            composable("main") {
                MainScreen(navController)
            }
            composable("category_edit") {
                CategoryEditScreen(navController)
            }
            composable("item_edit") {
                ItemEditScreen(navController)
            }
        }
    }
}
