package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator

@Composable
fun BottomNavigationBar(
    navigator: Navigator,
    shoppingTabBadgeCount: Int
) {
    NavigationBar {
        BottomNavigationItem.entries.forEach { item ->
            NavigationBarItem(
                modifier = Modifier.testTag(TestTags.bottomNavItem(item)),
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
                selected = item.route == navigator.currentTopLevelRoute,
                onClick = {
                    navigator.navigate(item.route)
                }
            )
        }
    }
}
