package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.pool_total_count
import io.github.obaya884.rebuy.ui.resources.shopping_progress
import io.github.obaya884.rebuy.ui.screen.LocalReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.MaterialAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 画面ごとに**バーへ何を出すか**（[ReBuyAppBarState]）を見る。
 *
 * **他の網より 1 段下を見ている。** 既存の `iosTest` は `TOP_APP_BAR_TITLE` を引いて「描かれた文字」で
 * 画面を同定しているが、段 4 でバーが SwiftUI へ移ると**そのタグは Compose ツリーから消える**。
 * ここはデータを直接見るので、描き手が入れ替わっても検証がそのまま残る。**Swift が受け取る契約は
 * この形**（画面定義書 §6）。
 *
 * **`commonTest` には置けない**（素の JVM で実行されて落ちる。テスト戦略定義書 §1）。
 *
 * 押したときに何が起きるかは原則見ない——`onClick` はラムダで値として比べられず、押した結果は
 * `NavigationIosTest`・`ShoppingIosTest` と instrumented がすでに見ている。**例外は「バーからしか
 * 押せないもの」だけ**（04 の ←、01 の ＋ と ⚙）。理由は各テストの KDoc にある。
 */
@OptIn(ExperimentalTestApi::class)
class AppBarStateIosTest {

    /**
     * バーを**描いたうえで**、渡された状態を控える描き手。
     *
     * **記録だけにしてはいけない**——バーが消えると ⚙ や ← が無くなり、画面を移れなくなる。
     * 控えるのは `SideEffect`（コンポジションが成立してから走る）で、描画の途中の値を拾わない。
     */
    private class RecordingAppBarRenderer : ReBuyAppBarRenderer {
        var last: ReBuyAppBarState? = null
            private set

        @Composable
        override fun Render(appBar: ReBuyAppBarState) {
            MaterialAppBarRenderer.Render(appBar)
            SideEffect { last = appBar }
        }

        fun reset() {
            last = null
        }
    }

    /** [ReBuyApp] を描いて [block] を実行する。バーの描き手だけを控える版に差し替える。 */
    private fun app(
        prepare: FakeDatabase.() -> Unit = {},
        block: ComposeUiTest.(RecordingAppBarRenderer) -> Unit
    ) = runComposeUiTest {
        val recorder = RecordingAppBarRenderer()
        startTestKoin(prepare)
        setContent {
            CompositionLocalProvider(LocalReBuyAppBarRenderer provides recorder) {
                ReBuyApp()
            }
        }
        block(recorder)
    }

    /** いま出ているバー。**同期はここで畳む**——呼ぶ側の記憶に頼らない。 */
    private fun ComposeUiTest.currentAppBar(recorder: RecordingAppBarRenderer): ReBuyAppBarState {
        waitForIdle()
        return requireNotNull(recorder.last) { "アプリバーが 1 度も描かれていない" }
    }

    /**
     * [action] のあとに**描き直された**バーを返す。
     *
     * **控えを先に消す。** `last` は上書きしかしないので、消さないと「操作が効かなくても前の
     * バーが返って緑」になりうる。消しておけば「操作のあとにバーが 1 度は描かれた」ことを毎回主張する。
     *
     * **待ちは `waitForIdle` で、「最初に描かれたら止める」形にはしない**——遷移の直後は settle 前の
     * フレームが挟まり、**1 つ前の画面のバーを拾う**（実測。戻る → 別の画面へ進む順路で「設定」を掴んだ）。
     * **返るのが遷移先のものであることは、各テストが期待するタイトルを名指しで assert していることが
     * 担保する。**
     *
     * **画面が変わらない操作にはこれを使わない。** バーは**中身が変わったときだけ**描き直されるので
     * （下の [プールの総数は絞り込んでも変わらない] を見よ）、変わらないことを主張したい場面で
     * 描き直しを要求すると、正しい実装のほうが落ちる。
     */
    private fun ComposeUiTest.appBarAfter(
        recorder: RecordingAppBarRenderer,
        action: ComposeUiTest.() -> Unit
    ): ReBuyAppBarState {
        recorder.reset()
        action()
        return currentAppBar(recorder)
    }

    /** 戻るだけを持つ画面（07・08・09 の 2 枚・ライセンス）の共通の形。 */
    private fun assertBackOnly(appBar: ReBuyAppBarState, title: String) {
        assertEquals(title, appBar.title)
        assertNotNull(appBar.onBack, "$title に戻るが無い")
        assertNull(appBar.count, "$title に添える数が出ている")
        assertEquals(emptyList(), appBar.actions, "$title にアクションが出ている")
    }

    /** 01 は根なので戻るを持たず、総数と ＋ と ⚙ を出す（画面 01）。 */
    @Test
    fun プールのバーは総数と2つのアクションを出す() = app(oneItem(ItemStatus.NO_DEAL)) { recorder ->
        val appBar = currentAppBar(recorder)

        assertEquals(ScreenTitle.pool, appBar.title)
        assertNull(appBar.onBack, "根なのに戻るが出ている")
        val count = requireNotNull(appBar.count) { "01 に総数が出ていない" }
        assertEquals(string(Res.string.pool_total_count, 1), count.text)
        // 01 の総数に印は付けない（`ReBuyAppBarState.Count` の KDoc）。意図として固定する
        assertNull(count.testTag)
        assertEquals(
            listOf(ReBuyAppBarIcon.ADD, ReBuyAppBarIcon.SETTINGS),
            appBar.actions.map { it.icon }
        )
        assertEquals(
            listOf(TestTags.POOL_ADD_BUTTON, TestTags.POOL_SETTINGS_BUTTON),
            appBar.actions.map { it.testTag }
        )
    }

    /**
     * 01 の ＋ と ⚙ が何に繋がっているか。
     *
     * **02（登録シート）と 07（設定）へはバーからしか行けない**ので、ここが本番で通る唯一の経路。
     * 04 の ← と同じ理由で、この 2 つだけは押した結果まで見る——いまこれを守っているのは
     * `POOL_ADD_BUTTON` / `POOL_SETTINGS_BUTTON` を押す網＝ Material の描き手を通る経路だけで、
     * **段 4 で消える**。
     */
    @Test
    fun プールのアクションは登録シートと設定へ繋がる() = app { recorder ->
        val actions = currentAppBar(recorder).actions.associateBy { it.icon }

        requireNotNull(actions[ReBuyAppBarIcon.ADD]).onClick()
        waitUntil { onAllNodesWithTag(TestTags.REGISTER_NAME_FIELD).fetchSemanticsNodes().isNotEmpty() }

        // シートを閉じてから ⚙ へ。**開いたままだと ⚙ がスクリムの下**で押せない
        onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTouchInput { swipeDown() }

        val setting = appBarAfter(recorder) {
            requireNotNull(actions[ReBuyAppBarIcon.SETTINGS]).onClick()
        }

        assertEquals(ScreenTitle.setting, setting.title)
    }

    /**
     * 01 の総数は**絞り込んでも変わらない**（画面 01）。
     *
     * `PoolViewModelTest` が同じ条項を見ているが、あちらは状態までで、**画面が `totalCount` と
     * `visibleItems.size` のどちらを繋いだか**は通らない。1 件だけの種では両者が同じ数になるので、
     * **絞り込みで数が変わる 2 件**を置く。
     *
     * **[appBarAfter] は使えない。** バーは**中身が変わったときだけ**描き直され、絞り込んでも総数が
     * 変わらない正しい実装では 1 秒待っても描かれない（実測。`ReBuyAppBarState` の持つラムダは
     * Compose が memoize するので、内容が同じなら `equals` が成立して skip される）。
     *
     * **それでもこのテストは空虚にならない。** 総数を `visibleItems.size` に繋ぐ変異では絞り込みで
     * 中身が変わるため描き直され、控えが「全 1 件」に更新されて落ちる（実測。落ちるのはこの 1 件だけ）。
     */
    @Test
    fun プールの総数は絞り込んでも変わらない() = app({
        seed(
            items = listOf(item(1, destinationId = 1), item(2)),
            destinations = listOf(destination(1))
        )
    }) { recorder ->
        val all = string(Res.string.pool_total_count, 2)
        assertEquals(all, currentAppBar(recorder).count?.text)

        onNodeWithTag(TestTags.POOL_CHIP_ANYWHERE).performClick()
        // **絞り込みが効いたことも確かめる。** これが無いと、チップが繋がっていなくても
        // 「総数が動かない」は成立してしまう
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertDoesNotExist()

        assertEquals(all, currentAppBar(recorder).count?.text, "絞り込みで総数が動いた")
    }

    /**
     * 04 は進捗と印を出し、戻るを持つ（画面 04）。
     *
     * **x と n を別の数にする**——「0 / 1」だと、進捗の第 1 引数を 0 に固定する変異も、
     * 分母を 1 に固定する変異も素通りする。
     */
    @Test
    fun 買い物のバーは進捗と印を出して戻るを持つ() = app(twoInBasket) { recorder ->
        val appBar = appBarAfter(recorder) {
            onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        }

        assertEquals(ScreenTitle.shoppingAll, appBar.title)
        assertNotNull(appBar.onBack, "04 に戻るが無い")
        assertEquals(string(Res.string.shopping_progress, 0, 2), appBar.count?.text)
        assertEquals(TestTags.SHOPPING_PROGRESS, appBar.count?.testTag)
        assertEquals(emptyList(), appBar.actions, "04 にアクションが出ている")

        val checked = appBarAfter(recorder) {
            onNodeWithTag(TestTags.shoppingRow(itemId = 1)).performClick()
        }

        assertEquals(string(Res.string.shopping_progress, 1, 2), checked.count?.text)
    }

    /**
     * 行き先を選んで入った 04 のタイトルは「◯◯で買い物中」（画面 04）。
     *
     * **全件モードと分岐する側**なので、[買い物のバーは進捗と印を出して戻るを持つ] とは別に通す。
     */
    @Test
    fun 行き先を選んだ買い物のバーは行き先名を出す() = app({
        seed(
            items = listOf(item(1, status = ItemStatus.IN_SHOPPING_LIST, destinationId = 1)),
            destinations = listOf(destination(1))
        )
    }) { recorder ->
        val appBar = appBarAfter(recorder) {
            onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
            onNodeWithTag(TestTags.shoppingStartRow(destinationId = 1)).performClick()
        }

        assertEquals(ScreenTitle.shopping("行き先1"), appBar.title)
    }

    /**
     * **04 の ← は「1 つ戻る」ではなく離脱確認を呼ぶ**（画面定義書 §6）。
     *
     * ここだけ押した結果まで見る。§6 が名指しで守れと書いている条項で、いまこれを守っているのは
     * `BACK_BUTTON` を押す網＝ Material の描き手を通る経路だけであり、**段 4 で消える**ため。
     * ラムダの比較ではなく**呼んで副作用を観測する**ので、描き手が入れ替わっても残る。
     *
     * 同じ理由が 01 の ＋ と ⚙ にも当たる（[プールのアクションは登録シートと設定へ繋がる]）。
     */
    @Test
    fun 買い物のバーの戻るは離脱確認へ繋がる() = app(twoInBasket) { recorder ->
        val appBar = appBarAfter(recorder) {
            onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        }

        requireNotNull(appBar.onBack).invoke()
        waitForIdle()

        onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertExists()
    }

    /**
     * 07・08・09 の 2 枚・ライセンスはどれも戻るだけを持つ。
     *
     * **5 画面を 1 件で歩く**——同じ形の主張が並ぶだけなので、落ちた画面は assert のメッセージが
     * 名指しする。**07 の行はタグで掴む**——行の文言は遷移先のタイトルと同じ語なので、
     * テキストで掴むと多重ヒットする（`TestTags` の KDoc）。
     */
    @Test
    fun 設定から辿れる画面のバーは戻るだけを持つ() = app { recorder ->
        assertBackOnly(
            appBarAfter(recorder) { onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick() },
            ScreenTitle.setting
        )

        assertBackOnly(
            appBarAfter(recorder) { onNodeWithTag(TestTags.SETTING_ROW_THEME).performClick() },
            ScreenTitle.theme
        )
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()

        assertBackOnly(
            appBarAfter(recorder) {
                onNodeWithTag(TestTags.SETTING_ROW_CATEGORY_EDIT).performClick()
            },
            ScreenTitle.categoryManage
        )
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()

        assertBackOnly(
            appBarAfter(recorder) {
                onNodeWithTag(TestTags.SETTING_ROW_DESTINATION_MANAGE).performClick()
            },
            ScreenTitle.destinationManage
        )
        onNodeWithTag(TestTags.BACK_BUTTON).performClick()

        assertBackOnly(
            appBarAfter(recorder) { onNodeWithTag(TestTags.SETTING_ROW_LICENSE).performClick() },
            ScreenTitle.license
        )
    }
}
