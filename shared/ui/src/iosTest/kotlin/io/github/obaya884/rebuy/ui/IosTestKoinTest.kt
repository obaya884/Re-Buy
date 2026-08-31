package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * [startTestKoin] の差し替えが効いていることを、画面を描かずに直接見る。
 *
 * 画面のテストからでも間接的には分かるが、**落ち方が「アイテム1 というテキストが無い」に
 * なって原因から遠い**。ここが落ちれば差し替えそのものが外れたと即断できる。
 */
class IosTestKoinTest {

    @Test
    fun 差し替えた後にKoinから引けるDAOはfakeのもの() {
        startTestKoin()

        val koin = KoinPlatformTools.defaultContext().get()
        assertSame(fakeDatabase.itemDao, koin.get<ItemDao>())
        assertSame(fakeDatabase.categoryDao, koin.get<CategoryDao>())
        assertSame(fakeDatabase.destinationDao, koin.get<DestinationDao>())
    }

    /**
     * 設定値の置き場も fake であること。**外れると本物の `NSUserDefaults` に書かれ、
     * 選んだテーマがシミュレータに残って以後のテストが実行順で結果を変える。**
     */
    @Test
    fun 差し替えた後にKoinから引ける設定の置き場はfakeのもの() {
        startTestKoin()

        assertSame(fakeSettingsStore, KoinPlatformTools.defaultContext().get().get<SettingsStore>())
    }
}
