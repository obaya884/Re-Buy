package io.github.obaya884.rebuy.ui

import kotlin.time.Instant

/**
 * 「最終購入」の日付を、**端末のロケールとタイムゾーン**に従った短い形式で書く。
 *
 * ロケール依存の書式化は stdlib にも kotlinx-datetime にも等価物が無いので、ここだけ
 * プラットフォームに委ねる。書式を固定してはならない——`ShortDateTest` が固定している契約。
 */
internal expect fun formatShortDate(instant: Instant): String
