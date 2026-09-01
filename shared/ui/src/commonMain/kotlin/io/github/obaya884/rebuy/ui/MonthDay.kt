package io.github.obaya884.rebuy.ui

import kotlin.time.Instant

/**
 * 一覧に出す「前回 M/D」の日付（画面定義書 §2）。**端末のタイムゾーンで日付に落とす。**
 *
 * `formatShortDate` と違って**ロケールに従わない**。一覧の行は狭く、年を出さずに
 * 月と日だけを同じ並びで見せたいので、書式を M/D に固定する（§2 が定めている）。
 * 揃えたいのは桁ではなく読み方なので、ゼロ埋めはしない。
 */
internal expect fun formatMonthDay(instant: Instant): String
