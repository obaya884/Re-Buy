package io.github.obaya884.rebuy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.screen.license.ABOUT_LIBRARIES_PATH
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ライセンス一覧が「空でない」「Android に載る依存だけ」「全件にライセンスが付いている」ことを固定する。
 *
 * 一覧が空になっても**ビルドもテストも緑のまま**で、T-31 ステップ 5 から 14 までの
 * 9 ステップのあいだ、気づけるのは人が実機で見たときだけだった。
 *
 * 収集の壊れ方は 2 方向ある。**絞りすぎると 0 件**（Kotlin ターゲット名ではなく AGP の
 * バリアント名で `filterVariants` を書いた場合）、**絞らないと Android に無いものが混ざる**
 * （`skiko` や `ui-uikit` が `commonMain` のメタデータ解決から入る）。件数の下限は前者しか
 * 止められないので、後者は中身で見る。
 */
class LicenseLibrariesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** 設定画面のライセンス行とライセンス画面のタイトルは実装側もハードコード（T-40）。 */
    private val licenseLabel = "ライセンス"

    /**
     * 画面と同じパス・同じパーサでリソースを読む。パスは実装側の定数をそのまま使うので、
     * **`LicenseScreen` 側でパスを間違えるとここが例外で落ちる**。
     */
    private fun readLibraries(): List<Library> = runBlocking {
        Libs.Builder()
            .withJson(Res.readBytes(ABOUT_LIBRARIES_PATH).decodeToString())
            .build()
            .libraries
    }

    @Test
    fun ライセンス一覧のもとになるリソースが百件以上の依存を持っている() {
        val libraries = readLibraries()

        // 移設前（段 3 着手時点）が 133 件、いまが 134 件。依存の増減で上下するので下限だけを見る。
        // 厳密な件数にすると、依存を足すたびに数字を書き換えるだけの作業になって網でなくなる
        assertTrue("ライセンス一覧が ${libraries.size} 件しかない", libraries.size >= 100)
    }

    @Test
    fun ライセンス一覧がAndroidに載る依存だけを集めている() {
        val ids = readLibraries().map { it.uniqueId }

        // Android の classpath からしか出てこないもの。メタデータ側だけを拾っていたら入らない
        assertTrue("koin-android が無い", "io.insert-koin:koin-android" in ids)
        assertTrue("room-runtime が無い", "androidx.room:room-runtime" in ids)

        // 逆に、Android には載らないもの。絞りを外すと commonMain のメタデータ解決から入る
        val notOnAndroid = ids.filter { "skiko" in it || "uikit" in it }
        assertEquals("Android に載らないものが混ざっている", emptyList<String>(), notOnAndroid)
    }

    @Test
    fun ライセンス一覧のすべてにライセンスが付いている() {
        // 名前だけ出てライセンスが空でも画面は成立してしまうが、表示の目的を果たさない
        val missing = readLibraries().filter { it.licenses.isEmpty() }.map { it.uniqueId }

        assertEquals("ライセンスが空の依存がある", emptyList<String>(), missing)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun ライセンス画面に一覧が表示される() {
        // リソースが読めることと、それが画面に届いていることは別。ここは後者を見る。
        // Libs.Builder().build() が名前順に並べ替えるので、先頭はスクロールせずに見えるはず
        val firstLibrary = readLibraries().first().name

        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        // 遷移が壊れたのか一覧が出ないのかを、失敗した行で切り分けられるようにする
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(licenseLabel)

        // 読み込みは produceState の中で走るので、描かれるまで待つ
        composeRule.waitUntilAtLeastOneExists(hasText(firstLibrary), timeoutMillis = 10_000)
    }
}
