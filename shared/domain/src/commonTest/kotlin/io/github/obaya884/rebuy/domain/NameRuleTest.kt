package io.github.obaya884.rebuy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 名前の規則のうち、DB を引かずに決まるぶん（データモデル定義書 §5）。
 * 重複は同種の他の行を見るので `NameValidationRepositoryTest` が Repository ごしに見る。
 */
class NameRuleTest {

    @Test
    fun 前後の空白を落とす() {
        assertEquals("アイテム1", NameRule.normalize("  アイテム1 "))
    }

    /** **全角スペースも落とす。** `trim()` は Unicode の空白判定なので U+3000 も対象。 */
    @Test
    fun 全角スペースも空白として落とす() {
        assertEquals("アイテム1", NameRule.normalize("　アイテム1　"))
    }

    /** 間の空白は残す。名前の一部なので触らない。 */
    @Test
    fun 間の空白は残す() {
        assertEquals("アイテム 1", NameRule.normalize(" アイテム 1 "))
    }

    @Test
    fun 空は弾く() {
        assertEquals(NameError.BLANK, NameRule.validate(NameRule.normalize("")))
    }

    @Test
    fun 空白だけの名前は弾く() {
        assertEquals(NameError.BLANK, NameRule.validate(NameRule.normalize("　  ")))
    }

    @Test
    fun 上限ちょうどは通り超えると弾く() {
        assertNull(NameRule.validate("あ".repeat(30)))
        assertEquals(NameError.TOO_LONG, NameRule.validate("あ".repeat(31)))
    }

    /**
     * 長さは `String.length`（UTF-16 単位）で数える。**サロゲートペアの絵文字が 2 文字ぶんに
     * 数えられる**ことを許容する仕様（§5）なので、15 個で上限ちょうど。
     */
    @Test
    fun 絵文字は2文字ぶんに数える() {
        assertNull(NameRule.validate("🍎".repeat(15)))
        assertEquals(NameError.TOO_LONG, NameRule.validate("🍎".repeat(16)))
    }

    /** 文字種は制限しない。記号も絵文字も名前に使える。 */
    @Test
    fun 文字種は制限しない() {
        assertNull(NameRule.validate("#1 / 2 🍎"))
    }

    // ---- 打ち切り ----

    @Test
    fun 上限までは切り詰めない() {
        assertEquals("あ".repeat(30), NameRule.truncate("あ".repeat(30)))
    }

    @Test
    fun 上限を超えたぶんを落とす() {
        assertEquals("あ".repeat(30), NameRule.truncate("あ".repeat(35)))
    }

    /**
     * **サロゲートペアを割らない。** 素の `take(30)` だと 30 単位目に絵文字の上位
     * サロゲートだけが残り、壊れた 1 文字が入力欄にも DB にも入る。
     */
    @Test
    fun 上限の境目にある絵文字は丸ごと落とす() {
        val truncated = NameRule.truncate("あ".repeat(29) + "🍎")

        assertEquals("あ".repeat(29), truncated)
        assertNull(NameRule.validate(truncated))
    }

    /**
     * **上限ちょうどで完結する絵文字は残す。** 落とす条件を「サロゲートなら」まで
     * 緩めると、ここで下位サロゲートだけが落ちて壊れた 1 文字が残る。
     */
    @Test
    fun 上限ちょうどで終わる絵文字は残す() {
        val name = "あ".repeat(28) + "🍎"

        assertEquals(name, NameRule.truncate(name))
    }

    @Test
    fun 絵文字だけの列でも割らない() {
        assertEquals("🍎".repeat(15), NameRule.truncate("🍎".repeat(20)))
    }
}
