package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.obaya884.rebuy.ui.navigation.Navigator

@Composable
fun BottomNavigationBar(
    navigator: Navigator,
    shoppingTabBadgeCount: Int
) {
    val items = listOf(
        BottomNavigationItem.Home,
        BottomNavigationItem.Shopping
    )
    NavigationBar {
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
                selected = item.key == navigator.state.topLevelRoute,
                onClick = {
                    navigator.navigate(item.key)
                }
            )
        }
    }
}
