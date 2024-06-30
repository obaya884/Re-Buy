package io.github.obaya884.favbasket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.obaya884.favbasket.data.category.Category
import io.github.obaya884.favbasket.data.category.CategoryDao
import io.github.obaya884.favbasket.data.item.Item
import io.github.obaya884.favbasket.data.item.ItemDao

@Database(
    entities = [Item::class, Category::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(InstantDateFormatStringConverter::class, DateLongConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        /*
         * 日時を扱う列の型をLONGからTEXT("YYYY-MM-DD HH:MM:SS")に変更するマイグレーション
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Itemテーブルのマイグレーション
                db.execSQL("DROP INDEX IF EXISTS index_items_categoryId")
                db.execSQL("ALTER TABLE items RENAME TO items_old")
                db.execSQL(
                    """
                    CREATE TABLE items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        status TEXT NOT NULL,
                        categoryId INTEGER,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO items (id, name, status, categoryId, createdAt, updatedAt)
                    SELECT id, name, status, categoryId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM items_old
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX index_items_categoryId ON items(categoryId)")
                db.execSQL("DROP TABLE items_old")

                // Categoryテーブルのマイグレーション
                db.execSQL("ALTER TABLE categories RENAME TO categories_old")
                db.execSQL(
                    """
                    CREATE TABLE categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO categories (id, name, createdAt, updatedAt)
                    SELECT id, name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM categories_old
                """.trimIndent()
                )
                db.execSQL("DROP TABLE categories_old")
            }
        }
    }
}
