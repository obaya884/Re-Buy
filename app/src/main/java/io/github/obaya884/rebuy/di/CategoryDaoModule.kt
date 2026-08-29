package io.github.obaya884.favbasket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.favbasket.data.AppDatabase
import io.github.obaya884.favbasket.data.category.CategoryDao

@Module
@InstallIn(SingletonComponent::class)
class CategoryDaoModule {
    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }
}
