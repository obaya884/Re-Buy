package io.github.obaya884.rebuy.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * 起動済みの Koin が、依存を 1 つずつだけ持っていることを固定する。
 *
 * `single` を `factory` に取り違えても、いまは `AppDatabase` の companion キャッシュと
 * Room の DAO キャッシュが同じ実体を返すので**挙動に現れない**。③ の段 3 で companion の
 * キャッシュを畳むと `single` が唯一のガードになるため、同一性を直接押さえておく。
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
    fun ItemRepositoryは1つだけ() {
        assertSame(koin.get<ItemRepository>(), koin.get<ItemRepository>())
    }

    @Test
    fun CategoryRepositoryは1つだけ() {
        assertSame(koin.get<CategoryRepository>(), koin.get<CategoryRepository>())
    }

    @Test
    fun 同じ型の定義が二重に積まれていない() {
        // 段 2 でモジュールを割ったとき、同じ定義が 2 か所に残っていないことを見る
        assertEquals(1, koin.getAll<ItemRepository>().size)
    }
}
