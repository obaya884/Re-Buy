package io.github.obaya884.rebuy.domain.di

import io.github.obaya884.rebuy.data.di.dataModule
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    includes(dataModule)

    singleOf(::ItemRepository)
    singleOf(::CategoryRepository)
}
