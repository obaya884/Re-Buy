package io.github.obaya884.rebuy.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * ナビゲーションのイベント（前進・後退）を受けて [NavigationState] を更新する。
 */
class Navigator(val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // トップレベルルートなので切り替えるだけ
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("${state.topLevelRoute} のスタックが無い")
        val currentRoute = currentStack.last()

        // 現在のトップレベルルートの根にいるなら、開始ルートのスタックへ戻る
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * すべてのスタックを根まで畳んでから、指定のトップレベルルートへ移る。
     * 買い物を終えてホームへ戻る動きに使う。
     */
    fun navigateAsRoot(route: NavKey) {
        state.backStacks.values.forEach { stack ->
            while (stack.size > 1) {
                stack.removeLastOrNull()
            }
        }
        state.topLevelRoute = route
    }
}
