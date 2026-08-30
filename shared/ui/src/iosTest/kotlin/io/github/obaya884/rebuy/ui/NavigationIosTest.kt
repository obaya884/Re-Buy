package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.obaya884.rebuy.ui.di.initKoin
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.category_edit_title
import io.github.obaya884.rebuy.ui.resources.home_title
import io.github.obaya884.rebuy.ui.resources.item_edit_title
import io.github.obaya884.rebuy.ui.resources.setting_title
import io.github.obaya884.rebuy.ui.resources.shopping_title
import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test

/**
 * iOS 側の画面遷移の特性テスト。**Android の `NavigationTest` に対応する iOS の網**（T-42）。
 *
 * ### なぜ `commonTest` ではなく `iosTest` に置くのか
 *
 * `commonTest` に置くと Android 向けにもコンパイルされ、`testAndroidHostTest`（Android
 * フレームワークの無い素の JVM）で実行されて `android.os.Build.FINGERPRINT` が null で落ちる。
 * Compose の UI テストを Android で走らせるには Robolectric が要り、その有効化は
 * `@RunWith` という JUnit 4 のアノテーションなので `commonTest` には書けない。
 * Gradle 側で `testAndroidHostTest` から除外する手もあるが、**除外がソースから見えず、
 * クラス名の一致で効く**ので、名前を変えた瞬間に黙って外れる。ここに置けばその危険が無い。
 *
 * Android 側は `:androidApp` の instrumented が実物の `MainActivity` を起動して同じ経路を
 * 見ているので、網が 2 本になること自体は損ではない。
 *
 * ### Android 版との差
 *
 * **端末の戻るを踏む 6 件は持たない。** iOS にハードウェアの戻るが無いため。
 * 戻る経路は TopAppBar の戻る矢印とボトムナビだけを見る。
 * 「戻るとアプリが終了する」も同じ理由で iOS には対応する挙動が無い。
 *
 * ### 実物を起動しないこと
 *
 * `runComposeUiTest` は Kotlin/Native のテストバイナリの中で `ReBuyApp()` を直に描くので、
 * Swift 側の起動経路（`iOSApp.swift` → `setupKoin()` → `ReBuyViewController()`）と
 * `.app` への同梱は通らない。そこは [T-46] が持つ。
 *
 * ### この網が捕まえないもの
 *
 * **DB が空の状態しか見ていない。** Koin をテストが自分で起動するので本物の Room を
 * 読み書きするが、シードする手立てが無いため**品目が 1 件以上ないと描かれない経路
 * （`HomeListItemRow` など）には届かない**。Android の T-21 と同じ問題。
 *
 * **落とし穴 22 は再現しない。** T-42 の着手時に、`HomePagerTabList` の 2 か所を
 * `is HomeTab.All` に戻して変異させたが、テストバイナリでも**実物のアプリでも**
 * 落ちなかった（空状態が描かれていることは確認済みなので、その `when` は通っている）。
 * 3 か所目の `HomeListItemRow` は上のとおり品目が要るので確かめられていない。
 * **この網が落とし穴 22 を止める、とは言えない**——経緯と扱いは T-41。
 */
@OptIn(ExperimentalTestApi::class)
class NavigationIosTest {

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private val homeTitle = string(Res.string.home_title)
    private val shoppingTitle = string(Res.string.shopping_title)
    private val settingTitle = string(Res.string.setting_title)
    private val itemEditTitle = string(Res.string.item_edit_title)
    private val categoryEditTitle = string(Res.string.category_edit_title)

    /** ライセンス画面のタイトルと設定画面の行は実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"

    /**
     * Koin を起動して [ReBuyApp] を描き、[block] を実行する。
     *
     * `:androidApp` では実物の `ReBuyApplication` が Koin を起動しているが、
     * テストバイナリにはそれが無いのでここで立てる。
     *
     * **テストごとに止めない。** 止めると 2 件目以降が `ClosedScopeException` で落ちる——
     * ViewModel を作る `KoinViewModelFactory` が前のテストのスコープを掴んだまま
     * 持ち越されるため（実測。単独実行では通り、続けて走らせると落ちる）。
     * アプリと同じく**プロセスにつき 1 回だけ起動する**形にしている。
     */
    private fun app(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) initKoin()
        setContent { ReBuyApp() }
        block()
    }

    /** 現在表示されている画面を TopAppBar のタイトルで判定する。 */
    private fun ComposeUiTest.assertCurrentScreenIs(title: String) {
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    private fun ComposeUiTest.tapBackArrow() {
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()
    }

    private fun ComposeUiTest.tapTab(item: BottomNavigationItem) {
        onNodeWithTag(TestTags.bottomNavItem(item)).performClick()
    }

    @Test
    fun 起動直後はホームが表示される() = app {
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームから設定へ遷移して戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して戻る矢印で設定に帰る() = app {
        onNodeWithTag(TestTags.HOME_SETTINGS_BUTTON).performClick()
        onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun ホームからアイテム一覧へ遷移して戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_ITEM_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(itemEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ホームからカテゴリー一覧へ遷移して戻る矢印でホームに帰る() = app {
        onNodeWithTag(TestTags.HOME_CATEGORY_EDIT_BUTTON).performClick()
        assertCurrentScreenIs(categoryEditTitle)

        tapBackArrow()
        assertCurrentScreenIs(homeTitle)
    }

    @Test
    fun ボトムナビでホームと買い物を往復できる() = app {
        tapTab(BottomNavigationItem.Shopping)
        assertCurrentScreenIs(shoppingTitle)

        tapTab(BottomNavigationItem.Home)
        assertCurrentScreenIs(homeTitle)
    }
}
