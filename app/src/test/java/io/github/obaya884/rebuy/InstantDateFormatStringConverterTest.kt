package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.InstantDateFormatStringConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.ZoneOffset

class InstantDateFormatStringConverterTest {
    /** 境界値テスト。 */
    @Test
    fun test_instantToString_boundaryValue() {
        // 下限
        OffsetDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .let { InstantDateFormatStringConverter.toString(it) }
            .also { assertEquals(it, "0000-01-01 00:00:00") }
        // 上限（秒未満は切り捨て）
        OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC)
            .toInstant()
            .let { InstantDateFormatStringConverter.toString(it) }
            .also { assertEquals(it, "9999-12-31 23:59:59") }

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
                    it,
                    OffsetDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant()
                )
            }
        // 上限
        InstantDateFormatStringConverter.toInstant("9999-12-31 23:59:59")
            .also {
                assertEquals(
                    it,
                    OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC).toInstant()
                )
            }
    }
}
