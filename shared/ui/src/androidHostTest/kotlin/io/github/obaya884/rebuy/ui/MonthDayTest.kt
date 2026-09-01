package io.github.obaya884.rebuy.ui

import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * 一覧に出す「前回 M/D」の書式（画面定義書 §2）。
 *
 * `formatShortDate` と違って**ロケールに従わない**ので、ロケールは差し替えずに
 * タイムゾーンだけを固定する。**期待値はリテラルで書く**（テスト戦略定義書 §2.1）。
 *
 * タイムゾーンを差し替える API が common に無いので JVM 側に置いている。
 * iOS の actual は文字列を見ずに済む不変条件を `MonthDayIosTest` が押さえる。
 */
class MonthDayTest {

    private lateinit var originalTimeZone: TimeZone

    @BeforeTest
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun 月と日をスラッシュで繋ぐ() {
        assertEquals("1/2", formatMonthDay(Instant.parse("2026-01-02T03:04:05Z")))
    }

    /** **ゼロ埋めしない。** 桁を揃えるのは等幅数字の役目で、書式の役目ではない。 */
    @Test
    fun ゼロ埋めしない() {
        assertEquals("9/5", formatMonthDay(Instant.parse("2026-09-05T00:00:00Z")))
        assertEquals("12/31", formatMonthDay(Instant.parse("2026-12-31T00:00:00Z")))
    }

    /**
     * **端末のタイムゾーンで日付に落とす。** UTC のままだと、日本では日付が 1 日ずれる
     * 時間帯（UTC の 15 時以降）が毎日ある。
     *
     * **タイムゾーンを差し替えて 2 通り見る。** 1 通りだけだと `ZoneId.of("Asia/Tokyo")` の
     * ような決め打ちの実装でも通ってしまう（`ShortDateTest` が同じ轍で 1 件足している）。
     */
    @Test
    fun 端末のタイムゾーンで日付を決める() {
        val instant = Instant.parse("2026-01-01T16:00:00Z")

        // UTC では 1/1。日本では日付が変わっている
        assertEquals("1/2", formatMonthDay(instant))

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        assertEquals("1/1", formatMonthDay(instant))
    }
}
