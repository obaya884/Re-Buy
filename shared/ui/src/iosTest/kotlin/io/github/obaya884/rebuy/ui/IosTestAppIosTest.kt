package io.github.obaya884.rebuy.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.data.item.ItemStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * **[runIosApp] が本番 iOS の構成を作っていること。**
 *
 * ここが無いと、[runIosApp] を通る 10 ファイルが**構成ごと旧仕様へ戻っても全件緑**になる——
 * 描き手を Material に戻す変異も、被さる上端を 0 にする変異も、中身を見るだけのテストは
 * 1 件も落とさない（実測）。**この Step の成果物そのものを守る網。**
 */
@OptIn(ExperimentalTestApi::class)
class IosTestAppIosTest {

    /** **外枠は Compose が描かない。** 描くと Step 4 で見出しが縦に 2 つ並ぶ（13 §6）。 */
    @Test
    fun 外枠は描かれない() = runIosApp(oneItem(ItemStatus.NO_DEAL)) { probe ->
        onNodeWithTag(TestTags.TOP_APP_BAR_TITLE).assertDoesNotExist()
        onNodeWithTag(TestTags.POOL_ADD_BUTTON).assertDoesNotExist()
        onNodeWithTag(TestTags.POOL_SETTINGS_BUTTON).assertDoesNotExist()

        // 中身は出ていて、外枠の内容は橋から渡っている
        onNodeWithTag(TestTags.poolRow(itemId = 1)).assertExists()
        probe.assertScreen(ScreenTitle.pool)
    }

    /** 04 の ← も出ない（01 は ← を持たないので、別の画面で見る）。 */
    @Test
    fun 戻るも描かれない() = runIosApp(oneItem(ItemStatus.IN_SHOPPING_LIST)) { probe ->
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()

        probe.assertScreen(ScreenTitle.shoppingAll)
        onNodeWithTag(TestTags.BACK_BUTTON).assertDoesNotExist()
    }

    /**
     * **被さる上端が効いている。** ここが 0 に戻ると、[runIosApp] を通るテストは
     * 「バーの下から始まる」状態を 1 度も通らなくなる（シートとダイアログを含む）。
     */
    @Test
    fun 被さる上端が効いている() = runIosApp(oneItem(ItemStatus.IN_SHOPPING_LIST)) {
        onNodeWithTag(TestTags.POOL_START_SHOPPING_BUTTON).performClick()
        waitForIdle()

        val top = onNodeWithTag(TestTags.shoppingRow(itemId = 1)).getUnclippedBoundsInRoot().top

        // 04 の一覧は素の値 16dp を持つ（`ShoppingScreen`）
        assertEquals(16.dp + BAR_OVERLAP_TOP, top, "被さる上端が効いていない")
    }

    /** **01 の添える数は印を持たない**（`ReBuyAppBarState.Count` の条項）。 */
    @Test
    fun プールの添える数は印を持たない() = runIosApp(oneItem(ItemStatus.NO_DEAL)) { probe ->
        probe.assertCount(text = "全 1 件", accessibilityId = null)
    }
}
