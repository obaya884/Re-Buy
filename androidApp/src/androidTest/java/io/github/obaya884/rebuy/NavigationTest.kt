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
 * 買い物モード（04）だけは DB に品目が要るが、**端末の戻るを離脱確認で受け止めるのは
 * Android 固有**（iOS にハードウェアの戻るが無い）なので、登録シートから 1 件用意して踏む。
 *
 * **iOS 側の対は `shared/ui/src/iosTest` の `NavigationIosTest`。** 共通化する手立てが無い
 * （モジュールも source set も別）ので、**遷移を足したら両方に足すこと**。
 * 向こうは端末の戻るを踏む 6 件を持たない代わりに、空状態を見る 2 件を持つ。
 */
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private val poolTitle = string(Res.string.pool_title)
    private val settingTitle = string(Res.string.setting_title)
    private val categoryEditTitle = string(Res.string.category_edit_title)
    private val categoryEditLabel = string(Res.string.setting_row_category_edit)
    private val shoppingTitleAll = string(Res.string.shopping_title_all)

    /** ライセンス画面のタイトルと設定画面の行は実装側もハードコードなので、ここでも文字列で持つ。 */
    private val licenseLabel = "ライセンス"

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

    /**
     * 品目を消す。**実機の DB に残すと、次の実行が重複名で弾かれて別の理由で落ち続ける**ので、
     * 品目を作るテストは finally からここを通す。
     */
    private fun deleteItem(name: String) {
        composeRule.onNodeWithText(name).performTouchInput { longClick() }
        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE).performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_DELETE_CONFIRM).performClick()
        composeRule.waitForIdle()
    }

    /** 設定の下にあるカテゴリーの管理を開く。 */
    private fun openCategoryEdit() {
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
     * 登録まで踏むと本物の DB に品目が残るので、開いて閉じるところまで。
     */
    @Test
    fun プールの追加ボタンで登録シートが開いて端末の戻るで閉じる() {
        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()

        pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertDoesNotExist()
        assertCurrentScreenIs(poolTitle)
    }

    /**
     * 行の長押しで編集シートが開き、端末の戻るで閉じる（画面 01・06）。
     *
     * **`ModalBottomSheet` も長押しのジェスチャも Android と skiko で実装が分かれる**ので、
     * iOS の `ItemEditSheetIosTest` だけでは Android 固有の壊れ方を止められない（§2.4）。
     * 品目が要るので、登録シートから 1 件入れてから踏む。
     */
    @Test
    fun 行の長押しで編集シートが開いて端末の戻るで閉じる() {
        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD)
            .performTextInput("長押しの確認用")
        composeRule.onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()
        composeRule.waitForIdle()

        try {
            composeRule.onNodeWithText("長押しの確認用").performTouchInput { longClick() }
            composeRule.onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertIsDisplayed()
        } finally {
            deleteItem("長押しの確認用")
        }

        composeRule.onNodeWithTag(TestTags.ITEM_SHEET_NAME_FIELD).assertDoesNotExist()
        assertCurrentScreenIs(poolTitle)
    }

    /**
     * 買い物モード（04）は端末の戻るを離脱確認で受け止める（画面 04）。
     *
     * `ShoppingIosTest` は ← の矢印しか踏めないので、**`BackHandler` が
     * `NavDisplay` の戻るより先に受けている**ことはここでしか見られない。
     */
    @Test
    fun 買い物モードの端末の戻るは離脱確認を挟む() {
        composeRule.onNodeWithTag(TestTags.POOL_ADD_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("離脱確認の確認用")
        composeRule.onNodeWithTag(TestTags.REGISTER_SUBMIT).performClick()
        composeRule.waitForIdle()

        // 行タップでカゴへ入れてから CTA → 03 の全件モードの行 → 04
        try {
            composeRule.onNodeWithText("離脱確認の確認用").performClick()
            composeRule.onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
            composeRule.onNodeWithTag(TestTags.SHOPPING_START_ALL_ROW).performClick()
            assertCurrentScreenIs(shoppingTitleAll)

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
        } finally {
            deleteItem("離脱確認の確認用")
        }
    }

    @Test
    fun 設定からカテゴリー一覧へ遷移して端末の戻るで設定に帰る() {
        openCategoryEdit()
        assertCurrentScreenIs(categoryEditTitle)

        pressBack()
        assertCurrentScreenIs(settingTitle)
    }

    @Test
    fun カテゴリー一覧の戻る矢印で設定に帰る() {
        openCategoryEdit()
        assertCurrentScreenIs(categoryEditTitle)

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
}
