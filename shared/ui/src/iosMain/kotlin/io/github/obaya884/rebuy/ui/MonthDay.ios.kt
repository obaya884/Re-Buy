package io.github.obaya884.rebuy.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.time.Instant
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * 書式を持たない（M/D は自分で組む）ので `NSDateFormatter` ではなく `NSCalendar` を使う。
 *
 * **暦法はグレゴリオ暦を名指しする。** `currentCalendar` は端末のロケールに従うので、
 * 和暦やヒジュラ暦の設定では月日そのものが変わってしまう。タイムゾーンは既定のまま
 * （端末の設定に従う）。
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun formatMonthDay(instant: Instant): String {
    val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
    val calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)!!
    val components = calendar.components(
        NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = date
    )
    return "${components.month}/${components.day}"
}
