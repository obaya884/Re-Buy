package io.github.obaya884.rebuy.ui

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone
import platform.Foundation.setDefaultTimeZone
import platform.Foundation.timeZoneWithName

/**
 * iOS の [formatShortDate] を、**文字列の中身を見ずに**押さえる。
 *
 * 書式は端末のロケールに委ねるので期待値を持てない（憲章 C-5）。代わりに、ロケールが
 * 何であっても成り立つ関係だけを見る——同じ日の 2 つの時刻は同じ文字列に、別の日なら別の文字列に。
 *
 * これで手書きの 2 か所を捕まえられる。エポックミリ秒を秒へ直す割り算と、
 * 既定タイムゾーンを読んでいること。
 *
 * `NSTimeZone` を差し替える API が common に無いのでここに置いている。
 * **CI は Linux なのでこのテストは回らない**（段 3 のステップ 16 で macOS のジョブを足すまで、
 * 手元の `./gradlew :shared:ui:iosSimulatorArm64Test` が唯一の実行経路）。
 */
class ShortDateIosTest {

    private lateinit var originalTimeZone: NSTimeZone

    @BeforeTest
    fun setUp() {
        originalTimeZone = NSTimeZone.defaultTimeZone
    }

    @AfterTest
    fun tearDown() {
        NSTimeZone.setDefaultTimeZone(originalTimeZone)
    }

    private fun setDefaultZone(name: String) {
        NSTimeZone.setDefaultTimeZone(checkNotNull(NSTimeZone.timeZoneWithName(name)))
    }

    @Test
    fun 端末のタイムゾーンで日付が決まる() {
        setDefaultZone("Asia/Tokyo")

        // UTC では日をまたぐが JST では同じ 8/30。UTC 固定の実装だと別の文字列になる
        assertEquals(
            formatShortDate(Instant.parse("2026-08-30T03:00:00Z")),
            formatShortDate(Instant.parse("2026-08-29T23:00:00Z"))
        )
    }

    @Test
    fun タイムゾーンが変われば日付も変わる() {
        setDefaultZone("UTC")

        // 上と同じ 2 つ。JST 固定の実装だと同じ文字列になってしまう
        assertNotEquals(
            formatShortDate(Instant.parse("2026-08-30T03:00:00Z")),
            formatShortDate(Instant.parse("2026-08-29T23:00:00Z"))
        )
    }

    @Test
    fun ミリ秒を秒に直してから渡している() {
        setDefaultZone("UTC")

        // 同じ日の 01:00 と 02:00。1000 で割り忘れると 41 日ぶん離れて別の日になる
        assertEquals(
            formatShortDate(Instant.parse("2026-08-30T01:00:00Z")),
            formatShortDate(Instant.parse("2026-08-30T02:00:00Z"))
        )
    }
}
