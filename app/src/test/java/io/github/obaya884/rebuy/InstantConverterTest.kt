package io.github.obaya884.rebuy

import io.github.obaya884.rebuy.data.InstantConverter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.TimeZone

/**
 * DB に保存される日時の表現を固定する。
 *
 * 固定している条項（CLAUDE.md「アーキテクチャ / データ層」）は 2 つ。
 * - エポックミリ秒（`INTEGER`）で保存する
 * - ミリ秒未満は切り捨てる（0 方向ではなく過去方向）
 */
class InstantConverterTest {

    private lateinit var originalTimeZone: TimeZone

    /**
     * 既定タイムゾーンを非 UTC に固定する。
     *
     * 変換が `LocalDateTime` 経由になるとホストのタイムゾーンで値がずれるが、
     * TZ=UTC で動く CI ではそのずれが見えない。JST に寄せて検出できるようにする。
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

    // ---- 書き出し方向（Instant → Long） ----

    @Test
    fun fromInstant_エポックからのミリ秒になる() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00
        val instant = instantOf(2024, 1, 1, 9, offset = ZoneOffset.ofHours(9))

        val actual = InstantConverter.fromInstant(instant)

        assertEquals(1_704_067_200_000L, actual)
    }

    @Test
    fun fromInstant_エポック前は負になる() {
        val actual = InstantConverter.fromInstant(instantOf(1969, 12, 31, 23, 59, 59))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun fromInstant_ミリ秒未満を切り捨てる() {
        val actual = InstantConverter.fromInstant(Instant.ofEpochSecond(1, 999_999))

        assertEquals(1_000L, actual)
    }

    @Test
    fun fromInstant_エポック前でも切り捨ては過去方向() {
        // エポックの 1 マイクロ秒前。0 方向の切り捨てなら -999 になる
        val actual = InstantConverter.fromInstant(Instant.ofEpochSecond(-1, 999_999))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun fromInstant_上限を超える日時は例外() {
        assertThrows(ArithmeticException::class.java) {
            InstantConverter.fromInstant(Instant.MAX)
        }
    }

    @Test
    fun fromInstant_下限を下回る日時は例外() {
        assertThrows(ArithmeticException::class.java) {
            InstantConverter.fromInstant(Instant.MIN)
        }
    }

    // ---- 読み出し方向（Long → Instant） ----

    @Test
    fun toInstant_エポックからのミリ秒として読む() {
        val actual = InstantConverter.toInstant(1_704_067_200_000L)

        assertEquals(instantOf(2024, 1, 1), actual)
    }

    @Test
    fun toInstant_Longの下限も読める() {
        val actual = InstantConverter.toInstant(Long.MIN_VALUE)

        // -9223372036854775808 ミリ秒 = -9223372036854776 秒 + 192,000,000 ナノ秒
        assertEquals(Instant.ofEpochSecond(-9_223_372_036_854_776L, 192_000_000), actual)
    }

    @Test
    fun toInstant_Longの上限も読める() {
        val actual = InstantConverter.toInstant(Long.MAX_VALUE)

        assertEquals(Instant.ofEpochSecond(9_223_372_036_854_775L, 807_000_000), actual)
    }

    // ---- 往復 ----

    @Test
    fun 往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = instantOf(2024, 6, 15, 12, 34, 56, nano = 789_123_456)

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(original.truncatedTo(ChronoUnit.MILLIS), actual)
    }

    @Test
    fun エポック前でも往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = instantOf(1960, 6, 15, 12, 34, 56, nano = 789_123_456)

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(original.truncatedTo(ChronoUnit.MILLIS), actual)
    }

}
