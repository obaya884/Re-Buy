package io.github.obaya884.favbasket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.obaya884.favbasket.data.category.CategoryDao
import io.github.obaya884.favbasket.domain.CategoryRepository

@Module
@InstallIn(SingletonComponent::class)
class CategoryRepositoryModule {
    @Provides
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepository(categoryDao)
    }
}
