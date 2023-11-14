package io.github.obaya884.favbasket.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.obaya884.favbasket.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.favbasket.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.favbasket.ui.screen.main.MainScreen
import io.github.obaya884.favbasket.ui.screen.setting.SettingScreen
import io.github.obaya884.favbasket.ui.screen.shopping.ShoppingScreen
import io.github.obaya884.favbasket.ui.theme.FavBasketTheme

@Composable
fun FavBasketApp() {
    val navController = rememberNavController()
    FavBasketTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            composable(Screen.Main.route) {
                MainScreen(navController)
            }
            composable(Screen.Setting.route) {
                SettingScreen(navController)
            }
            composable(Screen.CategoryEdit.route) {
                CategoryEditScreen(navController)
            }
            composable(Screen.ItemEdit.route) {
                ItemEditScreen(navController)
            }
            composable(Screen.Shopping.route) {
                ShoppingScreen(navController)
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Setting : Screen("setting")
    data object CategoryEdit : Screen("category_edit")
    data object ItemEdit : Screen("item_edit")
    data object Shopping : Screen("shopping")
}
