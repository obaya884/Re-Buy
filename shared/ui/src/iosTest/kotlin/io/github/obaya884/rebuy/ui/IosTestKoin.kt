package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.ui.di.initKoin
import org.koin.mp.KoinPlatformTools

/**
 * `iosTest` の Koin。**このバイナリでは Koin をプロセスにつき 1 回だけ起動し、止めない。**
 *
 * 止めると 2 件目以降のテストが `ClosedScopeException` で落ちる——一度掴んだ root scope が
 * プロセス単位でキャッシュされるので、閉じると以後の composition が巻き添えになる
 * （実測。単独実行では通り、続けて走らせると落ちる）。**`stopKoin()` を呼ばないこと。**
 *
 * その結果、`AppDatabase` は `single` かつ実ファイル（`NSDocumentDirectory`）なので
 * **1 本の DB がテスト間でも実行と実行の間でも共有される**。いまの `iosTest` は 1 件も
 * 書き込まないので害が出ていないだけで、**書き込むテストを足すときはこの方針ごと見直す**
 * （テスト用モジュールで in-memory に差し替える。[T-48]）。
 */
fun ensureKoinStarted() {
    // 「誰かが起動していれば何でもよい」判定になっている。いまは iosTest で Koin を触るのが
    // ここだけなので成立するが、触る側が増えたら先に走ったほうの構成を黙って使うことになる。
    //
    // `:androidApp` の KoinGraphTest は GlobalContext を使っているが、**あれは JVM 専用**で
    // Kotlin/Native からは解決できない（実測）。KMP で同じ判定をするのはこちら
    if (KoinPlatformTools.defaultContext().getOrNull() == null) initKoin()
}
