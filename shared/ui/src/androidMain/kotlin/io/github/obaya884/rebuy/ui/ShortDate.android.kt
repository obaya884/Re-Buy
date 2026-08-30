package io.github.obaya884.rebuy.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import java.time.Instant as JavaInstant

/**
 * 保存はエポックミリ秒なので、`java.time` へ渡すのにそこを経由しても表示は変わらない。
 * `FormatStyle.SHORT` と `ZoneId.systemDefault()` はどちらも `ShortDateTest` が固定している。
 */
internal actual fun formatShortDate(instant: Instant): String =
    JavaInstant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
