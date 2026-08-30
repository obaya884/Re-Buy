package io.github.obaya884.rebuy.ui

import kotlin.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * `NSDateFormatter` の `short` は Android の `FormatStyle.SHORT` と同じ位置づけで、端末の
 * ロケールと地域設定に従う。Android と同じ文字列になることは狙っていない（憲章 C-5）。
 *
 * `timeZone` を指定しないのは既定の端末のタイムゾーンを使うため。`ShortDateIosTest` が
 * それとミリ秒→秒の変換の両方を固定している。
 */
internal actual fun formatShortDate(instant: Instant): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterShortStyle
        timeStyle = NSDateFormatterNoStyle
    }
    val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
    return formatter.stringFromDate(date)
}
