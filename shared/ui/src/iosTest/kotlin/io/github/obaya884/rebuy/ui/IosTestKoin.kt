package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.category.CategoryDao
import io.github.obaya884.rebuy.data.item.ItemDao
import io.github.obaya884.rebuy.ui.di.initKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/**
 * `iosTest` が画面を描くときに使う DAO。**Room の代わりにこれを Koin へ差し込む**ので、
 * テストは 1 度もファイルを触らない。品目を置きたいテストは `seed(...)` を呼ぶ。
 */
val fakeDatabase = FakeDatabase()

/**
 * `iosTest` の Koin を用意する。**プロセスにつき 1 回だけ起動し、止めない。**
 *
 * 止めると 2 件目以降のテストが `ClosedScopeException` で落ちる——一度掴んだ root scope が
 * プロセス単位でキャッシュされるので、閉じると以後の composition が巻き添えになる
 * （実測。単独実行では通り、続けて走らせると落ちる）。**`stopKoin()` を呼ばないこと。**
 *
 * ### DAO を差し替える理由
 *
 * 本番の `platformDataModule` は `NSDocumentDirectory` の**実ファイル**に DB を作る。
 * これをそのまま使うと 2 つ困る。
 *
 * 1. **新品のシミュレータでは開けない。** テストバイナリはアプリのサンドボックスではなく
 *    シミュレータ共有のデータ領域で動くので、`data/Documents` が存在しないことがある。
 *    CI（macOS ランナーの新品シミュレータ）で実際に全件 `Unable to open database` で落ちた
 * 2. **実行と実行の間にも状態が残る。** 1 本のファイルを共有するので、書き込むテストを
 *    足した瞬間に次回の実行へ漏れる
 *
 * **iOS で本物の Room が動くことは、ここではなく `:shared:data` の iosTest が見る**（T-35）。
 * 画面遷移のテストがそこまで抱える必要は無い。
 *
 * ### 差し替えの順序が効く
 *
 * `single` は一度作ったら使い回されるので、**上書きは「これから作るもの」にしか効かない**。
 * `AppDatabase` → DAO → Repository はすべて `single` なので、画面を 1 度でも描いた後に
 * 差し替えても、Repository は古い DAO を掴んだままになる。**この関数が `setContent` より
 * 前に呼ばれること**が、差し替えが効く条件。
 *
 * `allowOverride = true` を渡せるのは `startKoin` の外だから。本番の
 * [initKoin] は `allowOverride(false)` のままで、**そちらの方針は緩めていない**。
 */
fun ensureKoinStarted() {
    // 「誰かが起動していれば何でもよい」判定になっている。いまは iosTest で Koin を触るのが
    // ここだけなので成立するが、触る側が増えたら先に走ったほうの構成を黙って使うことになる。
    //
    // `:androidApp` の KoinGraphTest は GlobalContext を使っているが、**あれは JVM 専用**で
    // Kotlin/Native からは解決できない（実測）。KMP で同じ判定をするのはこちら
    val context = KoinPlatformTools.defaultContext()
    if (context.getOrNull() != null) return

    initKoin()
    context.get().loadModules(
        listOf(
            module {
                single<ItemDao> { fakeDatabase.itemDao }
                single<CategoryDao> { fakeDatabase.categoryDao }
            }
        ),
        allowOverride = true
    )
}
