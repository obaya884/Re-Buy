package io.github.obaya884.rebuy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * 構成変更とプロセス death をまたいで保持されるナビゲーション状態を作る。
 *
 * @param configuration `NavKey` の開いた多相を解ける [SavedStateConfiguration]。
 *   引数で受け取るのは、どのルートを登録するかを知っているのが呼び出し側だから。
 *   **1 引数版の [rememberNavBackStack] と `NavKeySerializer` は使わない**——`commonMain` から
 *   呼べてしまうが中身が Android の reflection なので、iOS ではプロセス復元だけが落ちる
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    configuration: SavedStateConfiguration
): NavigationState {
    val backStack = rememberNavBackStack(configuration, startRoute)

    return remember(backStack) { NavigationState(backStack) }
}

/**
 * ナビゲーション状態の保持者。
 *
 * **スタックは 1 本**。プールが唯一の根で、タブを持たない（画面定義書 §1）。
 * 根は先頭に居座り続けるので、[NavKey] を別に持たなくても
 * 「どこから抜けるか」は `backStack.first()` で決まる。
 *
 * @param backStack 表示中の画面の列。末尾が手前
 */
class NavigationState(val backStack: NavBackStack<NavKey>)

/**
 * ナビゲーション状態を [NavEntry] の列に変換する。
 *
 * ViewModel 用の decorator を入れているのは、全画面が `koinViewModel()` で ViewModel を取るため。
 * これが無いと ViewModel が entry ごとにスコープされない。
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    entryProvider = entryProvider
)
