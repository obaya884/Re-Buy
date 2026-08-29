package io.github.obaya884.rebuy.data.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.obaya884.rebuy.data.APP_DATABASE_NAME
import io.github.obaya884.rebuy.data.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
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
    Room.databaseBuilder<AppDatabase>(name = "${documentDirectoryPath()}/$APP_DATABASE_NAME")
        .setDriver(BundledSQLiteDriver())
        // Native の Dispatchers.IO は 1.11.0 時点でも internal なので Default を使う。
        // Native の Default はスレッドプールなので、ブロッキング寄りの DB I/O でも
        // 単一スレッドを塞ぐことはない
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

/** iOS では Documents ディレクトリに置く。バックアップ対象で、アプリを消すまで残る。 */
@OptIn(ExperimentalForeignApi::class)
private fun documentDirectoryPath(): String {
    val url: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents ディレクトリが取れない" }
}
