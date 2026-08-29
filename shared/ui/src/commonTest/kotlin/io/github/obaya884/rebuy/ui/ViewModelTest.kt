package io.github.obaya884.rebuy.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
 * JUnit4 の `TestWatcher` を使った `MainDispatcherRule` から基底クラスに移した。
 * `@get:Rule` は common に無いため。**継承を書き忘れると ViewModel の生成時点で落ちる**ので、
 * 静かに素通りすることはない。
 */
abstract class ViewModelTest {

    protected val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }
}
