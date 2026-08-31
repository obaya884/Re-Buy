package io.github.obaya884.rebuy.data

import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.di.dataModule
import io.github.obaya884.rebuy.data.item.ItemDao
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * iOS で**本番の** DI 配線が解けることを見る（T-35）。
 *
 * Android には `androidHostTest` の `KoinModulesTest`（koin-test の `verify()`）と
 * instrumented の `KoinGraphTest` の 2 枚があるが、**iOS には 1 枚も無かった**。
 * `:shared:ui` の `IosTestKoinTest` は DAO を `FakeDatabase` へ差し替えたうえで見るので、
 * `platformDataModule` の `single { createAppDatabase() }` は一度も評価されない。
 * その結果、`databaseFilePath()` の `checkNotNull` も Koin 定義の欠落も、
 * **起動時クラッシュでしか気づけない**状態だった。
 *
 * `verify()` は JVM のリフレクションに依存するので native では使えない。実際に解決して見る。
 *
 * ### クエリを 1 本も投げない
 *
 * **意図的に投げない。** Room は最初のクエリで接続を開くので、投げると
 * `NSDocumentDirectory` に実ファイルを作ることになり、T-48 で直した
 * 「新品のシミュレータでは開けない」に逆戻りする。ここで見るのは**組み立てまで**。
 * 実際に開く経路は実物のアプリを起動する層でしか見られない（T-46）。
 *
 * ### global な Koin を使わない
 *
 * `startKoin` ではなく [koinApplication] のローカルコンテナで解く。同じプロセスで走る
 * `:shared:ui` の `iosTest` が global を差し替えているので、そちらに影響されない。
 */
class DataModuleIosTest {

    @Test
    fun 本番のモジュールからDBとDAOを解決できる() {
        val app = koinApplication { modules(dataModule) }
        try {
            val database = app.koin.get<AppDatabase>()

            // single なので何度引いても同じ実体。DAO も DB ごとに 1 つを使い回す
            assertSame(database, app.koin.get<AppDatabase>())
            assertSame(app.koin.get<ItemDao>(), app.koin.get<ItemDao>())
            assertSame(app.koin.get<CategoryDao>(), app.koin.get<CategoryDao>())
        } finally {
            app.close()
        }
    }
}
