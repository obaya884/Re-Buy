package io.github.obaya884.rebuy.di

import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditViewModel
import io.github.obaya884.rebuy.ui.screen.home.HomeViewModel
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditViewModel
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * UI 層の依存。**アプリが読み込む唯一の入口**。
 *
 * 各層のモジュールは `includes` で 1 つ下の層だけを知る。この連なりが
 * `:shared:ui` → `:shared:domain` → `:shared:data` という Gradle の依存の向き
 * （③ の段 2 で作る）とそのまま対応する。
 */
val uiModule = module {
    includes(domainModule)

    viewModelOf(::HomeViewModel)
    viewModelOf(::ShoppingViewModel)
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::ItemEditViewModel)
}
