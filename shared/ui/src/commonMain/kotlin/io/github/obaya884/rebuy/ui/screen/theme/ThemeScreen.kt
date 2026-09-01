package io.github.obaya884.rebuy.ui.screen.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.domain.ThemePalette
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.theme_ai
import io.github.obaya884.rebuy.ui.resources.theme_kaki
import io.github.obaya884.rebuy.ui.resources.theme_title
import io.github.obaya884.rebuy.ui.resources.theme_wakaba
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import io.github.obaya884.rebuy.ui.theme.reBuyColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * テーマ（画面 08）。行＝色見本＋名前＋選択中の ✓。**タップで即時反映**し、保存ボタンは無い。
 *
 * 明暗は OS 設定に追従するので、ここには明暗の切り替えを置かない（画面定義書 画面 08）。
 */
@Composable
fun ThemeScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = koinViewModel<ThemeViewModel>()
    val selected by viewModel.palette.collectAsState()

    ReBuyAppScaffold(
        topBarTitle = stringResource(Res.string.theme_title),
        topBarNavigationIcon = {
            IconButton(
                modifier = Modifier.testTag(TestTags.BACK_BUTTON),
                onClick = { navigator.goBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding)) {
            ThemePalette.entries.forEach { palette ->
                PaletteRow(
                    palette = palette,
                    isSelected = palette == selected,
                    onClick = { viewModel.select(palette) }
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            // 単一選択であることをセマンティクスに乗せる。✓ は見た目の担当
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Swatch(palette)
        Text(
            text = stringResource(palette.labelResource()),
            style = MaterialTheme.typography.bodyLarge,
            color = ReBuyTheme.colors.ink,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = ReBuyTheme.colors.accent
            )
        }
    }
}

/**
 * 色見本。**そのパレットの色で塗る**ので、選んでいないパレットの色もその場で分かる。
 * いまの明暗に合わせた見え方を出したいので、明暗だけは現在のテーマから引く。
 */
@Composable
private fun Swatch(palette: ThemePalette) {
    val colors = reBuyColors(palette, darkTheme = ReBuyTheme.colors.isDark)
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(colors.accent, CircleShape)
            .border(2.dp, colors.accentSoft, CircleShape)
    )
}
