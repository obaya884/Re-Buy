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
     * **[Long] のミリ秒に収まらない日時は、例外ではなく上限・下限に張り付く**
     * （`kotlin.time.Instant` は ±約 10 億年まで表現でき、[Long] のミリ秒（±約 29 万年）を超える）。
     * その場合は往復しても元の日時に戻らない。
     *
     * `Clock.System.now()` から得た値がこの範囲を出ることはないので実害は無いが、
     * 「収まらない値を渡すと落ちる」とは考えないこと。
     */
    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilliseconds()
}
