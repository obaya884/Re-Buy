package io.github.obaya884.rebuy.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.ui.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 遷移規則のテスト。UI にも DB にも依存しない純粋ロジックなので JVM 段で全分岐を通す。
 *
 * ここが固定しているのは「プールと買い物がそれぞれ独立した履歴を持ち、タブを離れても保持される」
 * というアプリの遷移仕様そのもの。ナビゲーションの実装を差し替えても、この規則は変えない。
 */
class NavigatorTest {

    private fun navigationState(
        currentTopLevelRoute: NavKey = Screen.Pool
    ): NavigationState = NavigationState(
        startRoute = Screen.Pool,
        topLevelRoute = mutableStateOf(currentTopLevelRoute),
        backStacks = mapOf(
            Screen.Pool to NavBackStack<NavKey>(Screen.Pool),
            Screen.Shopping to NavBackStack<NavKey>(Screen.Shopping)
        )
    )

    private fun NavigationState.poolStack() = backStacks.getValue(Screen.Pool)
    private fun NavigationState.shoppingStack() = backStacks.getValue(Screen.Shopping)

    @Test
    fun 非トップレベルへの遷移は現在のタブのスタックに積まれる() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.navigate(Screen.Setting)

        assertEquals(listOf(Screen.Pool, Screen.Setting), state.poolStack().toList())
        assertEquals(Screen.Pool, state.topLevelRoute)
    }

    @Test
    fun トップレベルへの遷移はタブを切り替えるだけでスタックに積まれない() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.navigate(Screen.Shopping)

        assertEquals(Screen.Shopping, state.topLevelRoute)
        assertEquals(listOf(Screen.Pool), state.poolStack().toList())
        assertEquals(listOf(Screen.Shopping), state.shoppingStack().toList())
    }

    @Test
    fun 同じ画面へ続けて遷移すると2段積まれる() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.navigate(Screen.Setting)
        navigator.navigate(Screen.Setting)

        assertEquals(
            listOf(Screen.Pool, Screen.Setting, Screen.Setting),
            state.poolStack().toList()
        )
    }

    @Test
    fun 深いスタックでは戻ると1段だけ降りる() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(Screen.Setting)
        navigator.navigate(Screen.License)

        navigator.goBack()

        assertEquals(listOf(Screen.Pool, Screen.Setting), state.poolStack().toList())
    }

    @Test
    fun 買い物タブの根で戻るとプールへ移りつつ買い物の履歴は残る() {
        val state = navigationState(currentTopLevelRoute = Screen.Shopping)
        val navigator = Navigator(state)

        navigator.goBack()

        assertEquals(Screen.Pool, state.topLevelRoute)
        assertEquals(listOf(Screen.Shopping), state.shoppingStack().toList())
    }

    @Test
    fun プールの根で戻っても状態は変わらない() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.goBack()

        assertEquals(Screen.Pool, state.topLevelRoute)
        assertEquals(listOf(Screen.Pool), state.poolStack().toList())
    }

    @Test
    fun navigateAsRootは全てのスタックを根まで畳んで指定のタブへ移る() {
        val state = navigationState(currentTopLevelRoute = Screen.Shopping)
        val navigator = Navigator(state)
        state.poolStack().add(Screen.Setting)
        state.poolStack().add(Screen.License)
        state.shoppingStack().add(Screen.CategoryEdit)

        navigator.navigateAsRoot(Screen.Pool)

        assertEquals(Screen.Pool, state.topLevelRoute)
        assertEquals(listOf(Screen.Pool), state.poolStack().toList())
        assertEquals(listOf(Screen.Shopping), state.shoppingStack().toList())
    }

    @Test
    fun navigateAsRootは全てのスタックが既に根でも壊れない() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.navigateAsRoot(Screen.Pool)

        assertEquals(Screen.Pool, state.topLevelRoute)
        assertEquals(listOf(Screen.Pool), state.poolStack().toList())
        assertEquals(listOf(Screen.Shopping), state.shoppingStack().toList())
    }

    @Test
    fun navigateAsRootにトップレベルでないルートを渡すと弾かれる() {
        val navigator = Navigator(navigationState())

        assertFailsWith<IllegalArgumentException> {
            navigator.navigateAsRoot(Screen.Setting)
        }
    }

    @Test
    fun 表示に使うスタックはプールにいるときプールだけ() {
        assertEquals(listOf(Screen.Pool), navigationState().stacksInUse)
    }

    @Test
    fun 表示に使うスタックは買い物にいるときプールの上に買い物を重ねる() {
        val state = navigationState(currentTopLevelRoute = Screen.Shopping)

        assertEquals(listOf(Screen.Pool, Screen.Shopping), state.stacksInUse)
    }
}
