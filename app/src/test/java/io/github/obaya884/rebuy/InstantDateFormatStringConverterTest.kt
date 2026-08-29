package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.InstantDateFormatStringConverter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.TimeZone

/**
 * DB に保存される日時の表現を固定する。
 *
 * 固定している条項（CLAUDE.md「アーキテクチャ / データ層」）は 3 つ。
 * - `YYYY-MM-DD HH:MM:SS` の **UTC** 文字列で保存する
 * - 0 年未満・10000 年以上は例外
 * - 読み出しは非準拠の文字列を黙って別の日時として読まず、例外にする
 */
class InstantDateFormatStringConverterTest {

    private lateinit var originalTimeZone: TimeZone

    /**
     * 既定タイムゾーンを非 UTC に固定する。
     *
     * これが無いと、実装の `withZone(ZoneOffset.UTC)` を `systemDefault()` に変えても、
     * TZ=UTC で動く CI では両者が同じ意味になり、全テストが緑のまま通ってしまう。
     * 「UTC で保存する」という条項をホストのタイムゾーンに依存せず守るために固定する。
     *
     * このクラスの全テストが JST で走るので、既定 TZ が UTC である前提のテストは足さないこと。
     */
    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun instantOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        nano: Int = 0,
        offset: ZoneOffset = ZoneOffset.UTC
    ): Instant = OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset).toInstant()

    private fun assertNotParsable(text: String) {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant(text)
        }
    }

    // ---- 書き出し方向（Instant → String）の境界 ----

    @Test
    fun toString_下限() {
        val actual = InstantDateFormatStringConverter.toString(instantOf(0, 1, 1))

        assertEquals("0000-01-01 00:00:00", actual)
    }

    @Test
    fun toString_上限は秒未満を切り捨てる() {
        val actual = InstantDateFormatStringConverter.toString(
            instantOf(9999, 12, 31, 23, 59, 59, nano = 999_999_999)
        )

        // 切り捨てなので、年 10000 へ繰り上がらない
        assertEquals("9999-12-31 23:59:59", actual)
    }

    @Test
    fun toString_下限より下は例外() {
        assertThrows(DateTimeException::class.java) {
            InstantDateFormatStringConverter.toString(instantOf(-1, 12, 31, 23, 59, 59))
        }
    }

    @Test
    fun toString_上限より上は例外() {
        assertThrows(DateTimeException::class.java) {
            InstantDateFormatStringConverter.toString(instantOf(10000, 1, 1))
        }
    }

    @Test
    fun toString_エポック前でも秒未満を切り捨てる() {
        // 切り捨ては 0 方向ではなく過去方向。0 方向なら "1970-01-01 00:00:00" になる
        val actual = InstantDateFormatStringConverter.toString(
            Instant.ofEpochSecond(-1, 500_000_000)
        )

        assertEquals("1969-12-31 23:59:59", actual)
    }

    // ---- 読み出し方向（String → Instant）の境界 ----

    @Test
    fun toInstant_下限() {
        val actual = InstantDateFormatStringConverter.toInstant("0000-01-01 00:00:00")

        assertEquals(instantOf(0, 1, 1), actual)
    }

    @Test
    fun toInstant_上限() {
        val actual = InstantDateFormatStringConverter.toInstant("9999-12-31 23:59:59")

        assertEquals(instantOf(9999, 12, 31, 23, 59, 59), actual)
    }

    // ---- 読み出し方向の異常系（形式に合わない） ----

    @Test
    fun toInstant_空文字は例外() = assertNotParsable("")

    @Test
    fun toInstant_時刻部が無いのは例外() = assertNotParsable("2024-01-01")

    @Test
    fun toInstant_末尾に余分な文字があるのは例外() = assertNotParsable("2024-01-01 00:00:00.123")

    @Test
    fun toInstant_ゼロ埋めされていないのは例外() = assertNotParsable("2024-1-1 0:0:0")

    @Test
    fun toInstant_区切りがTなのは例外() = assertNotParsable("2024-01-01T00:00:00")

    @Test
    fun toInstant_前に空白があるのは例外() = assertNotParsable(" 2024-01-01 00:00:00")

    @Test
    fun toInstant_後ろに空白があるのは例外() = assertNotParsable("2024-01-01 00:00:00 ")

    @Test
    fun toInstant_年が5桁なのは例外() = assertNotParsable("10000-01-01 00:00:00")

    @Test
    fun toInstant_年が負なのは例外() = assertNotParsable("-0001-12-31 23:59:59")

    @Test
    fun toInstant_月が範囲外なのは例外() = assertNotParsable("2024-13-01 00:00:00")

    @Test
    fun toInstant_日が範囲外なのは例外() = assertNotParsable("2024-01-00 00:00:00")

    @Test
    fun toInstant_分が範囲外なのは例外() = assertNotParsable("2024-01-01 00:60:00")

    // ---- 読み出し方向の異常系（形式は合うが日付として存在しない） ----
    // ResolverStyle.STRICT により丸めずに例外にする。既定の SMART だと
    // 2024-02-30 が 2024-02-29 に、24:00:00 が翌日に黙って読み替えられる。

    @Test
    fun toInstant_2月30日は丸めずに例外() = assertNotParsable("2024-02-30 00:00:00")

    @Test
    fun toInstant_閏年でない2月29日は丸めずに例外() = assertNotParsable("2023-02-29 00:00:00")

    @Test
    fun toInstant_4月31日は丸めずに例外() = assertNotParsable("2024-04-31 00:00:00")

    @Test
    fun toInstant_24時は翌日に繰り上げずに例外() = assertNotParsable("2024-01-01 24:00:00")

    // ---- UTC で保存するという条項 ----

    @Test
    fun toString_UTCで書き出す() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00
        val instant = instantOf(2024, 1, 1, 9, offset = ZoneOffset.ofHours(9))

        val actual = InstantDateFormatStringConverter.toString(instant)

        assertEquals("2024-01-01 00:00:00", actual)
    }

    @Test
    fun toInstant_UTCとして読む() {
        val expected = instantOf(2024, 1, 1, 9, offset = ZoneOffset.ofHours(9))

        val actual = InstantDateFormatStringConverter.toInstant("2024-01-01 00:00:00")

        assertEquals(expected, actual)
    }

    // ---- 往復 ----

    @Test
    fun 往復して秒未満の切り捨て以外は変わらない() {
        val original = instantOf(2024, 6, 15, 12, 34, 56, nano = 789_000_000)

        val actual = InstantDateFormatStringConverter.toInstant(
            InstantDateFormatStringConverter.toString(original)
        )

        assertEquals(original.truncatedTo(ChronoUnit.SECONDS), actual)
    }

    @Test
    fun エポック前でも往復して秒未満の切り捨て以外は変わらない() {
        val original = instantOf(1960, 6, 15, 12, 34, 56, nano = 789_000_000)

        val actual = InstantDateFormatStringConverter.toInstant(
            InstantDateFormatStringConverter.toString(original)
        )

        assertEquals(original.truncatedTo(ChronoUnit.SECONDS), actual)
    }
}
