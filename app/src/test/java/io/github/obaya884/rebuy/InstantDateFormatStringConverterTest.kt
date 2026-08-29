package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.InstantDateFormatStringConverter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    /** 境界値テスト。 */
    @Test
    fun test_instantToString_boundaryValue() {
        // 下限
        OffsetDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .let { InstantDateFormatStringConverter.toString(it) }
            .also { assertEquals("0000-01-01 00:00:00", it) }
        // 上限（秒未満は切り捨て）
        OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC)
            .toInstant()
            .let { InstantDateFormatStringConverter.toString(it) }
            .also { assertEquals("9999-12-31 23:59:59", it) }

        // 下限より下
        OffsetDateTime.of(-1, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)
            .toInstant()
            .let {
                runCatching {
                    InstantDateFormatStringConverter.toString(it)
                }
            }.also {
                assertTrue(it.exceptionOrNull() is DateTimeException)
            }
        // 上限より上
        OffsetDateTime.of(10000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .let {
                runCatching {
                    InstantDateFormatStringConverter.toString(it)
                }
            }.also {
                assertTrue(it.exceptionOrNull() is DateTimeException)
            }
    }

    @Test
    fun test_stringToInstant() {
        // 下限
        InstantDateFormatStringConverter.toInstant("0000-01-01 00:00:00")
            .also {
                assertEquals(
                    OffsetDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                    it
                )
            }
        // 上限
        InstantDateFormatStringConverter.toInstant("9999-12-31 23:59:59")
            .also {
                assertEquals(
                    OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC).toInstant(),
                    it
                )
            }
    }

    /**
     * 読み出し方向（DB から読む向き）の異常系。
     *
     * 旧バージョンや手書きで非準拠の文字列が入っていた場合に、黙って別の日時として読まずに落ちること。
     */
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

    /**
     * UTC で保存するという条項を固定する。
     *
     * 入力が UTC 由来の [Instant] だけだと、実装のタイムゾーンを systemDefault() に変えても
     * TZ=UTC の環境（CI）では緑のまま通ってしまうので、非 UTC のオフセット由来の値で確かめる。
     */
    @Test
    fun test_instantToString_UTCで書き出す() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00
        val instant = OffsetDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant()

        val actual = InstantDateFormatStringConverter.toString(instant)

        assertEquals("2024-01-01 00:00:00", actual)
    }

    @Test
    fun test_stringToInstant_UTCとして読む() {
        val expected = OffsetDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9)).toInstant()

        val actual = InstantDateFormatStringConverter.toInstant("2024-01-01 00:00:00")

        assertEquals(expected, actual)
    }

    /** 書き出して読み戻すと、秒未満を切り捨てた元の値に一致する。 */
    @Test
    fun test_往復して秒未満の切り捨て以外は変わらない() {
        val original =
            OffsetDateTime.of(2024, 6, 15, 12, 34, 56, 789_000_000, ZoneOffset.UTC).toInstant()

        val actual = InstantDateFormatStringConverter.toInstant(
            InstantDateFormatStringConverter.toString(original)
        )

        assertEquals(original.truncatedTo(ChronoUnit.SECONDS), actual)
    }
}
