package io.github.obaya884.rebuy.ui.screen.license

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS では一覧の中身がまだ無い。AboutLibraries の読み込みを composeResources 経由に移す
 * ステップ 14 で、この `actual` は消えて共通の 1 実装になる。
 *
 * 空なのは Android 側も同じ（落とし穴 17 で、ステップ 5 から一覧が 0 件になっている）。
 * 画面への遷移と戻りは [LicenseScreen] が共通で持っているので、そこは Android と同じに動く。
 */
@Composable
actual fun LicenseContent(modifier: Modifier) {
}
