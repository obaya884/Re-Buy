package io.github.obaya884.rebuy.ui.screen.license

import com.mikepenz.aboutlibraries.Libs

/**
 * 実行中のプラットフォームに対応する、Kotlin ターゲット名の接頭辞。
 * iOS は `iosArm64`（実機）と `iosSimulatorArm64` の 2 枚があるので、完全一致ではなく接頭辞で束ねる。
 */
internal expect val platformTargetPrefix: String

/**
 * ライセンス一覧を、実行中のプラットフォームに載る依存だけへ絞る
 * （設計は docs/仕様/15_アーキテクチャ定義書.md §6）。
 * `targets` が空の entry は絞らず残す——手足しした entry が黙って消えないように。
 *
 * public なのは、instrumented テストと `iosTest` が**画面と同じ絞り**で
 * 一覧の中身を突き合わせるため。
 */
fun Libs.forCurrentPlatform(): Libs = filterByTargetPrefix(platformTargetPrefix)

internal fun Libs.filterByTargetPrefix(prefix: String): Libs = copy(
    // licenses（ライセンス本文の集合）は絞らない。表示は Library 側の licenses を使う
    libraries = libraries.filter { library ->
        library.targets.isEmpty() || library.targets.any { it.startsWith(prefix) }
    }
)
