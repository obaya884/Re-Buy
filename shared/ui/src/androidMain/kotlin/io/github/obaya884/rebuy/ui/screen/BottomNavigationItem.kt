package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.ui.R
import io.github.obaya884.rebuy.ui.Screen

/**
 * ボトムナビの項目。トップレベルルート（それぞれが独立した backstack を持つ）の単一の正。
 * タブを増やすときはここに 1 行足せば、ナビゲーションバーと [topLevelRoutes] の両方に反映される。
 */
enum class BottomNavigationItem(val route: NavKey, val icon: ImageVector, val titleId: Int) {
    Home(Screen.Home, Icons.AutoMirrored.Filled.List, R.string.home_title),
    Shopping(Screen.Shopping, Icons.Default.ShoppingCart, R.string.shopping_title);

    companion object {
        val topLevelRoutes: Set<NavKey> = entries.map { it.route }.toSet()
    }
}
