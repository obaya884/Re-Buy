package io.github.obaya884.favbasket.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.obaya884.favbasket.R

sealed class BottomNavigationItem(val route: String, val icon: ImageVector, val titleId: Int) {
    data object Home :
        BottomNavigationItem("home", Icons.AutoMirrored.Filled.List, R.string.home_title)

    data object Shopping :
        BottomNavigationItem("shopping", Icons.Default.ShoppingCart, R.string.shopping_title)
}
