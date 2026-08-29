package io.github.obaya884.rebuy.data

import androidx.room.TypeConverter
import java.time.Instant

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
    fun toInstant(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)

    /**
     * [Instant] をエポックミリ秒に変換する。
     *
     * ミリ秒未満は切り捨てられる（0 方向ではなく過去方向）。
     *
     * @throws ArithmeticException エポックミリ秒が [Long] に収まらない場合。
     */
    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()
}
