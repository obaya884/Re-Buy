package io.github.obaya884.rebuy.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.domain.ItemRepository

@Module
@InstallIn(SingletonComponent::class)
class ItemRepositoryModule {
    @Provides
    fun provideItemRepository(itemDao: ItemDao): ItemRepository {
        return ItemRepository(itemDao)
    }
}
