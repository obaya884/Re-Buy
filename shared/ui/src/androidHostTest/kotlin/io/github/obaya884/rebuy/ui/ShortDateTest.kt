package io.github.obaya884.rebuy.ui

import java.util.Locale
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * 「最終購入」の日付書式を、**期待値をリテラルで**固定する。
 *
 * 期待値を `DateTimeFormatter` から作ると自己参照になって網でなくなるので、ロケールと
 * タイムゾーンを固定したうえで文字列を直に書く。固定書式へ「揃える」変更をここで止める。
 *
 * ロケールと既定タイムゾーンを差し替える API が common に無いので JVM 側に置いている
 * （`:shared:data` の `InstantConverterTimeZoneTest` と同じ理由）。
 * iOS の actual は文字列を見ずに済む不変条件だけを `ShortDateIosTest` が押さえる。
 *
 * **期待値の文字列は JDK が持つ CLDR の版に依存する**（`ja-JP` は `y/MM/dd`、`en-US` は `M/d/yy`）。
 * JDK を上げてここが落ちたときは、実装の退行ではなく CLDR が変わっただけの可能性を先に疑うこと。
 */
class ShortDateTest {

    private lateinit var originalTimeZone: TimeZone
    private lateinit var originalLocale: Locale

    @BeforeTest
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        Locale.setDefault(Locale.JAPAN)
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun ロケールに合わせた短い形式で書く() {
        val actual = formatShortDate(Instant.parse("2026-08-30T03:00:00Z"))

        assertEquals("2026/08/30", actual)
    }

    @Test
    fun ロケールが変われば書式も変わる() {
        // 固定書式に「揃える」変更が入るとここが落ちる
        Locale.setDefault(Locale.US)

        val actual = formatShortDate(Instant.parse("2026-08-30T03:00:00Z"))

        assertEquals("8/30/26", actual)
    }

    @Test
    fun 端末のタイムゾーンで日付が決まる() {
        // UTC では 8/29 23:00、JST では 8/30 08:00。UTC 固定にすると落ちる
        val actual = formatShortDate(Instant.parse("2026-08-29T23:00:00Z"))

        assertEquals("2026/08/30", actual)
    }

    @Test
    fun 端末のタイムゾーンが変われば日付も変わる() {
        // 上と同じ instant をロサンゼルスで読むと前日になる。
        // この 1 件が無いと「JST 固定」の実装が 3 件とも緑で通ってしまい、
        // 「UTC でない」ことしか示せていなかった
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        val actual = formatShortDate(Instant.parse("2026-08-29T23:00:00Z"))

        assertEquals("2026/08/29", actual)
    }
}
