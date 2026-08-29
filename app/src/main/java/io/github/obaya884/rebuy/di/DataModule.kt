package io.github.obaya884.rebuy.di

import io.github.obaya884.rebuy.data.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * データ層の依存。③ の段 2 で `:shared:data` へ移す。
 *
 * DAO を `single` にしているのは、`AppDatabase` がシングルトンで
 * `itemDao()` が毎回同じインスタンスを返すため。
 */
val dataModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().categoryDao() }
}
