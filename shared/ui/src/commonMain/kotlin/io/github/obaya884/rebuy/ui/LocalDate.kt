package io.github.obaya884.rebuy.ui

import kotlin.time.Instant

/**
 * 端末のタイムゾーンでの年月日。**プラットフォームに委ねるのはここだけ**にして、
 * 書式は共通側で組む（画面ごとに違う書式を、同じ日付の解釈の上に載せるため）。
 */
internal data class LocalDateFields(val year: Int, val month: Int, val day: Int)

internal expect fun localDateFields(instant: Instant): LocalDateFields

/**
 * 一覧に出す「前回 M/D」の日付（画面定義書 §2）。
 *
 * `formatShortDate` と違って**ロケールに従わない**。一覧の行は狭く、年を出さずに
 * 月と日だけを同じ並びで見せたいので、書式を M/D に固定する（§2 が定めている）。
 * 揃えたいのは桁ではなく読み方なので、ゼロ埋めはしない。
 */
internal fun formatMonthDay(instant: Instant): String =
    with(localDateFields(instant)) { "$month/$day" }

/**
 * 編集シートに出す「最終購入日: YYYY-MM-DD」の日付（画面定義書 §2）。
 *
 * こちらは**ゼロ埋めする**——年月日を並べる形は桁が揃っているほうが読みやすく、
 * §2 も YYYY-MM-DD と桁を指定している。
 */
internal fun formatFullDate(instant: Instant): String = with(localDateFields(instant)) {
    "$year-${month.pad()}-${day.pad()}"
}

private fun Int.pad(): String = toString().padStart(2, '0')
