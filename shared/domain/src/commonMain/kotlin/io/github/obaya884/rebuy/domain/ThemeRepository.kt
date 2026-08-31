package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 配色の 3 案（画面定義書 §5）。**既定は [AI]。**
 *
 * 名前をそのまま保存するので、**追加はしてよいが既存の名前は変えない**——変えると
 * 選択済みの端末が既定へ戻る。
 */
enum class ThemePalette {
    WAKABA,
    AI,
    KAKI;

    companion object {
        val DEFAULT = AI
    }
}

/**
 * テーマの選択を持つ（データモデル定義書 §9）。DB ではなく端末内のキーバリューに置く。
 *
 * 選択は**即時に反映する**（画面 08 に保存ボタンは無い）ので、書き込みと同じ呼び出しで
 * [palette] を更新する。明暗は OS 設定に追従するのでここでは持たない。
 */
class ThemeRepository(private val settingsStore: SettingsStore) {

    private val _palette = MutableStateFlow(read())

    val palette: StateFlow<ThemePalette> = _palette.asStateFlow()

    fun select(palette: ThemePalette) {
        settingsStore.putString(KEY, palette.name)
        _palette.value = palette
    }

    /** 未選択のときと、**知らない名前が入っていたとき**は既定に倒す。 */
    private fun read(): ThemePalette {
        val stored = settingsStore.getString(KEY) ?: return ThemePalette.DEFAULT
        return ThemePalette.entries.firstOrNull { it.name == stored } ?: ThemePalette.DEFAULT
    }

    private companion object {
        /** アプリ内で一意にする（iOS は `NSUserDefaults` の共有領域に載る）。 */
        const val KEY = "rebuy.theme.palette"
    }
}
