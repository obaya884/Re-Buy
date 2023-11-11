package io.github.obaya884.favbasket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.favbasket.data.AppDatabase
import io.github.obaya884.favbasket.data.item.ItemDao

@Module
@InstallIn(SingletonComponent::class)
class ItemDaoModule {
    @Provides
    fun provideItemDao(appDatabase: AppDatabase): ItemDao {
        return appDatabase.itemDao()
    }
}
