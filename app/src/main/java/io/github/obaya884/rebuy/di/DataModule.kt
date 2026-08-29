package io.github.obaya884.rebuy.di

import io.github.obaya884.rebuy.data.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** データ層の依存。 */
val dataModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    // DAO は DB ごとに 1 つの実体を使い回す
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().categoryDao() }
}
