package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.ui.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 解釈の余地がある文言だけを、**期待値をリテラルで持って**固定する。
 *
 * リソースを Android の `res` から Compose Resources へ移したこと（T-31 ステップ 11）で、
 * `\n` のエスケープと `%1$s` の位置指定を解くのは AAPT ではなく CMP 自身になった。
 * CMP の書式化は `String.format` ではなく `%(\d+)\$[ds]` の単純置換で、
 * **解釈できない書式は例外を出さずに生のまま画面へ出る**。
 *
 * ほかのテストは文言を `Res.string.*` から引く。実装と同じ正を見るためで、それは正しい。
 * ただしそのぶん、値が壊れたときに画面とテストが同じ壊れた値を見るので退行が捕まらない。
 * **ここだけはリテラルで持つ**——リソースから引いた瞬間に自己参照に戻り、網でなくなる。
 *
 * 49 件すべては固定しない。素通しの 43 件は AAPT でも CMP でも読み方が 1 通りしかなく、
 * 文言を変えるたびにこのテストを直す負債が残るだけ。
 */
class StringResourceFormatTest {

    private fun string(resource: StringResource): String = runBlocking { getString(resource) }

    private fun string(resource: StringResource, arg: String): String =
        runBlocking { getString(resource, arg) }

    @Test
    fun 改行のエスケープが実際の改行になる() {
        assertEquals(
            "カテゴリAを削除しますか？\nこのカテゴリに設定されたアイテムのカテゴリは未設定になります。",
            string(Res.string.category_edit_delete_dialog_message, "カテゴリA")
        )
    }

    @Test
    fun 位置指定の引数が差し込まれる() {
        assertEquals(
            "前回 8/29",
            string(Res.string.pool_last_bought_at, "8/29")
        )
        assertEquals(
            "全 12 件",
            string(Res.string.pool_total_count, "12")
        )
        assertEquals(
            "🏬 スーパー",
            string(Res.string.pool_destination_prefix, "スーパー")
        )
        assertEquals(
            "アイテムAを削除しますか？",
            string(Res.string.item_edit_delete_dialog_message, "アイテムA")
        )
        // 位置指定と改行の両方を持つ唯一の文言
        assertEquals(
            "カテゴリAを削除しますか？\nこのカテゴリに設定されたアイテムのカテゴリは未設定になります。",
            string(Res.string.category_edit_delete_dialog_message, "カテゴリA")
        )
    }
}
