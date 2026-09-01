package io.github.obaya884.rebuy.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.time.Instant
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * 書式を持たない（並べ方は共通側で組む）ので `NSDateFormatter` ではなく `NSCalendar` を使う。
 *
 * **暦法はグレゴリオ暦を名指しする。** `currentCalendar` は端末のロケールに従うので、
 * 和暦やヒジュラ暦の設定では年月日そのものが変わってしまう。タイムゾーンは既定のまま
 * （端末の設定に従う）。
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun localDateFields(instant: Instant): LocalDateFields {
    val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
    val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)!!
    val components = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = date
    )
    return LocalDateFields(
        year = components.year.toInt(),
        month = components.month.toInt(),
        day = components.day.toInt()
    )
}
