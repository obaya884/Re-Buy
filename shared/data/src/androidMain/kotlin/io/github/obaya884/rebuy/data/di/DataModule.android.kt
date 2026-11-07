package io.github.obaya884.rebuy.data.di

import android.content.Context
import androidx.room.Room
import io.github.obaya884.rebuy.data.APP_DATABASE_NAME
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.applyAppDatabaseOptions
import io.github.obaya884.rebuy.data.settings.SettingsStore
import io.github.obaya884.rebuy.data.settings.SharedPreferencesSettingsStore
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { createAppDatabase(androidContext()) }
    single<SettingsStore> { SharedPreferencesSettingsStore(androidContext()) }
}

/**
 * **既存端末の DB を引き継ぐため、Room がこれまで使っていたのと同じ絶対パスを渡す。**
 * `getDatabasePath(APP_DATABASE_NAME)` は、旧コードの `Room.databaseBuilder(context, klass, name)`
 * が相対名から解決していた場所と同じ。
 *
 * **場所と名前を変えると、既存端末のデータが見えなくなる。** 新しい場所に空の DB ができるだけで
 * エラーにはならない。`DataModuleTest` が固定している。
 */
private fun createAppDatabase(context: Context): AppDatabase {
    // Activity を握り続けないための保険
    val applicationContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = applicationContext.getDatabasePath(APP_DATABASE_NAME).absolutePath,
    )
        .applyAppDatabaseOptions(queryContext = Dispatchers.IO)
        .build()
}
