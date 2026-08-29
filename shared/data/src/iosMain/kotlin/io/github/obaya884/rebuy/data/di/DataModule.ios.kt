package io.github.obaya884.rebuy.data.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.obaya884.rebuy.data.APP_DATABASE_NAME
import io.github.obaya884.rebuy.data.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
// Native では Dispatchers.IO は拡張プロパティなので、この import が要る
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual val platformDataModule: Module = module {
    single { createAppDatabase() }
}

private fun createAppDatabase(): AppDatabase =
    Room.databaseBuilder<AppDatabase>(name = databaseFilePath())
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/**
 * iOS では Documents ディレクトリに置く。バックアップ対象で、アプリを消すまで残る。
 *
 * `create = false` にしてあるのは、Documents は OS が必ず用意するため。
 * 無ければ異常なので、黙って作らずに起動を止める。
 */
@OptIn(ExperimentalForeignApi::class)
private fun databaseFilePath(): String {
    val documents: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val file = checkNotNull(documents?.URLByAppendingPathComponent(APP_DATABASE_NAME)?.path) {
        "Documents ディレクトリが取れない"
    }
    return file
}
