package io.github.obaya884.rebuy.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import kotlin.time.Instant

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
     * 変換がローカル日時を経由するようになるとホストのタイムゾーンで値がずれるが、
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

    // ---- 書き出し方向（Instant → Long） ----

    @Test
    fun fromInstant_エポックからのミリ秒になる() {
        // JST の 2024-01-01 09:00:00 は UTC では同日 00:00:00
        val instant = Instant.parse("2024-01-01T09:00:00+09:00")

        val actual = InstantConverter.fromInstant(instant)

        assertEquals(1_704_067_200_000L, actual)
    }

    @Test
    fun fromInstant_エポック前は負になる() {
        val actual = InstantConverter.fromInstant(Instant.parse("1969-12-31T23:59:59Z"))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun fromInstant_ミリ秒未満を切り捨てる() {
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(1, 999_999))

        assertEquals(1_000L, actual)
    }

    @Test
    fun fromInstant_エポック前でも切り捨ては過去方向() {
        // エポックの 1 マイクロ秒前。0 方向の切り捨てなら -999 になる
        val actual = InstantConverter.fromInstant(Instant.fromEpochSeconds(-1, 999_999))

        assertEquals(-1_000L, actual)
    }

    @Test
    fun fromInstant_表現できる最も遠い未来もLongに収まる() {
        val actual = InstantConverter.fromInstant(Instant.DISTANT_FUTURE)

        assertEquals(3_093_527_980_800_000L, actual)
    }

    @Test
    fun fromInstant_表現できる最も遠い過去もLongに収まる() {
        val actual = InstantConverter.fromInstant(Instant.DISTANT_PAST)

        assertEquals(-3_217_862_419_200_001L, actual)
    }

    // ---- 読み出し方向（Long → Instant） ----

    @Test
    fun toInstant_エポックからのミリ秒として読む() {
        val actual = InstantConverter.toInstant(1_704_067_200_000L)

        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), actual)
    }

    @Test
    fun toInstant_Longの下限も読める() {
        val actual = InstantConverter.toInstant(Long.MIN_VALUE)

        // -9223372036854775808 ミリ秒 = -9223372036854776 秒 + 192,000,000 ナノ秒
        assertEquals(Instant.fromEpochSeconds(-9_223_372_036_854_776L, 192_000_000), actual)
    }

    @Test
    fun toInstant_Longの上限も読める() {
        val actual = InstantConverter.toInstant(Long.MAX_VALUE)

        assertEquals(Instant.fromEpochSeconds(9_223_372_036_854_775L, 807_000_000), actual)
    }

    // ---- 往復 ----

    @Test
    fun 往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = Instant.parse("2024-06-15T12:34:56.789123456Z")

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(Instant.parse("2024-06-15T12:34:56.789Z"), actual)
    }

    @Test
    fun エポック前でも往復してミリ秒未満の切り捨て以外は変わらない() {
        val original = Instant.parse("1960-06-15T12:34:56.789123456Z")

        val actual = InstantConverter.toInstant(InstantConverter.fromInstant(original))

        assertEquals(Instant.parse("1960-06-15T12:34:56.789Z"), actual)
    }
}
