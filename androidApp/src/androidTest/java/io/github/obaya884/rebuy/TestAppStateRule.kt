package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.category.Category
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.koin.core.context.GlobalContext

/**
 * テストごとに DB と設定値を空に戻し、[seed] を入れる（T-21）。
 * 差し替えそのものは [TestReBuyApplication] が持つ。
 *
 * **`order = 0` で、画面を起動するルールより先に置くこと。** Koin の `single` は
 * プロセスに 1 つで、テストをまたいで生き残る。前のテストが書いた行が残っていると、
 * 同じ名前の登録が重複で弾かれて別の理由で落ちる。シードも、画面が開く前に入れておけば
 * 「最初に描かれたもの」を見るテストが流れの到着を待たずに済む。
 *
 * ```
 * @get:Rule(order = 0) val appState = TestAppStateRule { item(Item(name = "…")) }
 * @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
 * ```
 */
class TestAppStateRule(
    private val seed: suspend Seed.() -> Unit = {}
) : ExternalResource() {

    override fun before() {
        val koin = GlobalContext.get()
        // メインスレッドで呼ぶと Room が落とす。ルールはテストのスレッドで走る
        koin.get<AppDatabase>().clearAllTables()
        // **戻す順に意味がある**（iOS の startTestKoin と同じ）。ThemeRepository は single で
        // 生成時に 1 度だけ読むので、保存先を空にしても読み終えた値は戻らない。select が
        // 保存先に既定を書くので、clear はその後。**この 2 行にはまだ網が無い**——androidTest に
        // テーマを変えるテストが無いので、外しても落ちない。足すときは「変えるテスト → 既定に
        // 戻っていることを見るテスト」の対で置く
        koin.get<ThemeRepository>().select(ThemePalette.DEFAULT)
        (koin.get<SettingsStore>() as InMemorySettingsStore).clear()
        runBlocking { Seed(koin.get(), koin.get(), koin.get()).seed() }
    }

    /** シードの書き口。**DAO に直接書く**——画面を通すと、そこが壊れたときに無関係なテストまで落ちる。 */
    class Seed(
        private val itemDao: ItemDao,
        private val categoryDao: CategoryDao,
        private val destinationDao: DestinationDao
    ) {
        /** @return 振られた id。カテゴリを品目に結ぶのに使う */
        suspend fun category(name: String): Int =
            categoryDao.insert(Category(name = name)).toInt()

        /** @return 振られた id。行き先を品目に結ぶのに使う */
        suspend fun destination(name: String): Int =
            destinationDao.insert(Destination(name = name)).toInt()

        suspend fun item(item: Item) {
            itemDao.insertItem(item)
        }
    }
}
