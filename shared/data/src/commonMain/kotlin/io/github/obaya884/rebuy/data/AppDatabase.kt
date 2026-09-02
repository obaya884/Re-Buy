package io.github.obaya884.rebuy.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao

/** DB のファイル名。**変えると既存端末のデータを見失う。** */
internal const val APP_DATABASE_NAME = "app_database"

/**
 * スキーマ version。**上げるときは必ず `Migration` とセット**（データモデル定義書 §8）。
 *
 * **`@Database` の retention は BINARY なので実行時には読めない。** テストが version を
 * ベタ書きせずに済むよう、ここを唯一の宣言にしている。
 */
const val APP_DATABASE_VERSION = 1

@Database(
    entities = [Item::class, Category::class, Destination::class],
    version = APP_DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class, ItemStatusConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun destinationDao(): DestinationDao
}

/**
 * `actual` は Room の KSP がターゲットごとに生成する。
 *
 * **単一性は Koin の `single` が保証する。** ここにも [AppDatabase] にもキャッシュは持たない。
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
