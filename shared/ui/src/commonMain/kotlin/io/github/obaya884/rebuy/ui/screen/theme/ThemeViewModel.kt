package io.github.obaya884.rebuy.ui.screen.theme

import androidx.lifecycle.ViewModel
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.domain.ThemeRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * テーマ画面（画面 08）。**選んだ瞬間に反映する**ので保存ボタンは無い。
 *
 * 選択はアプリ全体の見た目を変えるため、画面を離れても同じものが要る。
 * [palette] は `ThemeRepository` の状態をそのまま流し、`ReBuyApp` も同じものを見る。
 */
class ThemeViewModel(private val themeRepository: ThemeRepository) : ViewModel() {

    val palette: StateFlow<ThemePalette> = themeRepository.palette

    fun select(palette: ThemePalette) {
        themeRepository.select(palette)
    }
}
