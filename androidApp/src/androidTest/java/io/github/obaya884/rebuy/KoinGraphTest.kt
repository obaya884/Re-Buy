package io.github.obaya884.rebuy

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.ThemeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * 起動済みの Koin が、Repository を 1 つずつだけ持っていることを固定する。
 *
 * **DB と DAO は `:shared:data` の `DataModuleTest` が見る**（T-47）。ここで起動している
 * Koin は `TestReBuyApplication` が DB を in-memory に差し替えたもので（T-21）、本番の
 * `AppDatabase` の定義は解かれない。
 *
 * 画面を開かないので、`NavigationTest` の到達性には依存しない。
 */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {

    private val koin get() = GlobalContext.get()

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

    /** factory に取り違えると、画面と `ReBuyApp` が別の実体を見てテーマが反映されない。 */
    @Test
    fun ThemeRepositoryは1つだけ() {
        assertSame(koin.get<ThemeRepository>(), koin.get<ThemeRepository>())
    }

    @Test
    fun 同じ型の定義が二重に積まれていない() {
        // 段 2 でモジュールを割ったとき、同じ定義が 2 か所に残っていないことを見る
        assertEquals(1, koin.getAll<ItemRepository>().size)
    }
}
