package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import io.github.obaya884.rebuy.ui.screen.LocalBarOverlapTop
import io.github.obaya884.rebuy.ui.screen.LocalReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.NoAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarRenderer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

// 本番 iOS の構成でアプリを描く共有の入口。**画面を描く iosTest はここを通る**。
//
// 本番の iOS は外枠（ナビゲーションバー）を Compose では描かないので、**画面の同定と、
// 外枠からしか起こせない操作は、SwiftUI へ渡る内容（[ReBuyToolbar]）から起こす**。
//
// **ここを通すと、シートやダイアログも本番と同じ構成で描かれる**——外枠の差だけでなく、
// 上端が被さっている状態も一緒に付いてくる。

/**
 * 外枠が被さっている高さ。実機のバー高に近い値で、**値そのものに意味は無い**。
 *
 * **`internal` なのは、これが効いていること自体を測るため**（`IosTestAppIosTest`）——
 * 0 に戻っても、中身を見るだけのテストは全件緑で通る。
 */
internal val BAR_OVERLAP_TOP = 44.dp

/**
 * 本番 iOS の構成で [ReBuyApp] を描いて [block] を実行する。
 *
 * 端末の戻る（端スワイプ）の口は**常に仕込む**。入れても何も起きない（イベントを流したときだけ
 * 動く）ので、使うテストと使わないテストで形を分けない。流すのは [IosAppProbe.systemBack]。
 *
 * @param barOverlapTop 外枠が被さっている高さ。**0 にすると Android と同じ構成**になる
 * @param delegate バーを描く役。**Material にすると Android と同じ構成**になる
 */
@OptIn(ExperimentalTestApi::class)
internal fun runIosApp(
    prepare: FakeDatabase.() -> Unit = {},
    barOverlapTop: Dp = BAR_OVERLAP_TOP,
    delegate: ReBuyAppBarRenderer = NoAppBarRenderer,
    block: ComposeUiTest.(IosAppProbe) -> Unit,
) = runComposeUiTest {
    startTestKoin(prepare)
    val probe = IosAppProbe(this)
    setContent {
        InstallSystemBack(probe.systemBack)
        val renderer = remember(probe, delegate) {
            PublishingAppBarRenderer(delegate, probe.record)
        }
        CompositionLocalProvider(
            LocalReBuyAppBarRenderer provides renderer,
            LocalBarOverlapTop provides barOverlapTop,
        ) {
            ReBuyApp()
        }
    }
    waitForIdle()
    block(probe)
}

/**
 * 外枠の中身を見て、外枠からしか起こせない操作を起こす口。
 *
 * **本番と同じ経路**——`PublishingAppBarRenderer` が渡してきたものをそのまま使う。
 */
@OptIn(ExperimentalTestApi::class)
internal class IosAppProbe(private val test: ComposeUiTest) {

    /** 端末の戻る（端スワイプ）を流す口。`pressSystemBack` / `cancelSystemBack` に渡す。 */
    val systemBack = DirectNavigationEventInput()

    /** **最後に渡ったものだけを持つ。** 履歴を要る場面が無い（`ReBuyToolbarBridgeIosTest` が別に持つ）。 */
    private var latest: ReBuyToolbar? = null

    /** 描き手へ渡す記録口。**テストからは呼ばない**（見るのは [toolbar]）。 */
    internal val record: (ReBuyToolbar) -> Unit = { latest = it }

    /**
     * いま出ている外枠。**同期はここで畳む**——呼ぶ側の記憶に頼らない。
     *
     * **「いま描かれている画面」ではなく「最後に渡った内容」**。渡すのは内容が変わったときだけ
     * なので、画面が消えても後続が無ければ古い値が残る。**「留まったこと」を主張するなら、
     * ツリー側の 1 行を添えること**。
     */
    fun toolbar(): ReBuyToolbar {
        test.waitForIdle()
        return latest ?: fail("外枠の内容が 1 度も渡っていない")
    }

    /**
     * いま出ている画面。**見出しで同定する**——[ScreenTitle] が互いに異なることに依存している。
     */
    fun assertScreen(title: String) {
        assertEquals(title, toolbar().title, "出ている画面が違う")
    }

    /**
     * 外枠の ←。**「1 つ戻る」とは限らず、何をするかは画面が決める**（13 §6）。
     *
     * **落ちるときは `AssertionError`**——仕様違反として読めるようにする。`require` だと
     * 「テストの組み立てミス」に見える。
     */
    fun back() {
        val bar = toolbar()
        assertNotNull(bar.onBack, "${bar.title} に戻るが無い")()
        test.waitForIdle()
    }

    /** 外枠の押せるもの（01 の ＋ と ⚙）。 */
    fun tap(icon: ReBuyAppBarIcon) {
        val bar = toolbar()
        val action = bar.actions.firstOrNull { it.icon == icon }
            ?: fail("${bar.title} に $icon が無い")
        action.onClick()
        test.waitForIdle()
    }

    /**
     * 外枠に添えられた数（01 の「全 n 件」・04 の「x / n」）と、その印。
     *
     * **印まで見る**——iOS では `accessibilityIdentifier` が Swift から部品を同定する唯一の手段で、
     * 文字だけ見ると印を落としても緑になる。01 は印を持たない（`ReBuyAppBarState.Count` の条項）。
     */
    fun assertCount(text: String?, accessibilityId: String?) {
        val bar = toolbar()
        assertEquals(text, bar.countText, "添える数が違う")
        assertEquals(accessibilityId, bar.countAccessibilityId, "添える数の印が違う")
    }
}
