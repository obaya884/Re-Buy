package io.github.obaya884.rebuy

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * 起動済みの Koin が、依存を 1 つずつだけ持っていることを固定する。
 *
 * `single` が `AppDatabase` の単一性の**唯一のガード**になっている（`AppDatabase.kt` の
 * KDoc と対）。`factory` に取り違えると DB の実体が複数できるので、同一性を直接押さえておく。
 *
 * 画面を開かないので、`NavigationTest` の到達性には依存しない。
 */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {

    private val koin get() = GlobalContext.get()

    @Test
    fun AppDatabaseは1つだけ() {
        assertSame(koin.get<AppDatabase>(), koin.get<AppDatabase>())
    }

    @Test
    fun ItemDaoは1つだけ() {
        assertSame(koin.get<ItemDao>(), koin.get<ItemDao>())
    }

    @Test
    fun CategoryDaoは1つだけ() {
        assertSame(koin.get<CategoryDao>(), koin.get<CategoryDao>())
    }

    @Test
    fun DestinationDaoは1つだけ() {
        assertSame(koin.get<DestinationDao>(), koin.get<DestinationDao>())
    }

    @Test
    fun ItemRepositoryは1つだけ() {
        assertSame(koin.get<ItemRepository>(), koin.get<ItemRepository>())
    }

    @Test
    fun CategoryRepositoryは1つだけ() {
        assertSame(koin.get<CategoryRepository>(), koin.get<CategoryRepository>())
    }

    @Test
    fun DestinationRepositoryは1つだけ() {
        assertSame(koin.get<DestinationRepository>(), koin.get<DestinationRepository>())
    }

    @Test
    fun 同じ型の定義が二重に積まれていない() {
        // 段 2 でモジュールを割ったとき、同じ定義が 2 か所に残っていないことを見る
        assertEquals(1, koin.getAll<ItemRepository>().size)
    }
}
