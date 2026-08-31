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
 * が相対名から解決していた場所と同じ。
 *
 * **`getDatabasePath()` は `databases/` ディレクトリが無ければ作る。** 同梱 driver は
 * 親ディレクトリを作らないので、`File(context.dataDir, "databases/…")` のように
 * 「単純化」すると初回起動が壊れる。
 */
private fun createAppDatabase(context: Context): AppDatabase {
    // Activity を握り続けないための保険
    val applicationContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = applicationContext.getDatabasePath(APP_DATABASE_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // ④ の実装中は Migration を書かず入れ直す（データモデル定義書 §8）。
        // **MVP 投入前に外す**（T-51）。外し忘れると本番でデータが消える
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
