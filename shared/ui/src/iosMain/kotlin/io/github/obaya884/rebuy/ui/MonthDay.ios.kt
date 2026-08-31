package io.github.obaya884.rebuy.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.time.Instant
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * 書式を持たない（M/D は自分で組む）ので `NSDateFormatter` ではなく `NSCalendar` を使う。
 * 既定のカレンダーは端末のタイムゾーンに従う。
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun formatMonthDay(instant: Instant): String {
    val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = date
    )
    return "${components.month}/${components.day}"
}
