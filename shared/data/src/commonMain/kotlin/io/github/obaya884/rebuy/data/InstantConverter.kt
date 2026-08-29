package io.github.obaya884.rebuy.data

import androidx.room.TypeConverter
import kotlin.time.Instant

/**
 * 日時をエポックミリ秒（`INTEGER`）として保存する。
 *
 * この形式を選んだ理由は CLAUDE.md「アーキテクチャ / データ層」にある。
 */
object InstantConverter {
    /**
     * エポックミリ秒を [Instant] に変換する。
     *
     * [Long] で表せる値はすべて読める。
     */
    @TypeConverter
    fun toInstant(epochMilli: Long): Instant = Instant.fromEpochMilliseconds(epochMilli)

    /**
     * [Instant] をエポックミリ秒に変換する。
     *
     * ミリ秒未満は切り捨てられる（0 方向ではなく過去方向）。
     *
     * オーバーフローは起きない。[Instant] が表現できる最も遠い未来・過去
     * （[Instant.DISTANT_FUTURE] / [Instant.DISTANT_PAST]）でも [Long] に収まる。
     */
    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilliseconds()
}
