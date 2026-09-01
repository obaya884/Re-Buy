package io.github.obaya884.rebuy.ui

import java.time.ZoneId
import kotlin.time.Instant
import java.time.Instant as JavaInstant

internal actual fun localDateFields(instant: Instant): LocalDateFields {
    val date = JavaInstant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return LocalDateFields(date.year, date.monthValue, date.dayOfMonth)
}
