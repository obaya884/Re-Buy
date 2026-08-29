package io.github.obaya884.rebuy.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField


// https://qiita.com/sdkei/items/6acf34f081ddde59ce11
// https://github.com/sdkei/DateTimeInLocalDatabaseOfAndroid/blob/develop/app/src/main/java/io/github/sdkei/datetimeinlocaldatabase/DateTimeConverter.kt
object InstantDateFormatStringConverter {
    /**
     * [String] を [Instant] に変換する。
     */
    @TypeConverter
    fun toInstant(from: String): Instant {
        return formatter.parse(from, Instant::from)
    }

    /**
     * [Instant] を [String] に変換する。
     *
     * 秒未満の値は切り捨てられる。
     *
     * @throws DateTimeParseException  0 年未満や 10,000 年以上の場合。
     */
    @TypeConverter
    fun toString(from: Instant): String {
        return formatter.format(from)
    }

    /**
     * `YYYY-MM-DD HH:MM:SS` 形式の日時フォーマッター。
     *
     * 0 年未満や 10,000 年以上はエラーになる。
     *
     * DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss") で生成したものだと
     * 0 年未満や 10,000 年以上でもエラーにならないため、
     * DateTimeFormatterBuilder で生成する。
     */
    private val formatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            // ^ 4 桁を超える場合や負の場合はエラーとする。
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter()
            .withZone(ZoneOffset.UTC) // タイムゾーンを UTC にする。
}
