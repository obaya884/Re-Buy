package io.github.obaya884.favbasket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.favbasket.data.item.ItemDao
import io.github.obaya884.favbasket.domain.ItemRepository

@Module
@InstallIn(SingletonComponent::class)
class ItemRepositoryModule {
    @Provides
    fun provideItemRepository(itemDao: ItemDao): ItemRepository {
        return ItemRepository(itemDao)
    }
}
