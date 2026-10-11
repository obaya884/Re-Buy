package io.github.obaya884.rebuy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * instrumented が**端末の本物の DB と設定値に触れていない**ことを固定する（T-21）。
 *
 * 差し替え（[TestReBuyApplication]）が外れても、他のテストは本物の DB の上でそのまま緑になる。
 * 実機で回したときにオーナーのデータを読み書きする形へ、**黙って戻る**のを止めるのがここ。
 *
 * アプリの `databases/` が空であることを見るので、**まっさらな端末（GMD）を前提にしている**。
 * アプリを使っている実機では本物のファイルが既にあり、差し替えが効いていても落ちる。
 * **本番の DB が `databases/` の下にあることも前提にしている**——外へ動くとここは素通りするが、
 * そのときは `:shared:data` の `DataModuleTest` が落ちる。2 本で 1 つの網なので、片方だけ消さない。
 */
@RunWith(AndroidJUnit4::class)
class TestReBuyApplicationTest {

    private val koin get() = GlobalContext.get()

    @Test
    fun クエリを投げてもDBファイルは作られない() {
        // Room は最初のクエリで接続を開く。ここまで来れば、本物ならファイルができている
        runBlocking { koin.get<ItemDao>().getAllItems().first() }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(emptyList<String>(), context.databaseList().toList())
    }

    /**
     * Runner が `Application` を差し替えるので、**本番の [ReBuyApplication] はどのテストも通らない**。
     * マニフェストからの指定が外れると本番だけが Koin を起動せずに落ちるので、指定だけは押さえる。
     */
    @Test
    fun マニフェストのApplicationは本番のもの() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(ReBuyApplication::class.java.name, info.className)
    }

    @Test
    fun 設定値の保存先はメモリ上のもの() {
        assertTrue(koin.get<SettingsStore>() is InMemorySettingsStore)
    }
}
