package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.pool_total_count
import io.github.obaya884.rebuy.ui.resources.shopping_progress
import io.github.obaya884.rebuy.ui.screen.NoAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarState
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.koin.mp.KoinPlatformTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * **iOS の本番の経路**——バーを Compose が描かず、内容を Swift へ渡す形（画面定義書 §6）。
 *
 * `AppBarStateIosTest` が見るのは「画面ごとにバーへ何を出すか」で、描き手は Material のまま。
 * **ここはその 1 段外側**で、`ReBuyIosApp` が挿す「描かない描き手」と購読口そのものを見る。
 * 段 4 の Step 4 で `ContentView` がこの入口に繋がるまで、**この経路を通るのはここだけ**。
 *
 * **`commonTest` には置けない**（素の JVM で実行されて落ちる。テスト戦略定義書 §1）。
 */
@OptIn(ExperimentalTestApi::class)
class ReBuyToolbarBridgeIosTest {

    /**
     * 画面定義書 §5 の accent（パレット別・明暗別）。
     *
     * **明暗はシミュレータの設定で決まる**のでペアで持ち、パレットの対応だけを固定する。
     * 1 つの集合にまとめると、**パレットどうしで色を入れ替える変異**が素通りする。
     */
    private val accentsInSpec = mapOf(
        ThemePalette.WAKABA to setOf(0xFF2E6B4AL, 0xFF74BD93L),
        ThemePalette.AI to setOf(0xFF34558BL, 0xFF8FB0E3L),
        ThemePalette.KAKI to setOf(0xFFB5541FL, 0xFFDC9660L),
    )

    private val themeRepository: ThemeRepository
        get() = KoinPlatformTools.defaultContext().get().get()

    /** 本番と同じ形で [ReBuyIosApp] を描き、渡ってきたものを順に控える。 */
    private fun iosApp(
        prepare: FakeDatabase.() -> Unit = {},
        block: ComposeUiTest.(List<ReBuyToolbar>) -> Unit
    ) = runComposeUiTest {
        val published = mutableListOf<ReBuyToolbar>()
        startTestKoin(prepare)
        setContent { ReBuyIosApp { published += it } }
        waitForIdle()
        block(published)
    }

    /** いま Swift が持っているはずのもの。**同期はここで畳む**——呼ぶ側の記憶に頼らない。 */
    private fun ComposeUiTest.currentToolbar(published: List<ReBuyToolbar>): ReBuyToolbar {
        waitForIdle()
        return published.lastOrNull() ?: error("ツールバーが 1 度も渡っていない")
    }

    private fun ReBuyToolbar.action(icon: ReBuyAppBarIcon): ReBuyToolbarAction =
        actions.firstOrNull { it.icon == icon } ?: error("$title に $icon が無い")

    /**
     * **バーのノードが 1 つも出ないのに、画面の中身は出る。**
     *
     * ここが崩れると Step 4 で二重に描かれる（[NoAppBarRenderer]）。`AppBarStateIosTest` の
     * 記録用の描き手は Material を描き続けるので、**この経路は既存の網を 1 度も通らない**。
     *
     * **印（`accessibilityIdentifier`）もここで固定する**——Swift 側が画面と部品を同定する
     * 唯一の手段で、Step 4 で Compose の `testTag` が消えたあとは代わりが無い。
     */
    @Test
    fun バーは描かれず中身と内容だけが残る() = iosApp(oneItem(ItemStatus.NO_DEAL)) { published ->
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertDoesNotExist()
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).assertDoesNotExist()
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).assertDoesNotExist()

        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertExists()
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).assertExists()

        val toolbar = currentToolbar(published)
        assertEquals(ScreenTitle.pool, toolbar.title)
        assertEquals(string(Res.string.pool_total_count, 1), toolbar.countText)
        // 01 の「全 n 件」は印を持たない（`ReBuyAppBarState.Count` の条項）
        assertNull(toolbar.countAccessibilityId, "01 に無いはずの印が渡っている")
        assertEquals(
            listOf(ReBuyAppBarIcon.ADD, ReBuyAppBarIcon.SETTINGS),
            toolbar.actions.map { it.icon }
        )
        assertEquals(
            listOf(TestTags.POOL_ADD_BUTTON, TestTags.POOL_SETTINGS_BUTTON),
            toolbar.actions.map { it.accessibilityId }
        )
        assertNull(toolbar.onBack, "根なのに戻るが渡っている")
    }

    /**
     * 渡した動作が**本番の配線に繋がっている**こと（末尾のアクション）。
     *
     * 07 へはバーからしか行けないので、**この経路が切れると設定に二度と入れない**。
     * Step 6 で既存の 6 ファイルが画面の同定手段を失うとき、**受け皿になるのがこの形**。
     */
    @Test
    fun 渡した歯車で設定へ行ける() = iosApp { published ->
        currentToolbar(published).action(ReBuyAppBarIcon.SETTINGS).onClick()

        val toolbar = currentToolbar(published)
        assertEquals(ScreenTitle.setting, toolbar.title)
        assertNotNull(toolbar.onBack, "設定に戻るが無い")
        // ← も SwiftUI が描く。Compose 側にも出ると Step 4 で 2 つ並ぶ
        onNodeWithTag(TestTags.BACK_BUTTON).assertDoesNotExist()
    }

    /**
     * 先頭のアクションも正しい動作に繋がっていること。
     *
     * **[渡した歯車で設定へ行ける] だけでは添字を末尾に固定する実装が素通りする**（押している
     * ⚙ がたまたま最後の要素のため）。02 もバーからしか開けないので、受け皿はここしかない。
     */
    @Test
    fun 渡した先頭のアクションで登録シートが開く() = iosApp { published ->
        currentToolbar(published).action(ReBuyAppBarIcon.ADD).onClick()

        waitForIdle()
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertExists()
    }

    /**
     * **04 の戻るは離脱確認を通る**（画面定義書 §6）。
     *
     * バーの ← は「1 つ戻る」ではなく画面が決めた動作を呼ぶ。素通しにすると、
     * **店で手を動かしている最中に黙って 01 へ戻る**。
     */
    @Test
    fun 買い物中の戻るは離脱確認を出す() = iosApp(oneItem(ItemStatus.IN_SHOPPING_LIST)) { published ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()

        val toolbar = currentToolbar(published)
        assertEquals(ScreenTitle.shoppingAll, toolbar.title)
        requireNotNull(toolbar.onBack).invoke()

        waitForIdle()
        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertExists()
    }

    /**
     * **添える数は、動いたら渡し直される。**
     *
     * 動作（`onBack`・`actions`）と違って、**数は渡した時点の文字列が焼き込まれる**。鍵に
     * 入っていないと渡し直す道が無く、**買い物のあいだ SwiftUI のバーが「0 / 2」のまま凍る**。
     */
    @Test
    fun 買い物の進捗は渡し直される() = iosApp(twoInBasket) { published ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()

        val started = currentToolbar(published)
        assertEquals(string(Res.string.shopping_progress, 0, 2), started.countText)
        assertEquals(TestTags.SHOPPING_PROGRESS, started.countAccessibilityId)

        onNodeWithTag(TestTags.shoppingRow(itemId = 1)).performClick()

        assertEquals(string(Res.string.shopping_progress, 1, 2), currentToolbar(published).countText)
    }

    /**
     * 遷移のあと、**渡っているのが遷移先のバー**であること。**回数も見る**——遷移中は遷移元と
     * 遷移先が同時に composition にいるので、内容だけを見ると往復して落ち着いた場合に気づけない。
     *
     * 画面ごとの積み上げ（LIFO）を入れない判断を、この網が守る（経緯は log_23 2026-09-12）。
     *
     * **タイトル以外の要因で列が伸びたら、緩めてよい**——01 や 07 のバーに添える数が付く、
     * 起動時に settle が 1 回挟まる、などは退行ではない。そのときは「同じ見出しが連続しない
     * ことと、末尾が遷移先であること」に置き換える。
     */
    @Test
    fun 往復しても渡るのは遷移先のバー() = iosApp { published ->
        currentToolbar(published).action(ReBuyAppBarIcon.SETTINGS).onClick()
        assertEquals(ScreenTitle.setting, currentToolbar(published).title, "進んだ先が渡っていない")

        requireNotNull(currentToolbar(published).onBack).invoke()
        assertEquals(ScreenTitle.pool, currentToolbar(published).title, "戻った先が渡っていない")

        assertEquals(
            listOf(ScreenTitle.pool, ScreenTitle.setting, ScreenTitle.pool),
            published.map { it.title },
            "遷移のたびに 1 回だけ渡っていない"
        )
    }

    /**
     * **内容が同じなら渡し直さない。**
     *
     * [ReBuyToolbarKey] が無い実装（[ReBuyAppBarState] を直に比べる形）は、ラムダで `equals` が
     * 壊れているので再コンポーズのたびに渡し、SwiftUI のツールバーが毎フレーム作り直される。
     *
     * **アプリ全体を描いて測ってはいけない**——6 画面のバーのラムダは捕捉が安定していて
     * Compose がメモ化するため、`equals` が成立して `Render` ごと skip される。
     * **素朴な実装でも緑になる**ので、ここは描き手を直接叩く。
     */
    @Test
    fun 再コンポーズしても内容が同じなら渡し直さない() = barOnly { harness, published ->
        val before = harness.renders
        harness.tick++
        waitForIdle()

        assertTrue(harness.renders > before, "描き手が再実行されていない（測れていない）")
        assertEquals(1, published.size, "内容が同じなのに渡し直している")
    }

    /**
     * **内容が変われば渡す。** 上のテストだけだと「1 度も渡さない」実装が緑になる
     * （テスト戦略定義書 §2.2）。
     */
    @Test
    fun 内容が変われば渡し直す() = barOnly { harness, published ->
        harness.title = ScreenTitle.setting
        waitForIdle()

        assertEquals(2, published.size, "内容が変わったのに渡っていない")
        assertEquals(ScreenTitle.setting, published.last().title)
    }

    /**
     * **鍵が同じでも、渡したクロージャは最新の動作を呼ぶ。**
     *
     * 渡した時点のラムダを抱えると、Swift が古い実装を持ち続ける——04 の ← が
     * 古い画面の離脱確認を呼ぶ、という黙った不具合になる。
     */
    @Test
    fun 渡したあとに動作が差し替わっても最新を呼ぶ() = barOnly { harness, published ->
        harness.tick = 7
        waitForIdle()

        requireNotNull(published.single().onBack).invoke()

        assertEquals(7, harness.lastBack, "渡した時点の動作を抱えている")
    }

    /**
     * **押せるものも同じ**（[渡したあとに動作が差し替わっても最新を呼ぶ] の `actions` 版）。
     *
     * 戻るだけを見ていると、`actions` の側に publish 時点のラムダを焼き込む実装が素通りする。
     */
    @Test
    fun 渡したあとに押せるものが差し替わっても最新を呼ぶ() = barOnly { harness, published ->
        harness.tick = 7
        waitForIdle()

        published.single().actions.single().onClick()

        assertEquals(7, harness.lastAction, "渡した時点の動作を抱えている")
    }

    /**
     * **もう存在しないボタンを押しても落ちない。**
     *
     * Swift は最後に渡ったツールバーを持ち続けるので、**アクションが減った直後のタップ**は
     * 現実に起こる（渡し直しとタップの間の 1 フレーム）。範囲外を防いでいるのは `getOrNull` で、
     * ここが添字直参照に戻ると落ちる。
     */
    @Test
    fun 消えたボタンを押しても何も起きない() = barOnly { harness, published ->
        val stale = published.single()
        harness.actionCount = 0
        waitForIdle()

        assertEquals(0, currentToolbar(published).actions.size, "渡し直されていない")
        stale.actions.single().onClick()

        assertNull(harness.lastAction, "もう無いボタンの動作が走った")
    }

    /**
     * **accent はテーマに追従する**（画面定義書 §5・§6）。
     *
     * 3 パレットで値が異なることを見ないと、固定値を返す実装も、**鍵から accent を落として
     * 渡し直さなくなった実装**も全件緑で通る。**パレットごとに照合する**ので、色の対応を
     * 入れ替える変異も落ちる。`ReBuyApp` の `ReBuyTheme(palette = …)` の結線を端から端まで
     * 通しているのはこのテストだけ。
     */
    @Test
    fun accentは3パレットで異なり仕様の値になる() = iosApp { published ->
        assertEquals(ThemePalette.DEFAULT, themeRepository.palette.value, "既定から始まっていない")

        val byPalette = ThemePalette.entries.associateWith { palette ->
            themeRepository.select(palette)
            currentToolbar(published).accentArgb
        }

        // 下の照合に含意されるが、**いちばん起きやすい壊れ方を名指しする**ために先に置く
        // （鍵から accent が落ちると 3 つとも同じ値になり、ここが的確なメッセージで落ちる）
        assertEquals(
            ThemePalette.entries.size,
            byPalette.values.toSet().size,
            "パレットを変えても色が変わっていない: $byPalette"
        )
        byPalette.forEach { (palette, argb) ->
            assertTrue(
                argb in accentsInSpec.getValue(palette),
                "$palette の accent が §5 の値でない: $argb"
            )
        }
    }

    /** 描き手を直接叩く台。**アプリ全体では測れないもの**だけがここを使う。 */
    private class BarHarness {
        /** 再コンポーズを起こす。**バーの見た目は変わらない**ので鍵は動かない。 */
        var tick by mutableStateOf(0)
        var title by mutableStateOf(ScreenTitle.pool)

        /** 押せるものの個数。**0 にすると、渡した後に消えたボタンを押す経路**へ入れる。 */
        var actionCount by mutableStateOf(1)

        /**
         * `Render` が実際に再実行された回数。**再コンポーズが起きたことを自分で主張する**ために数える。
         *
         * **数えるのは描き手の中**（[CountingRenderer]）。台の側で数えると、引数が安定化して
         * `Render` ごと skip されたときに**増えたまま空虚になる**——`AppBarStateIosTest` の
         * KDoc が記録している現象がそれ。
         */
        var renders = 0
            private set

        /** 最後に走った戻る・押せるものが読んだ [tick]。 */
        var lastBack: Int? = null
        var lastAction: Int? = null

        fun countRender() {
            renders++
        }
    }

    /** [BarHarness.renders] を数えてから「描かない」に委ねる。 */
    private class CountingRenderer(private val harness: BarHarness) : ReBuyAppBarRenderer {
        @Composable
        override fun Render(appBar: ReBuyAppBarState) {
            NoAppBarRenderer.Render(appBar)
            SideEffect { harness.countRender() }
        }
    }

    /**
     * 描き手 1 つだけを描く。
     *
     * **Koin を引かない**（`ReBuyTheme` のパレットは既定引数、`NoAppBarRenderer` は何も引かない）ので
     * `startTestKoin` を呼んでいない。**ここで画面や `ReBuyApp` を描くようになったら呼ぶこと**——
     * 呼ばないと直前のテストが選んだテーマを黙って引き継ぐ。
     */
    private fun barOnly(block: ComposeUiTest.(BarHarness, List<ReBuyToolbar>) -> Unit) =
        runComposeUiTest {
            val harness = BarHarness()
            val published = mutableListOf<ReBuyToolbar>()
            val renderer = PublishingAppBarRenderer(CountingRenderer(harness)) { published += it }
            setContent {
                ReBuyTheme {
                    val tick = harness.tick
                    renderer.Render(
                        ReBuyAppBarState(
                            title = harness.title,
                            // ラムダは毎コンポーズ作り直される（tick を捕捉しているため）。
                            // **本番の画面のラムダもこうなりうる**のがこの網の動機
                            onBack = { harness.lastBack = tick },
                            actions = List(harness.actionCount) { index ->
                                ReBuyAppBarState.Action(
                                    icon = ReBuyAppBarIcon.ADD,
                                    testTag = "action_$index",
                                    onClick = { harness.lastAction = tick },
                                )
                            },
                        )
                    )
                }
            }
            waitForIdle()
            block(harness, published)
        }
}
