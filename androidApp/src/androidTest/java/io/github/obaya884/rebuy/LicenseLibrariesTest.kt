package io.github.obaya884.rebuy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.screen.license.ABOUT_LIBRARIES_PATH
import io.github.obaya884.rebuy.ui.screen.license.forCurrentPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ライセンス一覧が「空でない」「両プラットフォーム分を `targets` 付きで持つ」
 * 「画面に渡るのは Android に載る依存だけ」「全件にライセンスが付いている」ことを固定する。
 *
 * 一覧が空になっても**ビルドもテストも緑のまま**で、T-31 ステップ 5 から 14 までの
 * 9 ステップのあいだ、気づけるのは人が実機で見たときだけだった。
 *
 * 収集の壊れ方は 2 方向ある。**絞りすぎると 0 件**（存在しない構成名を名指しすると
 * 一致する構成が無く、黙って空になる）、**`targets` が付かないと絞りが素通しになり**
 * iOS 専用の `skiko` や `ui-uikit` が Android の一覧に出る。件数の下限は前者しか
 * 止められないので、後者は中身で見る。絞りの規則そのものは commonTest の
 * `PlatformLibrariesTest`、iOS 側の絞りは iosTest の `LicenseLibrariesIosTest` が持つ。
 *
 * 絞った結果の中身をデータ段では見ない——リソースの `targets`（このクラス）と絞りの規則
 * （commonTest）から導出できるため。画面段の「描かない」だけを持つ。
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
    private fun readLibs(): Libs = runBlocking {
        Libs.Builder()
            .withJson(Res.readBytes(ABOUT_LIBRARIES_PATH).decodeToString())
            .build()
    }

    private fun readLibraries(): List<Library> = readLibs().libraries

    @Test
    fun ライセンス一覧のもとになるリソースが百件以上の依存を持っている() {
        val libraries = readLibraries()

        // 移設前（段 3 着手時点）が 133 件、いまが 138 件（iOS 分を含む。T-39）。
        // 依存の増減で上下するので下限だけを見る。厳密な件数にすると、依存を足すたびに
        // 数字を書き換えるだけの作業になって網でなくなる
        assertTrue("ライセンス一覧が ${libraries.size} 件しかない", libraries.size >= 100)
    }

    @Test
    fun ライセンス一覧のもとになるリソースは両プラットフォーム分をターゲット付きで持っている() {
        val libraries = readLibraries()

        // targets 無しの entry は絞りを素通しする約束なので、収集側で 1 件でも
        // 付け損なうと「絞れているように見えて絞れていない」状態になる。**依存の全件に要る。**
        // 手足しの entry（同梱フォントなど。configPath 由来）には targets が付かないので、
        // その分だけを名指しで除く——**除外は接頭辞ではなく uniqueId で持つ**ことで、
        // 収集側の取りこぼしがここに紛れ込まないようにする
        val untargeted = libraries
            .filter { it.targets.isEmpty() }
            .map { it.uniqueId }
            .filterNot { it in MANUAL_ENTRY_IDS }
        assertEquals("targets が付いていない依存がある", emptyList<String>(), untargeted)

        // 手足しの entry そのものは消えていないこと（消えるとライセンス表示の義務を欠く）
        val manual = libraries.map { it.uniqueId }.filter { it in MANUAL_ENTRY_IDS }
        assertEquals("手足しした entry が一覧に無い", MANUAL_ENTRY_IDS, manual.toSet())

        // iOS 分が丸ごと落ちても総数の下限（100 件）は Android 分だけで満たしてしまうので、
        // iOS のターゲットを持つ件数にも下限を置く（実測 72 件）
        val iosCount = libraries.count { library -> library.targets.any { it.startsWith("ios") } }
        assertTrue("iOS のターゲットを持つ依存が $iosCount 件しかない", iosCount >= 50)

        // Android の classpath からしか出てこないもの
        val byId = libraries.associateBy { it.uniqueId }
        assertEquals(
            "koin-android が無いか、targets が違う",
            setOf("android"),
            byId["io.insert-koin:koin-android"]?.targets
        )
        // iOS の klib 構成からしか出てこないもの。名前照合の既定収集に戻ると消える
        assertTrue(
            "skiko が iOS のターゲット付きで入っていない",
            byId["org.jetbrains.skiko:skiko"]?.targets.orEmpty().any { it.startsWith("ios") }
        )
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
        // 画面はプラットフォームで絞った一覧を出すので、先頭も絞った側から取る。
        // Libs.Builder().build() が名前順に並べ替えるので、先頭はスクロールせずに見えるはず
        val firstLibrary = readLibs().forCurrentPlatform().libraries.first().name

        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        // 遷移が壊れたのか一覧が出ないのかを、失敗した行で切り分けられるようにする
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(licenseLabel)

        // 読み込みは produceState の中で走るので、描かれるまで待つ
        composeRule.waitUntilAtLeastOneExists(hasText(firstLibrary), timeoutMillis = 10_000)
    }

    /**
     * 画面が `forCurrentPlatform` を通していること。データ段のテストは自分で絞りを呼ぶので、
     * **`LicenseScreen` から絞りが外れる退行はここでしか捕まらない**。
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun ライセンス画面はAndroidに載らない依存を描かない() {
        val filtered = readLibs().forCurrentPlatform().libraries
        // iOS 専用の entry（targets はあるが android が無い）。表示名で画面と突き合わせる
        val iosOnlyName = readLibraries()
            .first { library -> library.targets.isNotEmpty() && library.targets.none { it.startsWith("android") } }
            .name

        composeRule.onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(filtered.first().name), timeoutMillis = 10_000)

        // 対照: 絞り後の末尾の行へはスクロールで到達できる。スクロールの手段が生きていることを
        // 先に確かめ、次の「到達できない」が手段の死による偽陽性でないようにする
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(filtered.last().name))

        // 本題: iOS 専用の行はどこまでスクロールしても現れない。
        // LicenseScreen から forCurrentPlatform が外れると、ここで行が見つかって失敗しなくなる
        assertThrows(AssertionError::class.java) {
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(iosOnlyName))
        }
    }

    private companion object {
        /** `shared/ui/aboutlibraries/libraries/` に手で置いた entry。依存として解決されない。 */
        val MANUAL_ENTRY_IDS = setOf("fonts:zen-maru-gothic")
    }
}
