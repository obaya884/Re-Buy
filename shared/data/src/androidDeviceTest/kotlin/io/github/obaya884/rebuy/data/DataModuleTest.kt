package io.github.obaya884.rebuy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.di.dataModule
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
 * **Android で本番の定義から DB ファイルを開くのもここだけ。** 既存端末のデータがある場所に
 * 書くことを見る。ライブラリのテスト APK は自分自身を計測するので、ファイルはテスト APK の
 * `databases/` に落ち、アプリの DB には触れない。
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

    /**
     * **既存端末のデータは `databases/app_database` にある。** 名前も場所も本番の定数や
     * `getDatabasePath()` を使わず直書きする——共有すると、変えたときにテストも一緒に動いて
     * 緑のままになる。
     *
     * ここで落ちたら、テストを直す前に既存端末のデータの引き継ぎを考えること。
     */
    @Test
    fun 本番のモジュールのDBは既存端末と同じファイルに書く() {
        // 新品の端末と同じく `databases/` ごと無い状態から開き、前の実行が残したファイルで
        // 素通りさせない。**親ディレクトリを作ること自体は見ていない**（無くても開ける。log_23）
        databasesDir().deleteRecursively()
        val app = productionKoin()
        try {
            runBlocking { app.koin.get<ItemDao>().insertItem(Item(name = "アイテム1")) }
            // Room の接続を閉じてから、別の接続でファイルを読む
            app.koin.get<AppDatabase>().close()

            assertEquals(listOf("アイテム1"), itemNamesInFile(File(databasesDir(), "app_database")))
        } finally {
            app.koin.get<AppDatabase>().close()
            app.close()
            databasesDir().deleteRecursively()
        }
    }

    /**
     * **既存端末の設定値は `rebuy_settings` にある。** 名前を本番の定数と共有しない理由は上と同じ。
     *
     * ここで落ちたら、テストを直す前に既存端末の設定値の引き継ぎを考えること。
     */
    @Test
    fun 本番のモジュールの設定値は既存端末と同じファイルを読み書きする() {
        val preferences = context.getSharedPreferences("rebuy_settings", Context.MODE_PRIVATE)
        preferences.edit().clear().putString("既存のキー", "既存の値").commit()
        val app = productionKoin()
        try {
            val store = app.koin.get<SettingsStore>()

            assertEquals("既存の値", store.getString("既存のキー"))
            assertNull(store.getString("無いキー"))

            store.putString("新しいキー", "新しい値")
            assertEquals("新しい値", preferences.getString("新しいキー", null))
        } finally {
            app.close()
            preferences.edit().clear().commit()
        }
    }

    private fun databasesDir() = File(context.dataDir, "databases")

    /** Room を通さずにファイルの中身を見る。Room で開くと、無いファイルも空の DB として作ってしまう。 */
    private fun itemNamesInFile(file: File): List<String> =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            .use { database ->
                database.rawQuery("SELECT name FROM items", null).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
                }
            }
}
