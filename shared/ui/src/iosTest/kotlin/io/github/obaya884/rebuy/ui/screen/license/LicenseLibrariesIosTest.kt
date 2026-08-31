package io.github.obaya884.rebuy.ui.screen.license

import com.mikepenz.aboutlibraries.Libs
import io.github.obaya884.rebuy.ui.resources.Res
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * iOS のライセンス一覧の中身を固定する（T-39）。
 *
 * 一覧の読み込みは非同期なので、`NavigationIosTest` は画面のタイトルまでしか見ていない。
 * ここでは画面と同じパス・同じパーサ・同じ絞り（[forCurrentPlatform]）でデータ側を見る。
 * Android 側の対は instrumented の `LicenseLibrariesTest`、絞りの規則そのものは
 * commonTest の `PlatformLibrariesTest`。
 */
class LicenseLibrariesIosTest {

    /** パスは実装側の定数をそのまま使う。`LicenseScreen` 側でパスを間違えるとここが落ちる。 */
    private fun readLibs(): Libs = runBlocking {
        Libs.Builder()
            .withJson(Res.readBytes(ABOUT_LIBRARIES_PATH).decodeToString())
            .build()
    }

    @Test
    fun iOSの画面に渡る一覧はiOSに載る依存だけになる() {
        val ids = readLibs().forCurrentPlatform().libraries.map { it.uniqueId }

        // iOS の一覧の大半は両 OS に載る依存（68 件）。iOS 専用の数件だけに縮む壊れ方
        // （両 OS entry から ios のターゲットだけが落ちる）は、存在 1 件と量の下限で止める
        assertTrue("androidx.room:room-runtime" in ids, "room-runtime（両 OS に載る依存）が無い")
        assertTrue(ids.size >= 50, "絞った結果が ${ids.size} 件しかない")

        // iOS の klib 構成からしか出てこないもの。収集が既定の名前照合
        // （klib 構成を拾えない）に戻ると、この 2 件が真っ先に消える
        assertTrue("org.jetbrains.skiko:skiko" in ids, "skiko が無い")
        assertTrue("org.jetbrains.compose.ui:ui-uikit" in ids, "ui-uikit が無い")

        // Android 専用のもの。絞りが素通しになると混ざる
        assertFalse("io.insert-koin:koin-android" in ids, "koin-android が混ざっている")
    }

    @Test
    fun iOSの一覧の全件にライセンスが付いている() {
        // 同じ json の全件は instrumented 側も見ているが、こちらは GMD を要さず
        // iOS の CI 段だけでも回る
        val missing = readLibs().forCurrentPlatform().libraries
            .filter { it.licenses.isEmpty() }
            .map { it.uniqueId }

        assertEquals(emptyList(), missing, "ライセンスが空の依存がある")
    }
}
