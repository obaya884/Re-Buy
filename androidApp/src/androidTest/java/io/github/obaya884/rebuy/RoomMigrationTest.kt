package io.github.obaya884.rebuy

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.rebuy.data.APP_DATABASE_VERSION
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.applyAppDatabaseOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val databasePath = context.getDatabasePath(TEST_DB).absolutePath

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    /**
     * **旧経路（`MigrationTestHelper` の framework SQLite）で書いた行を、本番の経路
     * （`BundledSQLiteDriver` ＋ 絶対パス）で読めること**（T-25 (d)）。
     *
     * 開けるだけでなく**行を読む**のが要点。空の DB を開くだけだと、driver 間で値の
     * 読み方が食い違っても素通りする。
     */
    @Test
    fun 旧経路で書いた行を本番の経路で読める() {
        helper.createDatabase(TEST_DB, APP_DATABASE_VERSION).apply {
            insertItem(name = "アイテム1")
            close()
        }

        assertTrue(
            withProductionOptions { database ->
                runBlocking { database.itemDao().existsName("アイテム1", exceptId = 0) }
            }
        )
    }

    /**
     * **移行できない DB は、消さずに落ちる**（アーキテクチャ定義書 §2.3）。
     *
     * `fallbackToDestructiveMigration` を本番の設定（`applyAppDatabaseOptions`）に足すと、
     * 例外が飛ばなくなり、行も消えて落ちる（両方とも変異で実測）。**ただし独立な 2 本の網
     * ではない**——Room は失敗時にロールバックするので、例外が飛ぶ世界では行が残るのは
     * ほぼ自動的に真。行を見るのは「将来 Room が落とし方を変えた」ときのための観測点。
     *
     * **移行できない向きはダウングレードで作るしかない。** version 1 が起点なのでこれより
     * 古い DB は作れず（version 0 は `onCreate` へ行く）、上げ方向のフィクスチャが無い。
     * Room は上下どちらも同じ経路（`onMigrate` → 移行が見つからなければ例外）を通り、
     * `fallbackToDestructiveMigration` が落とすフラグも方向に依存しないので、
     * **この変異に対しては下げ方向で十分**。version 2 が出たら上げ方向を足す（T-34）。
     */
    @Test
    fun 移行できないDBは落ちてデータは残る() {
        helper.createDatabase(TEST_DB, APP_DATABASE_VERSION).apply {
            insertItem(name = "アイテム1")
            execSQL(
                "INSERT INTO categories (name, sortOrder, createdAt, updatedAt) " +
                    "VALUES ('カテゴリー1', 1, 0, 0)"
            )
            // アプリが知らない version。この DB への移行は誰も書いていない
            version = APP_DATABASE_VERSION + 1
            close()
        }

        val error = assertThrows(IllegalStateException::class.java) {
            withProductionOptions { database ->
                runBlocking { database.itemDao().existsName("アイテム1", exceptId = 0) }
            }
        }

        // Room の他の失敗（スキーマ照合など）も IllegalStateException なので、理由まで見る。
        // 文面は Room の版が変われば追随する
        assertTrue(
            "移行が無いことが理由ではない: ${error.message}",
            error.message.orEmpty().contains(
                "A migration from ${APP_DATABASE_VERSION + 1} to $APP_DATABASE_VERSION " +
                    "was required but not found"
            )
        )
        // テーブル単位で落ちる退行も見たいので 2 つ読む
        assertEquals(listOf("アイテム1"), namesInFile("items"))
        assertEquals(listOf("カテゴリー1"), namesInFile("categories"))
    }

    /** 本番（`DataModule.android.kt`）と同じ設定で開く。`build()` は遅延なのでクエリまで打つ。 */
    private fun <T> withProductionOptions(block: (AppDatabase) -> T): T {
        val database = Room.databaseBuilder<AppDatabase>(context = context, name = databasePath)
            .applyAppDatabaseOptions(queryContext = Dispatchers.IO)
            .build()
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    /** Room を通さずにファイルの中身を見る。**Room で開くと落ちる DB なので生で読む。** */
    private fun namesInFile(table: String): List<String> =
        SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
            .use { database ->
                database.rawQuery("SELECT name FROM $table", null).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
                }
            }

    private fun SupportSQLiteDatabase.insertItem(name: String) = execSQL(
        "INSERT INTO items (name, status, createdAt, updatedAt) VALUES (?, 0, 0, 0)",
        arrayOf(name)
    )

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
