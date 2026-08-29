package io.github.obaya884.rebuy.di

import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditViewModel
import io.github.obaya884.rebuy.ui.screen.home.HomeViewModel
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditViewModel
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** UI 層の依存。③ の段 2 で `:shared:ui` へ移す。 */
val uiModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ShoppingViewModel)
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::ItemEditViewModel)
}
