package io.github.obaya884.rebuy.ui

import java.time.ZoneId
import kotlin.time.Instant
import java.time.Instant as JavaInstant

internal actual fun formatMonthDay(instant: Instant): String {
    val date = JavaInstant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return "${date.monthValue}/${date.dayOfMonth}"
}
