package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.InstantDateFormatStringConverter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.TimeZone

class InstantDateFormatStringConverterTest {

    private lateinit var originalTimeZone: TimeZone

    /**
     * 既定タイムゾーンを非 UTC に固定する。
     *
     * これが無いと、実装の `withZone(ZoneOffset.UTC)` を `systemDefault()` に変えても、
     * TZ=UTC で動く CI では両者が同じ意味になり、全テストが緑のまま通ってしまう。
     * 「UTC で保存する」という条項をホストのタイムゾーンに依存せず守るために固定する。
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
        hour: Int,
        minute: Int,
        second: Int,
        nano: Int = 0,
        offset: ZoneOffset = ZoneOffset.UTC
    ) = OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset).toInstant()

    // ---- 書き出し方向（Instant → String）の境界 ----

    @Test
    fun test_instantToString_下限() {
        val actual = InstantDateFormatStringConverter.toString(instantOf(0, 1, 1, 0, 0, 0))

        assertEquals("0000-01-01 00:00:00", actual)
    }

    @Test
    fun test_instantToString_上限は秒未満を切り捨てる() {
        val actual = InstantDateFormatStringConverter.toString(
            instantOf(9999, 12, 31, 23, 59, 59, nano = 999_999_999)
        )

        // 切り捨てなので、年 10000 へ繰り上がらない
        assertEquals("9999-12-31 23:59:59", actual)
    }

    @Test
    fun test_instantToString_下限より下は例外() {
        assertThrows(DateTimeException::class.java) {
            InstantDateFormatStringConverter.toString(instantOf(-1, 12, 31, 23, 59, 59))
        }
    }

    @Test
    fun test_instantToString_上限より上は例外() {
        assertThrows(DateTimeException::class.java) {
            InstantDateFormatStringConverter.toString(instantOf(10000, 1, 1, 0, 0, 0))
        }
    }

    // ---- 読み出し方向（String → Instant）の境界 ----

    @Test
    fun test_stringToInstant_下限() {
        val actual = InstantDateFormatStringConverter.toInstant("0000-01-01 00:00:00")

        assertEquals(instantOf(0, 1, 1, 0, 0, 0), actual)
    }

    @Test
    fun test_stringToInstant_上限() {
        val actual = InstantDateFormatStringConverter.toInstant("9999-12-31 23:59:59")

        assertEquals(instantOf(9999, 12, 31, 23, 59, 59), actual)
    }

    // ---- 読み出し方向の異常系 ----
    // 旧バージョンや手書きで非準拠の文字列が入っていた場合に、
    // 黙って別の日時として読まずに落ちること。

    @Test
    fun test_stringToInstant_空文字() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("")
        }
    }

    @Test
    fun test_stringToInstant_時刻部が無い() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("2024-01-01")
        }
    }

    @Test
    fun test_stringToInstant_末尾に余分な文字がある() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("2024-01-01 00:00:00.123")
        }
    }

    @Test
    fun test_stringToInstant_ゼロ埋めされていない() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("2024-1-1 0:0:0")
        }
    }

    @Test
    fun test_stringToInstant_月が範囲外() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("2024-13-01 00:00:00")
        }
    }

    @Test
    fun test_stringToInstant_年が5桁() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("10000-01-01 00:00:00")
        }
    }

    @Test
    fun test_stringToInstant_年が負() {
        assertThrows(DateTimeParseException::class.java) {
            InstantDateFormatStringConverter.toInstant("-001-12-31 23:59:59")
        }
    }

    // ---- UTC で保存するという条項 ----

    @Test
    fun test_instantToString_UTCで書き出す() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00
        val instant = instantOf(2024, 1, 1, 9, 0, 0, offset = ZoneOffset.ofHours(9))

        val actual = InstantDateFormatStringConverter.toString(instant)

        assertEquals("2024-01-01 00:00:00", actual)
    }

    @Test
    fun test_stringToInstant_UTCとして読む() {
        val expected = instantOf(2024, 1, 1, 9, 0, 0, offset = ZoneOffset.ofHours(9))

        val actual = InstantDateFormatStringConverter.toInstant("2024-01-01 00:00:00")

        assertEquals(expected, actual)
    }

    // ---- 往復 ----

    @Test
    fun test_往復して秒未満の切り捨て以外は変わらない() {
        val original = instantOf(2024, 6, 15, 12, 34, 56, nano = 789_000_000)

        val actual = InstantDateFormatStringConverter.toInstant(
            InstantDateFormatStringConverter.toString(original)
        )

        assertEquals(original.truncatedTo(ChronoUnit.SECONDS), actual)
    }
}
