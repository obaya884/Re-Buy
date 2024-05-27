package io.github.obaya884.favbasket.ui.screen.home

import io.github.obaya884.favbasket.data.category.Category

sealed class HomeTab(val title: String) {
    data object InBasket : HomeTab("買い物リスト")
    data object All : HomeTab("すべて")
    data class CategoryTab(val category: Category) : HomeTab(category.name)

    companion object {
        fun homeTabs(categories: List<Category>): List<HomeTab> {
            return listOf(
                InBasket,
                All,
            ) + categories.map { category -> CategoryTab(category) }
        }
    }
}

