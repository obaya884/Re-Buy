package io.github.obaya884.rebuy.ui

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone
import platform.Foundation.setDefaultTimeZone
import platform.Foundation.timeZoneWithName

/**
 * iOS の [formatMonthDay]。**こちらは書式が仕様なので、期待値をリテラルで持てる**
 * （ロケールに委ねる `formatShortDate` との違い）。
 *
 * 見ているのは Android と同じ 3 点——M/D で繋ぐこと、ゼロ埋めしないこと、
 * 端末のタイムゾーンで日付に落とすこと。**手書きなのは秒への割り算と `NSCalendar` の
 * 使い方**で、どちらもここでしか通らない。
 */
class LocalDateIosTest {

    private lateinit var originalTimeZone: NSTimeZone

    @BeforeTest
    fun setUp() {
        originalTimeZone = NSTimeZone.defaultTimeZone
        NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName("Asia/Tokyo")!!)
    }

    @AfterTest
    fun tearDown() {
        NSTimeZone.setDefaultTimeZone(originalTimeZone)
    }

    @Test
    fun 月と日をスラッシュで繋ぎゼロ埋めしない() {
        assertEquals("1/2", formatMonthDay(Instant.parse("2026-01-02T03:04:05Z")))
        assertEquals("9/5", formatMonthDay(Instant.parse("2026-09-05T00:00:00Z")))
        assertEquals("12/31", formatMonthDay(Instant.parse("2026-12-31T00:00:00Z")))
    }

    @Test
    fun 年月日はゼロ埋めして繋ぐ() {
        assertEquals("2026-01-02", formatFullDate(Instant.parse("2026-01-02T03:04:05Z")))
        assertEquals("2026-12-31", formatFullDate(Instant.parse("2026-12-31T00:00:00Z")))
    }

    /**
     * **タイムゾーンを差し替えて 2 通り見る。** 1 通りだけだと固定タイムゾーンの
     * 決め打ちでも通ってしまう。
     */
    @Test
    fun 端末のタイムゾーンで日付を決める() {
        val instant = Instant.parse("2026-01-01T16:00:00Z")

        assertEquals("1/2", formatMonthDay(instant))

        NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName("America/Los_Angeles")!!)
        assertEquals("1/1", formatMonthDay(instant))
        assertEquals("2026-01-01", formatFullDate(instant))
    }
}
