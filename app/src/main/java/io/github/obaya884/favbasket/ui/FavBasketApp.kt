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
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route
        ) {
            composable(Screen.Main.route) {
                MainScreen(navController)
            }
            composable(Screen.CategoryEdit.route) {
                CategoryEditScreen(navController)
            }
            composable(Screen.ItemEdit.route) {
                ItemEditScreen(navController)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object CategoryEdit : Screen("category_edit")
    object ItemEdit : Screen("item_edit")
}
