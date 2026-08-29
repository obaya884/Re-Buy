package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.R
import io.github.obaya884.rebuy.ui.Screen

sealed class BottomNavigationItem(val key: NavKey, val icon: ImageVector, val titleId: Int) {
    data object Home :
        BottomNavigationItem(Screen.Home, Icons.AutoMirrored.Filled.List, R.string.home_title)

    data object Shopping :
        BottomNavigationItem(Screen.Shopping, Icons.Default.ShoppingCart, R.string.shopping_title)
}
