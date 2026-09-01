package io.github.obaya884.rebuy.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.obaya884.rebuy.ui.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 遷移規則のテスト。UI にも DB にも依存しない純粋ロジックなので JVM 段で全分岐を通す。
 *
 * ここが固定しているのは「**履歴は 1 本で、プールが唯一の根**」というアプリの遷移仕様
 * （画面定義書 §1）。ナビゲーションの実装を差し替えても、この規則は変えない。
 */
class NavigatorTest {

    private fun navigator(): Navigator = Navigator(
        NavigationState(backStack = NavBackStack<NavKey>(Screen.Pool))
    )

    private fun Navigator.stack(): List<NavKey> = state.backStack.toList()

    @Test
    fun 遷移するとスタックに積まれる() {
        val navigator = navigator()

        navigator.navigate(Screen.Setting)

        assertEquals(listOf(Screen.Pool, Screen.Setting), navigator.stack())
    }

    @Test
    fun 同じ画面へ続けて遷移すると2段積まれる() {
        val navigator = navigator()

        navigator.navigate(Screen.Setting)
        navigator.navigate(Screen.Setting)

        assertEquals(listOf(Screen.Pool, Screen.Setting, Screen.Setting), navigator.stack())
    }

    @Test
    fun 深いスタックでは戻ると1段だけ降りる() {
        val navigator = navigator()
        navigator.navigate(Screen.Setting)
        navigator.navigate(Screen.License)

        navigator.goBack()

        assertEquals(listOf(Screen.Pool, Screen.Setting), navigator.stack())
    }

    /** 根で戻ってもスタックは空にしない。アプリを抜けるかどうかはシステムの戻るが決める。 */
    @Test
    fun 根で戻っても状態は変わらない() {
        val navigator = navigator()

        navigator.goBack()

        assertEquals(listOf(Screen.Pool), navigator.stack())
    }

    @Test
    fun popToRootは何段積まれていても根まで畳む() {
        val navigator = navigator()
        navigator.navigate(Screen.Setting)
        navigator.navigate(Screen.Shopping(destinationId = 1))

        navigator.popToRoot()

        assertEquals(listOf(Screen.Pool), navigator.stack())
    }

    @Test
    fun popToRootは既に根でも壊れない() {
        val navigator = navigator()

        navigator.popToRoot()

        assertEquals(listOf(Screen.Pool), navigator.stack())
    }
}
