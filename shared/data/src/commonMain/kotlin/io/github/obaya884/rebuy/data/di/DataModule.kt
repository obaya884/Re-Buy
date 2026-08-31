package io.github.obaya884.rebuy.data.di

import io.github.obaya884.rebuy.data.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `AppDatabase` の作り方はプラットフォームで違う（Android は `Context`、iOS は Documents
 * ディレクトリ）。その差だけをここに閉じ込め、`dataModule` の形は共通に保つ。
 */
expect val platformDataModule: Module

/** データ層の依存。 */
val dataModule = module {
    includes(platformDataModule)
    // DAO は DB ごとに 1 つの実体を使い回す
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().destinationDao() }
}
