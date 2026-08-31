package io.github.obaya884.rebuy.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.navigation.rememberNavigationState
import io.github.obaya884.rebuy.ui.navigation.toEntries
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditScreen
import io.github.obaya884.rebuy.ui.screen.home.HomeScreen
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditScreen
import io.github.obaya884.rebuy.ui.screen.license.LicenseScreen
import io.github.obaya884.rebuy.ui.screen.setting.SettingScreen
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingScreen
import io.github.obaya884.rebuy.ui.screen.theme.ThemeScreen
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject

@Composable
fun ReBuyApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    // テーマの選択はアプリ全体に効くので、画面ではなくここで見る（画面 08 はタップで即時反映）
    val themeRepository = koinInject<ThemeRepository>()
    val palette by themeRepository.palette.collectAsState()

    val navigationState = rememberNavigationState(
        startRoute = Screen.Home,
        topLevelRoutes = BottomNavigationItem.topLevelRoutes,
        configuration = screenSavedStateConfiguration
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = remember(navigator, snackbarHostState) {
        entryProvider<NavKey> {
            entry<Screen.Home> { HomeScreen(navigator, snackbarHostState) }
            entry<Screen.Shopping> { ShoppingScreen(navigator, snackbarHostState) }
            entry<Screen.Setting> { SettingScreen(navigator, snackbarHostState) }
            entry<Screen.CategoryEdit> { CategoryEditScreen(navigator, snackbarHostState) }
            entry<Screen.ItemEdit> { ItemEditScreen(navigator, snackbarHostState) }
            entry<Screen.License> { LicenseScreen(navigator, snackbarHostState) }
            entry<Screen.Theme> { ThemeScreen(navigator, snackbarHostState) }
        }
    }

    ReBuyTheme(palette = palette) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}

/**
 * 画面のルート。宣言順は [entryProvider] の登録順（トップレベルが先）に揃える。
 *
 * **画面を足すときは 3 か所そろえる**——ここの `data object`、[entryProvider] の `entry<...>`、
 * そして [screenSavedStateConfiguration] の `subclass(...)`。
 *
 * 3 つ目を忘れると、**その画面にいる状態でプロセス保存が走った瞬間に `SerializationException`**
 * になる。Android も iOS も同じ（実測）——Android を救っていた reflection 経路は
 * `NavigationState` が `SavedStateConfiguration` へ移ったときに使うのをやめた。
 * `ScreenSerializationTest`（JVM 段）と `NavigationStateRestorationTest`（instrumented）が止める。
 * 2 つ目は instrumented の `NavigationTest` が 6 画面を実際に踏むことで押さえている。
 */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen
    @Serializable data object Shopping : Screen
    @Serializable data object Setting : Screen
    @Serializable data object CategoryEdit : Screen
    @Serializable data object ItemEdit : Screen
    @Serializable data object License : Screen
    @Serializable data object Theme : Screen
}

/**
 * backstack を保存・復元するときの [Screen] の読み書き方。
 *
 * backstack が持つのは `NavKey`（sealed でない interface）なので、どの実装型かを
 * **開いた多相**として書き分ける必要がある。Android には reflection でこれを賄う経路があるが
 * iOS には無いので、サブクラスを 1 つずつ登録する。
 */
internal val screenSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Screen.Home::class)
            subclass(Screen.Shopping::class)
            subclass(Screen.Setting::class)
            subclass(Screen.CategoryEdit::class)
            subclass(Screen.ItemEdit::class)
            subclass(Screen.License::class)
            subclass(Screen.Theme::class)
        }
    }
}
