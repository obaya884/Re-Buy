package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.ItemStatusConverter
import io.github.obaya884.rebuy.data.item.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * DB に保存される [ItemStatus] の数値表現を固定する。
 *
 * この対応が変わると、既存の DB に入っている値が別の状態として読まれる。
 * CLAUDE.md「アーキテクチャ / データ層」の「enum の `value` は既存 DB と互換を壊さない限り
 * 変更しない」を機械で守るのがこのテストの役目。
 */
class ItemStatusConverterTest {

    @Test
    fun test_fromItemStatus_取引なしは0() {
        assertEquals(0, ItemStatusConverter.fromItemStatus(ItemStatus.NO_DEAL))
    }

    @Test
    fun test_fromItemStatus_買い物リストにあるは1() {
        assertEquals(1, ItemStatusConverter.fromItemStatus(ItemStatus.IN_SHOPPING_LIST))
    }

    @Test
    fun test_fromItemStatus_買い物リストでチェック済みは2() {
        assertEquals(2, ItemStatusConverter.fromItemStatus(ItemStatus.CHECKED_IN_SHOPPING_LIST))
    }

    @Test
    fun test_toItemStatus_0は取引なし() {
        assertEquals(ItemStatus.NO_DEAL, ItemStatusConverter.toItemStatus(0))
    }

    @Test
    fun test_toItemStatus_1は買い物リストにある() {
        assertEquals(ItemStatus.IN_SHOPPING_LIST, ItemStatusConverter.toItemStatus(1))
    }

    @Test
    fun test_toItemStatus_2は買い物リストでチェック済み() {
        assertEquals(ItemStatus.CHECKED_IN_SHOPPING_LIST, ItemStatusConverter.toItemStatus(2))
    }

    /**
     * 未知の値は黙って別の状態として読まずに落ちる。
     *
     * 既定値へ倒すと、壊れた行が「取引なし」として静かに紛れ込む。
     */
    @Test
    fun test_toItemStatus_未知の値は例外() {
        assertThrows(NoSuchElementException::class.java) {
            ItemStatusConverter.toItemStatus(3)
        }
    }

    @Test
    fun test_全ての状態が往復しても変わらない() {
        ItemStatus.entries.forEach { status ->
            val actual = ItemStatusConverter.toItemStatus(
                ItemStatusConverter.fromItemStatus(status)
            )

            assertEquals(status, actual)
        }
    }

    /**
     * 値が重複していないこと。
     *
     * [ItemStatusConverter.toItemStatus] は `first` で先頭一致を返すので、
     * 新しい状態が既存の値を再利用すると、黙って別の要素が返るようになる。
     */
    @Test
    fun test_状態の値に重複が無い() {
        val values = ItemStatus.entries.map { it.value }

        assertEquals(values.size, values.distinct().size)
    }
}
