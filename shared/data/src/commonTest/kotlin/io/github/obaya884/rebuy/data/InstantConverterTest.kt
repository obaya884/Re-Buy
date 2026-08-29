package io.github.obaya884.rebuy.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * DB に保存される日時の表現を固定する。
 *
 * 固定している条項（CLAUDE.md「アーキテクチャ / データ層」）は 2 つ。
 * - エポックミリ秒（`INTEGER`）で保存する
 * - ミリ秒未満は切り捨てる（0 方向ではなく過去方向）
 *
 * **タイムゾーンに依存しないことの検査はここには無い。** 既定タイムゾーンを差し替える
 * API が common に無いため、`InstantConverterTimeZoneTest`（`androidHostTest`）に置いてある。
 */
class InstantConverterTest {

    // ---- 書き出し方向（Instant → Long） ----

    @Test
    fun fromInstant_エポックからのミリ秒になる() {
        // オフセット付きの表記が正しいエポックミリ秒になること
        val instant = Instant.parse("2024-01-01T09:00:00+09:00")

        val actual = InstantConverter.fromInstant(instant)

        assertEquals(1_704_067_200_000L, actual)
    }

    @Test
    fun fromInstant_エポック前は負になる() {
        val actual = InstantConverter.fromInstant(Instant.parse("1969-12-31T23:59:59Z"))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun fromInstant_ミリ秒未満を切り捨てる() {
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(1, 999_999))

        assertEquals(1_000L, actual)
    }

    @Test
    fun fromInstant_エポック前でも切り捨ては過去方向() {
        // エポックの 999 ミリ秒ほど前。0 方向の切り捨てなら -999 になる
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(-1, 999_999))

        assertEquals(-1_000L, actual)
    }

    /**
     * `Instant` は ±約 10 億年まで表現できるが、`Long` のミリ秒は ±約 29 万年しか持てない。
     * 収まらない値は**例外ではなく上限・下限に張り付く**。往復しても戻らないので、
     * 「収まらない値を渡すと落ちる」と考えないための固定。
     */
    @Test
    fun fromInstant_Longに収まらない未来は上限に張り付く() {
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(Long.MAX_VALUE))

        assertEquals(Long.MAX_VALUE, actual)
    }

    @Test
    fun fromInstant_Longに収まらない過去は下限に張り付く() {
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(Long.MIN_VALUE))

        assertEquals(Long.MIN_VALUE, actual)
    }

    /** 張り付く手前、`Long` のミリ秒に収まる境界そのものは往復する。 */
    @Test
    fun Longに収まる境界の日時は往復する() {
        val boundary = Instant.fromEpochMilliseconds(Long.MAX_VALUE)

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(boundary))

        assertEquals(boundary, actual)
    }

    // ---- 読み出し方向（Long → Instant） ----

    @Test
    fun toInstant_エポックからのミリ秒として読む() {
        val actual = InstantConverter.toInstant(1_704_067_200_000L)

        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), actual)
    }

    @Test
    fun toInstant_Longの下限も読める() {
        val actual = InstantConverter.toInstant(Long.MIN_VALUE)

        // -9223372036854775808 ミリ秒 = -9223372036854776 秒 + 192,000,000 ナノ秒
        assertEquals(Instant.fromEpochSeconds(-9_223_372_036_854_776L, 192_000_000), actual)
    }

    @Test
    fun toInstant_Longの上限も読める() {
        val actual = InstantConverter.toInstant(Long.MAX_VALUE)

        assertEquals(Instant.fromEpochSeconds(9_223_372_036_854_775L, 807_000_000), actual)
    }

    // ---- 往復 ----

    @Test
    fun 往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = Instant.parse("2024-06-15T12:34:56.789123456Z")

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(Instant.parse("2024-06-15T12:34:56.789Z"), actual)
    }

    @Test
    fun エポック前でも往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = Instant.parse("1960-06-15T12:34:56.789123456Z")

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(Instant.parse("1960-06-15T12:34:56.789Z"), actual)
    }
}
