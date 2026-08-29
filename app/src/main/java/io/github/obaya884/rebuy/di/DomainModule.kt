package io.github.obaya884.rebuy.di

import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import org.koin.dsl.module

/** ドメイン層の依存。③ の段 2 で `:shared:domain` へ移す。 */
val domainModule = module {
    single { ItemRepository(get()) }
    single { CategoryRepository(get()) }
}
