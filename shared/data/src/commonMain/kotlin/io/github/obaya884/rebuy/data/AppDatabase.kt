package io.github.obaya884.rebuy.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao

/** DB のファイル名。**変えると既存端末のデータを見失う。** */
internal const val APP_DATABASE_NAME = "app_database"

@Database(
    entities = [Item::class, Category::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class, ItemStatusConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
}

/**
 * `actual` は Room の KSP がターゲットごとに生成する。
 *
 * 単一性は Koin の `single` が保証するので、ここにインスタンスのキャッシュは持たない
 * （`synchronized` は JVM 専用で common に置けないという事情もある）。
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
