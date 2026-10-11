package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 画面遷移の特性テスト。**遷移の仕様そのものを変えない限り書き換えない**——
 * ナビゲーション基盤を差し替えたときに挙動が変わっていないことを、ここで機械的に確かめる。
 *
 * 遷移規則そのもの（スタックの積み方・タブごとの履歴保持）は JVM 段の `NavigatorTest` が持つ。
 * ここが見るのは「UI の操作がその規則に正しく結線されているか」。
 *
 * 品目や行き先が要るテストは [TestAppStateRule] で DB に直接入れてから踏む（T-21）。
 * DB はメモリ上にあり、テストごとに空へ戻るので、後片付けは要らない。
 * **端末の戻りは iOS にもある**（端スワイプ）——iOS 側の対は `ShoppingIosTest`（FB-18）。
 *
 * **iOS 側の対は `shared/ui/src/iosTest` の `NavigationIosTest`。** 共通化する手立てが無い
 * （モジュールも source set も別）ので、**遷移を足したら両方に足すこと**。
 * 向こうは端末の戻るを踏む 6 件を持たない代わりに、空状態を見る 2 件を持つ。
 */
class NavigationTest {

    /** 品目が要るテストの 1 件。行タップでカゴへ入れる前の状態 */
    private val itemName = "アイテム1"

    /** シードで振られた行き先の id。**DB を空にしても採番は戻らない**ので、固定値で書かない */
    private var destinationId = 0

    @get:Rule(order = 0)
    val appState = TestAppStateRule {
        destinationId = destination(DESTINATION_NAME)
        item(Item(name = itemName))
        item(Item(name = DESTINATION_ITEM_NAME, destinationId = destinationId))
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private fun string(resource: StringResource, vararg args: Any): String =
        runBlocking { getString(resource, *args) }

    private val poolTitle = string(Res.string.pool_title)
    private val settingTitle = string(Res.string.setting_title)
    private val categoryManageTitle = string(Res.string.manage_category_title)
    private val categoryEditLabel = string(Res.string.setting_row_category_edit)
    private val shoppingTitleAll = string(Res.string.shopping_title_all)

    private val licenseLabel = string(Res.string.license_title)

    /** 現在表示されている画面を TopAppBar のタイトルで判定する。 */
    private fun assertCurrentScreenIs(title: String) {
        composeRule.onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertTextEquals(title)
    }

    private fun pressBack() {
        Espresso.pressBack()
        composeRule.waitForIdle()
    }

    private fun tapBackArrow() {
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
    }

    /** 行き先なしの品目をカゴへ入れて CTA を踏む。行き先付きが無いので 03 を挟まず全件モードの 04 へ入る（FB-04） */
    private fun startShoppingWithoutDestination() {
        composeRule.onNodeWithText(itemName).performClick()
        composeRule.onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        assertCurrentScreenIs(shoppingTitleAll)
    }

    /** 設定の下にあるカテゴリの管理を開く。 */
    private fun openCategoryManage() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(categoryEditLabel).performClick()
    }

    @Test
    fun 起動直後はプールが表示される() {
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun プールから設定へ遷移して端末の戻るでプールに帰る() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        pressBack()
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun プールから設定へ遷移して戻る矢印でプールに帰る() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        assertCurrentScreenIs(settingTitle)

        tapBackArrow()
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun 設定からライセンスへ遷移して端末の戻るで1段ずつプールまで帰る() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        pressBack()
        assertCurrentScreenIs(settingTitle)

        pressBack()
        assertCurrentScreenIs(poolTitle)
    }

    @Test
    fun ライセンスの戻る矢印で設定に帰る() {
        composeRule.onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText(licenseLabel).performClick()
        assertCurrentScreenIs(licenseLabel)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    /**
     * ＋ で登録シートが開き、端末の戻るで閉じる（画面 02・§2）。
     *
     * **`ModalBottomSheet` は Android と skiko で実装が分かれる**ので、iOS の
     * `PoolIosTest` だけでは Android 固有の壊れ方を止められない（テスト戦略定義書 §2.4）。
     */
    @Test
    fun プールの追加ボタンで登録シートが開いて端末の戻るで閉じる() {
        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        assertCurrentScreenIs(poolTitle)
    }

    /**
     * 行の長押しで編集シートが開き、端末の戻るで閉じる（画面 01・06）。
     *
     * **`ModalBottomSheet` も長押しのジェスチャも Android と skiko で実装が分かれる**ので、
     * iOS の `ItemEditSheetIosTest` だけでは Android 固有の壊れ方を止められない（§2.4）。
     */
    @Test
    fun 行の長押しで編集シートが開いて端末の戻るで閉じる() {
        composeRule.onNodeWithText(itemName).performTouchInput { longClick() }
        composeRule.waitUntilTagExists(TestTags.ITEM_SHEET_NAME_FIELD)
        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        assertCurrentScreenIs(poolTitle)
    }

    /**
     * 登録シートから入れた品目がプールに出て、シートが閉じる（画面 02）。
     *
     * シードでは通らない経路——`ModalBottomSheet` の閉じ方と、Android の driver での書き込みが
     * `Flow` で画面へ戻ってくるところ（§2.4）。
     */
    @Test
    fun 登録シートから入れた品目がプールに出てシートが閉じる() {
        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("登録した品目")
        composeRule.onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        composeRule.onNodeWithText("登録した品目").assertIsDisplayed()
    }

    /** 編集シートから削除すると、確認を挟んでプールから行が消える（画面 06）。 */
    @Test
    fun 編集シートから削除すると確認を挟んで行が消える() {
        composeRule.onNodeWithText(itemName).performTouchInput { longClick() }
        composeRule.waitUntilTagExists(TestTags.ITEM_SHEET_DELETE)

        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        composeRule.onNodeWithText(itemName).assertDoesNotExist()
    }

    /**
     * 買い物モード（04）は端末の戻るを離脱確認で受け止める（画面 04）。
     *
     * **`BackHandler` が `NavDisplay` の戻るより先に受けている**ことを Android 側で見る。
     * iOS 側は `ShoppingIosTest` が端スワイプで同じ経路を通す（FB-18）。
     */
    @Test
    fun 買い物モードの端末の戻るは離脱確認を挟む() {
        startShoppingWithoutDestination()

        // ダイアログを開いたままの戻るは、確認なく抜けずダイアログを閉じるだけ（§2）
        pressBack()
        pressBack()
        assertCurrentScreenIs(shoppingTitleAll)

        // 「続ける」なら 04 に留まる
        pressBack()
        composeRule.onNodeWithTag(TestTags.SHOPPING_LEAVE_CANCEL).performClick()
        assertCurrentScreenIs(shoppingTitleAll)

        pressBack()
        composeRule.onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).performClick()
        assertCurrentScreenIs(poolTitle)
    }

    /**
     * **05 表示中の戻るはシートを閉じるだけ**（画面 04）。離脱確認は出さない。
     *
     * **`ModalBottomSheet` は Android と skiko で実装が分かれる**ので、iOS 側の
     * `ShoppingIosTest` と対で持つ（§2.4）。
     */
    @Test
    fun 気づいたものを足すシートの端末の戻るはシートだけ閉じる() {
        startShoppingWithoutDestination()
        composeRule.onNodeWithTag(TestTags.SHOPPING_ADD_NOTICED_ROW).performClick()
        composeRule.onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(TestTags.ADD_NOTICED_SEARCH_FIELD).assertDoesNotExist()
        // 離脱確認は出さない。04 に留まる
        composeRule.onNodeWithTag(TestTags.SHOPPING_LEAVE_CONFIRM).assertDoesNotExist()
        assertCurrentScreenIs(shoppingTitleAll)
    }

    /**
     * 行き先付きの品目がカゴにあると 03 が開き、行のタップでその行き先の 04 へ入る（画面 03）。
     *
     * **Android で 03 を開くのはここだけ**——FB-04 以降、全件モードは 03 を通らない。
     * 03 も `ModalBottomSheet` なので、iOS の `ShoppingStartSheetIosTest` と対で持つ（§2.4）。
     */
    @Test
    fun 行き先付きの品目で03の行から行き先の買い物へ入る() {
        composeRule.onNodeWithText(DESTINATION_ITEM_NAME).performClick()
        composeRule.onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        val rowTag = TestTags.shoppingStartRow(destinationId)
        composeRule.waitUntilTagExists(rowTag)

        composeRule.onNodeWithTag(rowTag).performClick()

        assertCurrentScreenIs(string(Res.string.shopping_title, DESTINATION_NAME))
    }

    @Test
    fun 設定からカテゴリの管理へ遷移して端末の戻るで設定に帰る() {
        openCategoryManage()
        assertCurrentScreenIs(categoryManageTitle)

        pressBack()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun カテゴリの管理の戻る矢印で設定に帰る() {
        openCategoryManage()
        assertCurrentScreenIs(categoryManageTitle)

        tapBackArrow()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun プールで戻るとアプリが終了する() {
        assertCurrentScreenIs(poolTitle)

        // Activity が破棄されると composeRule.activity は取得自体が落ちるので、先に掴んでおく
        val activity = composeRule.activity

        // pressBack だとアプリ終了時に NoActivityResumedException になるので無条件版を使う
        Espresso.pressBackUnconditionally()
        // composeRule.waitForIdle() は composition が消えた後だと当てにならないので Espresso 側で待つ
        Espresso.onIdle()

        assertTrue(activity.isFinishing || activity.isDestroyed)
    }

    private companion object {
        const val DESTINATION_NAME = "行き先1"
        const val DESTINATION_ITEM_NAME = "アイテム2"
    }
}
