package io.github.obaya884.favbasket.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.obaya884.favbasket.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.favbasket.ui.screen.home.HomeScreen
import io.github.obaya884.favbasket.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.favbasket.ui.screen.setting.SettingScreen
import io.github.obaya884.favbasket.ui.screen.shopping.ShoppingScreen
import io.github.obaya884.favbasket.ui.theme.FavBasketTheme

@Composable
fun FavBasketApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    FavBasketTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController, snackbarHostState)
            }
            composable(Screen.Setting.route) {
                SettingScreen(navController, snackbarHostState)
            }
            composable(Screen.CategoryEdit.route) {
                CategoryEditScreen(navController, snackbarHostState)
            }
            composable(Screen.ItemEdit.route) {
                ItemEditScreen(navController, snackbarHostState)
            }
            composable(Screen.Shopping.route) {
                ShoppingScreen(navController, snackbarHostState)
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Setting : Screen("setting")
    data object CategoryEdit : Screen("category_edit")
    data object ItemEdit : Screen("item_edit")
    data object Shopping : Screen("shopping")
}
