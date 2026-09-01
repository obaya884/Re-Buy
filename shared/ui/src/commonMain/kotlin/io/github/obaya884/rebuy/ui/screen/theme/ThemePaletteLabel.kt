package io.github.obaya884.rebuy.ui.screen.theme

import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.theme_ai
import io.github.obaya884.rebuy.ui.resources.theme_kaki
import io.github.obaya884.rebuy.ui.resources.theme_wakaba
import org.jetbrains.compose.resources.StringResource

/**
 * パレットの表示名。**08 で選ぶときと 07 で今の選択を出すときで同じ語を使う**ので、
 * 対応を 1 か所に置く。
 */
internal fun ThemePalette.labelResource(): StringResource = when (this) {
    ThemePalette.WAKABA -> Res.string.theme_wakaba
    ThemePalette.AI -> Res.string.theme_ai
    ThemePalette.KAKI -> Res.string.theme_kaki
}
