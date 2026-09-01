package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.destination.DestinationDao
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.data.settings.SettingsStore
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.di.initKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/**
 * 画面を描くテストが使う DAO の中身。**書き込みはテストの `prepare` からだけ行う。**
 *
 * `single` は最初の解決で 1 度だけ評価されてキャッシュされるので、Koin へ渡す実体は
 * プロセスに 1 つしか置けない。テストごとの独立は [startTestKoin] の戻しで作る。
 */
val fakeDatabase = FakeDatabase()

/**
 * 設定値の置き場も差し替える。本番は `NSUserDefaults` なので、**差し替えないと選んだテーマが
 * シミュレータに残り**、テストの実行順で結果が変わる。
 *
 * `ThemeRepository` は `single` でプロセスに 1 つ、生成時に 1 度だけ保存先を読むので、
 * **保存先を空にしても読み終えた値は戻らない**。[startTestKoin] が既定へ選び直すことで
 * 揃えているが、選択に依存するテストは**事前状態を自分で assert すること**——
 * 揃え方が外れたときに、テストの実行順で結果が変わる形に戻る。
 */
val fakeSettingsStore = FakeSettingsStore()

/**
 * テスト用の Koin を用意し、[fakeDatabase] と [fakeSettingsStore] を空へ戻してから
 * [prepare] を適用する。
 *
 * **Koin はプロセスにつき 1 回だけ起動し、止めない。** 止めると 2 件目以降が
 * `ClosedScopeException` で落ちる——一度掴んだ root scope がプロセス単位でキャッシュされ、
 * 閉じると以後の composition が巻き添えになる。**`stopKoin()` を呼ばないこと。**
 *
 * **画面が composition に入る前に呼ぶこと。** DAO の差し替えは `loadModules` で行うが、
 * `AppDatabase` → DAO → Repository はすべて `single` なので、**上書きは「これから作るもの」に
 * しか効かない**。composition が走った後では Repository が本物の DAO を掴んだままになる。
 * **この順序はテストで守れていない**——`setContent` は即座に composition を走らせないので、
 * 後ろで呼んでも現状は間に合ってしまう（変異で実測）。`setContent` の前に置くのは約束事。
 *
 * DAO を差し替えるのは、本番が `NSDocumentDirectory` の実ファイルに DB を作るため
 * （新品のシミュレータでは開けず、実行と実行の間にも状態が残る。経緯は T-48）。
 * **iOS で本物の Room が動くことは `:shared:data` の iosTest が見る**（T-35）。
 *
 * **`dataModule` に DAO を足したらここにも足すこと。** 差し替え漏れた DAO は本物の Room を
 * 起こすので、手元では黙って実ファイルを使い、CI だけが落ちる。
 */
fun startTestKoin(prepare: FakeDatabase.() -> Unit = {}) {
    // 起動済みなら差し替えは済んでいる。`allowOverride = true` を渡せるのは startKoin の外
    // だからで、本番の initKoin は allowOverride(false) のまま触っていない
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        initKoin().koin.loadModules(
            listOf(
                module {
                    single<ItemDao> { fakeDatabase.itemDao }
                    single<CategoryDao> { fakeDatabase.categoryDao }
                    single<DestinationDao> { fakeDatabase.destinationDao }
                    single<SettingsStore> { fakeSettingsStore }
                }
            ),
            allowOverride = true
        )
    }
    fakeDatabase.seed()
    fakeSettingsStore.clear()
    // 選んだテーマもここで既定へ戻す。**保存先を空にするだけでは戻らない**ので、
    // テーマを変えるテストの後ろに並んだテストが巻き添えになる（実測）
    KoinPlatformTools.defaultContext().get().get<ThemeRepository>().select(ThemePalette.DEFAULT)
    fakeDatabase.prepare()
}
