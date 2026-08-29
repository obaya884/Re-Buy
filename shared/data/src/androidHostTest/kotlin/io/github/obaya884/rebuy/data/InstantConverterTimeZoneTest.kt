package io.github.obaya884.rebuy.data

import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * 変換がタイムゾーンに依存しないことを固定する。
 *
 * 変換がローカル日時を経由するようになるとホストのタイムゾーンで値がずれるが、
 * **TZ=UTC で動く CI ではそのずれが見えない。** JST に寄せて検出できるようにする。
 *
 * [InstantConverterTest] 本体と分けてあるのは、既定タイムゾーンを差し替える API が
 * common に無いため。JVM でしか書けないので `androidHostTest` に置いている。
 */
class InstantConverterTimeZoneTest {

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
    fun 非UTCのホストでもエポックからのミリ秒になる() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00。
        // ローカル日時を経由すると 9 時間ぶんずれる
        val actual = InstantConverter.fromInstant(Instant.parse("2024-01-01T09:00:00+09:00"))

        assertEquals(1_704_067_200_000L, actual)
    }

    @Test
    fun 非UTCのホストでもミリ秒未満の切り捨て以外は往復して変わらない() {
        // ミリ秒未満を持たせる。ローカル日時を経由する実装では、切り捨ての向きが
        // タイムゾーンで変わる壊れ方がありうる
        val original = Instant.parse("2024-06-15T12:34:56.789123456Z")

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(Instant.parse("2024-06-15T12:34:56.789Z"), actual)
    }

    @Test
    fun 非UTCのホストでもエポック直前は負になる() {
        // JST では 1970-01-01 の前後で日付境界がずれる
        val actual = InstantConverter.fromInstant(Instant.parse("1969-12-31T23:59:59Z"))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun 非UTCのホストでもエポックからのミリ秒として読める() {
        // 往復ではなく toInstant 単独。どちら向きが壊れたかを読めるようにする
        val actual = InstantConverter.toInstant(1_704_067_200_000L)

        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), actual)
    }
}
