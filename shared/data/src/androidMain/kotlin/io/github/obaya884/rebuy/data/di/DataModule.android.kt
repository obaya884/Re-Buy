package io.github.obaya884.rebuy.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.obaya884.rebuy.data.APP_DATABASE_NAME
import io.github.obaya884.rebuy.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { createAppDatabase(androidContext()) }
}

/**
 * **既存端末の DB を引き継ぐため、Room がこれまで使っていたのと同じ絶対パスを渡す。**
 * `getDatabasePath(APP_DATABASE_NAME)` は、旧コードの `Room.databaseBuilder(context, klass, name)`
 * が置いていた場所と同じ。名前の付け方を変えると利用者のデータが消える。
 */
private fun createAppDatabase(context: Context): AppDatabase {
    val applicationContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = applicationContext.getDatabasePath(APP_DATABASE_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
