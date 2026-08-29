package io.github.obaya884.rebuy.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `Dispatchers.Main` をテスト用のディスパッチャに差し替える。
 *
 * `viewModelScope` は `Dispatchers.Main` に紐づいており、JVM 段には Android の
 * メインルーパーが無いため、差し替えないと ViewModel を作った時点で落ちる。
 *
 * `StandardTestDispatcher` なので、コルーチンは `runTest` の中で
 * `advanceUntilIdle()` などを呼ぶまで走らない。「まだ走っていない状態」を
 * 意図的に観測できるようにするための選択。
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
