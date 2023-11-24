package io.github.obaya884.favbasket.ui.screen.home

import io.github.obaya884.favbasket.data.category.Category

sealed class HomeTab(val title: String) {
    data object AllTab : HomeTab("すべて")
    data class CategoryTab(val category: Category) : HomeTab(category.name)
}

fun homeTabs(categories: List<Category>): List<HomeTab> {
    return listOf(HomeTab.AllTab) + categories.map { category -> HomeTab.CategoryTab(category) }
}
