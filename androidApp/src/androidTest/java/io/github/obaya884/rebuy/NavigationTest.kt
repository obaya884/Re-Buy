package io.github.obaya884.rebuy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * DB の中身に依存する遷移（買い物終了でプールへ戻る）は、データを用意する必要があるため扱わない。
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
