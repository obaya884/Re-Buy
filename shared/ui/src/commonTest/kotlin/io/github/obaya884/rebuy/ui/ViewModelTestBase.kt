package io.github.obaya884.rebuy.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * `Dispatchers.Main` をテスト用のディスパッチャに差し替える。
 *
 * `viewModelScope` は `Dispatchers.Main` に紐づいており、テストの実行環境には Android の
 * メインルーパーが無いため、差し替えないと ViewModel を作った時点で落ちる。
 *
 * `StandardTestDispatcher` なので、コルーチンは `runTest` の中で
 * `advanceUntilIdle()` などを呼ぶまで走らない。「まだ走っていない状態」を
 * 意図的に観測できるようにするための選択。
 *
 * common に `@get:Rule` が無いので基底クラスで差し替える。
 *
 * **継承の書き忘れが確実に落ちるのは Android だけ。** 実測では、継承を外すと Android は
 * 全件落ちるが iOS は約半分が緑のまま通る（`androidx.lifecycle` は Main の不在を例外で
 * 捕まえるが、iOS には Darwin の main キューが実在するので例外にならず、コルーチンが
 * 積まれたまま走らない。否定形のアサートはその状態で緑になる）。
 * **移送の判定は必ず両ターゲットで見ること。**
 *
 * サブクラスに `@BeforeTest` / `@AfterTest` を足さないこと。kotlin.test は同種の
 * アノテーション同士の実行順序を約束していないので、JUnit4 の Rule のように
 * 「必ず外側を包む」保証が無い。
 */
abstract class ViewModelTestBase {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }
}
