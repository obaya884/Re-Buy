package io.github.obaya884.rebuy.ui.di

import android.content.Context
import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Koin の依存グラフを組み立てられることを固定する。
 *
 * Hilt はグラフをコンパイル時に検証していたが、Koin は起動時まで分からない。
 * その保証を JVM 段で取り戻すためのテスト。`verify` は**モジュール側の定義を列挙して**
 * コンストラクタ引数の型がグラフに居るかを見るので、定義が増えれば自動で対象に入る
 * （画面を開くテストの到達性に依存しない）。
 *
 * `extraTypes` に [Context] を挙げているのは、`androidContext()` が Koin の外から
 * 注入される値で、定義としては現れないため。
 */
class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun uiModuleから全層の依存グラフが組み立てられる() {
        // uiModule が includes で domainModule を、domainModule が dataModule を連れてくる
        uiModule.verify(extraTypes = listOf(Context::class))
    }
}
