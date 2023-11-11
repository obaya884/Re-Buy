package io.github.obaya884.favbasket.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun FavBasketAppDrawer(
    navController: NavController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope
) {
    val currentBackStackEntry = navController.currentBackStackEntry
    val currentDestination = currentBackStackEntry?.destination?.route

    fun navigateTo(route: String) {
        if (currentDestination != route) {
            navController.navigate(route)
        }
        scope.launch {
            scaffoldState.drawerState.close()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DrawerItem.values().forEach { drawerItem ->
            Row(
                modifier = Modifier
                    .clickable { navigateTo(drawerItem.route) }
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Icon(imageVector = drawerItem.icon, contentDescription = null)
                Spacer(modifier = Modifier.padding(8.dp))
                Text(stringResource(id = drawerItem.titleStringResId), fontSize = 18.sp)
            }
        }
    }
}

enum class DrawerItem(
    val icon: ImageVector,
    val titleStringResId: Int,
    val route: String
) {
    Home(
        Icons.Default.Home,
        R.string.drawer_home,
        "main"
    ),
    ItemEdit(
        Icons.Default.Star,
        R.string.drawer_item_edit,
        "item_edit"
    ),
    CategoryEdit(
        Icons.Default.Favorite,
        R.string.drawer_category_edit,
        "category_edit"
    )
}
