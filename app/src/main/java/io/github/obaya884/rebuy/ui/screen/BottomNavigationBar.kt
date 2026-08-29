package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
    navController: NavController,
    shoppingTabBadgeCount: Int
) {
    val items = listOf(
        BottomNavigationItem.Home,
        BottomNavigationItem.Shopping
    )
    NavigationBar {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (item == BottomNavigationItem.Shopping && shoppingTabBadgeCount > 0) {
                                Badge {
                                    Text(
                                        text = shoppingTabBadgeCount.toString()
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = item.icon, contentDescription = null)
                    }
                },
                label = { Text(stringResource(item.titleId)) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
