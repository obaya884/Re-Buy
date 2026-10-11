package io.github.obaya884.rebuy

import android.app.Application
import androidx.room.Room
import io.github.obaya884.rebuy.data.AppDatabase
import io.github.obaya884.rebuy.data.applyAppDatabaseOptions
import io.github.obaya884.rebuy.data.settings.SettingsStore
import io.github.obaya884.rebuy.ui.di.initKoin
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * instrumented 用の `Application`。**端末にある本物の DB と設定値に触れない**（T-21）。
 *
 * 本番の [ReBuyApplication] と同じく [initKoin] で起動し、その直後に DB と設定値の保存先を
 * メモリ上のものへ差し替える。**差し替えはプロセスの起動時、何も解決されていないうちに
 * 済ませる必要がある**——`AppDatabase` → DAO → Repository はすべて `single` なので、
 * 上書きは「これから作るもの」にしか効かない。テスト本体で差し替えると、
 * `createAndroidComposeRule` が先に起動した Activity が本物の DB を掴んだ後になる。
 *
 * `allowOverride = true` を渡せるのは起動後の `loadModules` だからで、本番の
 * `initKoin` は `allowOverride(false)` のまま触っていない。
 *
 * **`dataModule` が DB の外に保存先を足したら、ここにも足すこと。** 差し替え漏れた
 * 保存先は端末の本物を読み書きする。
 *
 * テストごとの初期化は [TestAppStateRule] が持つ。
 */
class TestReBuyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TestReBuyApplication)
        }.koin.loadModules(
            listOf(
                module {
                    // 開き方（driver・クエリのコンテキスト）は本番と同じ。違うのは置き場所だけ
                    single<AppDatabase> {
                        Room.inMemoryDatabaseBuilder<AppDatabase>(androidContext())
                            .applyAppDatabaseOptions(queryContext = Dispatchers.IO)
                            .build()
                    }
                    single<SettingsStore> { InMemorySettingsStore() }
                }
            ),
            allowOverride = true
        )
    }
}

/**
 * 設定値の保存先のメモリ版。本番は `SharedPreferences` で、端末に残る。
 *
 * 画面（メインスレッド）が書き、[TestAppStateRule]（テストのスレッド）が消すので、
 * スレッドをまたげる入れ物にする。
 */
class InMemorySettingsStore : SettingsStore {
    private val values = ConcurrentHashMap<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    fun clear() = values.clear()
}
