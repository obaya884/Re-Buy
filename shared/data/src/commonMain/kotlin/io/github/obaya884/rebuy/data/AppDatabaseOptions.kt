package io.github.obaya884.rebuy.data

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * version ごとの [Migration]。**スキーマを変えたらここに足す**（アーキテクチャ定義書 §2.3）。
 *
 * 本番の両プラットフォームがこの 1 つを見るので、足せば両方に効く。
 */
internal val ALL_MIGRATIONS: Array<Migration> = emptyArray()

/**
 * 本番の DB の開き方。**パスの解決と `build()` 以外はここが 1 か所で持つ**
 * （アーキテクチャ定義書 §2.2）。
 *
 * **`fallbackToDestructiveMigration` は足さない。** 移行できない DB を黙って消すより、
 * 起動時に落として気づけるほうを採る（§2.3）。落ちること・落ちてもデータが残ることは
 * `RoomMigrationTest` が本関数を通して見ている（両方とも変異で実測）。
 *
 * **`addMigrations` の配線には網が無い。** [ALL_MIGRATIONS] が空のうちは落としても誰も
 * 落ちず、`runMigrationsAndValidate` は [Migration] を引数で受け取って自前の設定で回すので
 * **本関数を通らない**。旧 version の DB を本関数で開いて移行後の行を読むテストだけが
 * ここに歯を与える（T-34）。
 *
 * @param queryContext クエリを流すコンテキスト。**`Dispatchers.IO` は commonMain から
 * 引けない**（Native では `Dispatchers` のメンバではないため）ので、呼ぶ側が渡す。
 * coroutines が common に持つようになれば、引数ごと落とせる。
 */
fun RoomDatabase.Builder<AppDatabase>.applyAppDatabaseOptions(
    queryContext: CoroutineContext,
): RoomDatabase.Builder<AppDatabase> =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .addMigrations(*ALL_MIGRATIONS)
