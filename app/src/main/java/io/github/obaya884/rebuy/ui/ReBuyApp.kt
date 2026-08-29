package io.github.obaya884.rebuy.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.navigation.rememberNavigationState
import io.github.obaya884.rebuy.ui.navigation.toEntries
import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.rebuy.ui.screen.home.HomeScreen
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.rebuy.ui.screen.license.LicenseScreen
import io.github.obaya884.rebuy.ui.screen.setting.SettingScreen
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingScreen
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import kotlinx.serialization.Serializable

@Composable
fun ReBuyApp() {
    val snackbarHostState = remember { SnackbarHostState() }

    val navigationState = rememberNavigationState(
        startRoute = Screen.Home,
        topLevelRoutes = setOf(Screen.Home, Screen.Shopping)
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = entryProvider<NavKey> {
        entry<Screen.Home> { HomeScreen(navigator, snackbarHostState) }
        entry<Screen.Shopping> { ShoppingScreen(navigator, snackbarHostState) }
        entry<Screen.Setting> { SettingScreen(navigator, snackbarHostState) }
        entry<Screen.CategoryEdit> { CategoryEditScreen(navigator, snackbarHostState) }
        entry<Screen.ItemEdit> { ItemEditScreen(navigator, snackbarHostState) }
        entry<Screen.License> { LicenseScreen(navigator, snackbarHostState) }
    }

    ReBuyTheme {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}

sealed class Screen : NavKey {
    @Serializable
    data object Home : Screen()

    @Serializable
    data object Setting : Screen()

    @Serializable
    data object CategoryEdit : Screen()

    @Serializable
    data object ItemEdit : Screen()

    @Serializable
    data object Shopping : Screen()

    @Serializable
    data object License : Screen()
}
