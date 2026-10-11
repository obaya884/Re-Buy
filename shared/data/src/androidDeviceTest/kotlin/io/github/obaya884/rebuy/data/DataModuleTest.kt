package io.github.obaya884.rebuy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.di.dataModule
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication

/**
 * Android で**本番の** `dataModule` が DB と DAO を 1 つずつだけ持つことを固定する。
 * iOS 側の対は `DataModuleIosTest`。
 *
 * `single` が `AppDatabase` の単一性の**唯一のガード**になっている（`AppDatabase.kt` の
 * KDoc と対）。`factory` に取り違えると DB の実体が複数できるので、同一性を直接押さえておく。
 *
 * **アプリ側（`:androidApp`）の instrumented は DB を in-memory に差し替えて走る**（T-21）ので、
 * 本番の `platformDataModule` の定義が解かれるのはここだけ。Repository から上の単一性は
 * `:androidApp` の `KoinGraphTest` が見る。
 *
 * **Android で本番の定義から DB ファイルを開くのもここだけ。** 開けること・`databases/` の下に
 * できることを見る。ライブラリのテスト APK は自分自身を計測するので、ファイルはテスト APK の
 * `databases/` に落ち、アプリの DB には触れない。**DB 名そのものは定数を共有しているので見ていない**（T-59）。
 *
 * global な Koin を使わず [koinApplication] のローカルコンテナで解く。
 */
@RunWith(AndroidJUnit4::class)
class DataModuleTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun productionKoin() = koinApplication {
        androidContext(context)
        modules(dataModule)
    }

    @Test
    fun 本番のモジュールはDBとDAOを1つずつだけ持つ() {
        val app = productionKoin()
        try {
            assertSame(app.koin.get<AppDatabase>(), app.koin.get<AppDatabase>())
            assertSame(app.koin.get<ItemDao>(), app.koin.get<ItemDao>())
            assertSame(app.koin.get<CategoryDao>(), app.koin.get<CategoryDao>())
            assertSame(app.koin.get<DestinationDao>(), app.koin.get<DestinationDao>())
            // DB の外に置く設定値（データモデル定義書 §9）
            assertSame(app.koin.get<SettingsStore>(), app.koin.get<SettingsStore>())
        } finally {
            app.close()
        }
    }

    @Test
    fun 本番のモジュールのDBはdatabasesの下にファイルを作って開ける() {
        // 新品の端末と同じく `databases/` ごと無い状態から開き、前の実行が残したファイルで
        // 素通りさせない。**親ディレクトリを作ること自体は見ていない**（無くても開ける。log_23）
        databasesDir().deleteRecursively()
        val app = productionKoin()
        try {
            // Room は最初のクエリで接続を開く
            runBlocking { app.koin.get<ItemDao>().getAllItems().first() }

            assertTrue(context.getDatabasePath(APP_DATABASE_NAME).exists())
        } finally {
            app.koin.get<AppDatabase>().close()
            app.close()
            databasesDir().deleteRecursively()
        }
    }

    private fun databasesDir() = File(context.dataDir, "databases")
}
