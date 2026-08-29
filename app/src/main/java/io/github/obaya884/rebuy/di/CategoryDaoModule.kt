package io.github.obaya884.rebuy.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.category.CategoryDao

@Module
@InstallIn(SingletonComponent::class)
class CategoryDaoModule {
    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }
}
