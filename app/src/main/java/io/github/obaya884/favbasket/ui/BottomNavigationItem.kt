package io.github.obaya884.favbasket.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavigationItem(val route: String, val icon: ImageVector, val title: String) {
    data object Home : BottomNavigationItem("home", Icons.AutoMirrored.Filled.List, "Home")
    data object Shopping : BottomNavigationItem("shopping", Icons.Default.ShoppingCart, "Shopping")
}
