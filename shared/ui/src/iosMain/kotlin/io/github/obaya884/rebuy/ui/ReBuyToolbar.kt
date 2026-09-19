package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon

/**
 * SwiftUI のツールバーに出すもの。**Swift が見る契約**（`docs/仕様/13_画面定義書.md` §6）。
 *
 * `ReBuyAppBarState` をそのまま渡さないのは 2 つの理由による。**(1) 色が要る**——§6 は「色は ARGB で
 * 渡す」と定めるが、accent は**画面が決めるものではない**ので `ReBuyAppBarState` の契約に合わない。
 * **(2) 押したときの動作を最新へ転送する必要がある**——詳細は [PublishingAppBarRenderer]。
 *
 * **`data class` にしない。** ラムダを持つので `equals` が参照比較になり、値として比べられる
 * 見かけだけが付く。**同じ内容かどうかは [ReBuyToolbarKey] が判定する**ので、この型に等値は要らない。
 */
class ReBuyToolbar internal constructor(
    /** 見出し。 */
    val title: String,
    /** 添える数（01 の「全 n 件」・04 の「x / n」）。null なら出さない。 */
    val countText: String?,
    /**
     * 添える数に付ける `accessibilityIdentifier`。**01 は印を持たない**ので null
     * （[ReBuyToolbarAction.accessibilityId] と違って省略できる）。
     */
    val countAccessibilityId: String?,
    /** 主要色。`0xAARRGGBB`。**テーマ（3 パレット × 明暗）に追従する**（§5）。 */
    val accentArgb: Long,
    /** 戻る。**「1 つ戻る」とは限らず、何をするかは画面が決める**（§6）。null なら ← を出さない。 */
    val onBack: (() -> Unit)?,
    /** 押せるもの。**並びは画面の左から右**で、戻るは [onBack] が持つのでここには入らない。 */
    val actions: List<ReBuyToolbarAction>,
)

/** [ReBuyToolbar] の押せるもの 1 つ。 */
class ReBuyToolbarAction internal constructor(
    /** **種類だけを渡し、SF Symbols への対応表は Swift 側が持つ**（§6）。 */
    val icon: ReBuyAppBarIcon,
    /** `accessibilityIdentifier` に入れる印。 */
    val accessibilityId: String,
    val onClick: () -> Unit,
)
