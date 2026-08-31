package io.github.obaya884.rebuy.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * 暫定: ボトムナビの項目。**画面定義書はタブを持たない設計**（プールが唯一の根）で、
 * プール側にバーは無い。買い物画面から戻る道がこれしか無いので、買い物モードを
 * 03 経由に組み替える F-009 まで買い物側にだけ残す。
 *
 * トップレベルルート（それぞれが独立した backstack を持つ）の単一の正。
 * タブを増やすときはここに 1 行足せば、ナビゲーションバーと [topLevelRoutes] の両方に反映される。
 */
enum class BottomNavigationItem(val route: NavKey, val icon: ImageVector, val title: StringResource) {
    Pool(Screen.Pool, Icons.AutoMirrored.Filled.List, Res.string.pool_nav_label),
    Shopping(Screen.Shopping, Icons.Default.ShoppingCart, Res.string.shopping_title);

    companion object {
        val topLevelRoutes: Set<NavKey> = entries.map { it.route }.toSet()
    }
}
