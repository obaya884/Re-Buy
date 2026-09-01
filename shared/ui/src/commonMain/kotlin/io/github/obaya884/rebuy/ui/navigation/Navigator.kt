package io.github.obaya884.rebuy.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * ナビゲーションのイベント（前進・後退）を受けて [NavigationState] を更新する。
 */
class Navigator(val state: NavigationState) {

    fun navigate(route: NavKey) {
        state.backStack.add(route)
    }

    /** **根では何もしない。** アプリを抜けるかどうかはシステムの戻るに任せる。 */
    fun goBack() {
        if (state.backStack.size > 1) {
            state.backStack.removeAt(state.backStack.lastIndex)
        }
    }

    /**
     * 根（プール）まで畳む。買い物を終えて 01 へ戻る動きに使う。
     * 04 の上に何段積まれていても、**買い物の終わりは必ず 01** にする。
     */
    fun popToRoot() {
        while (state.backStack.size > 1) {
            state.backStack.removeAt(state.backStack.lastIndex)
        }
    }
}
