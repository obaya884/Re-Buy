package io.github.obaya884.rebuy.ui.di

import io.github.obaya884.rebuy.domain.di.domainModule
import io.github.obaya884.rebuy.ui.screen.category_edit.CategoryEditViewModel
import io.github.obaya884.rebuy.ui.screen.pool.PoolViewModel
import io.github.obaya884.rebuy.ui.screen.register.RegisterViewModel
import io.github.obaya884.rebuy.ui.screen.shopping_start.ShoppingStartViewModel
import io.github.obaya884.rebuy.ui.screen.item_edit.ItemEditViewModel
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.screen.shopping.ShoppingViewModel
import io.github.obaya884.rebuy.ui.screen.theme.ThemeViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
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

    viewModelOf(::PoolViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ShoppingStartViewModel)
    // 行き先はルートが持つ。渡ってくるのはキーそのもの（`ShoppingScreen` の KDoc）
    viewModel { (route: Screen.Shopping) ->
        ShoppingViewModel(
            itemRepository = get(),
            destinationRepository = get(),
            destinationId = route.destinationId
        )
    }
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::ItemEditViewModel)
    viewModelOf(::ThemeViewModel)
}

/**
 * Koin を起動する。**アプリの起動時に 1 回だけ呼ぶ。**
 *
 * プラットフォームごとに違うのは [appDeclaration] に渡すものだけ——Android は
 * `androidContext(...)`、iOS は何も無い（DB のパスは `NSDocumentDirectory` から自力で引く）。
 * 起動の作法そのものを両側にコピーすると、`allowOverride` の方針を変えたときに
 * 片側だけ直る形になるので、ここに 1 つだけ置く。
 *
 * 呼び出し元は Android が `ReBuyApplication.onCreate()`、iOS が `setupKoin()`。
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    // 同じ型の定義が 2 か所に現れたら黙って後勝ちさせず、落として気づく
    allowOverride(false)
    appDeclaration()
    modules(uiModule)
}
