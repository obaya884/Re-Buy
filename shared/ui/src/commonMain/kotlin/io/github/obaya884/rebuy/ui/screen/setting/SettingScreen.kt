package io.github.obaya884.rebuy.ui.screen.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
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
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.VERSION_NAME
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import io.github.obaya884.rebuy.ui.screen.theme.ThemeViewModel
import io.github.obaya884.rebuy.ui.screen.theme.labelResource
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 設定（画面 07）。プール（01）のアプリバーの ⚙ から開く。
 *
 * **利用規約・プライバシーポリシー・問い合わせは行ごと出さない**（画面 07）。
 * 中身が無い行を置くより、リリース前に必要になった時点で足すほうが迷わない。
 *
 * 現在のテーマ名を出すために 08 と同じ [ThemeViewModel] を見る。**選択は
 * `ThemeRepository` が持つ**ので、読むだけの ViewModel をもう 1 つ作る必要がない。
 */
@Composable
fun SettingScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val palette by themeViewModel.palette.collectAsState()

    ReBuyAppScaffold(
        topBarTitle = stringResource(Res.string.setting_title),
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
            SettingRow(
                label = stringResource(Res.string.setting_row_category_edit),
                testTag = TestTags.SETTING_ROW_CATEGORY_EDIT,
                // 暫定: 09 カテゴリの管理は F-012。いまは旧画面へ入る
                onTap = { navigator.navigate(Screen.CategoryEdit) }
            )
            // 暫定: 「行き先の管理」の行は F-013 で足す。行き先の管理画面がまだ無い
            SettingRow(
                label = stringResource(Res.string.theme_title),
                testTag = TestTags.SETTING_ROW_THEME,
                onTap = { navigator.navigate(Screen.Theme) },
                // 開かなくても今どれを選んでいるか分かるように（画面 07）
                value = stringResource(palette.labelResource())
            )
            SettingRow(
                label = stringResource(Res.string.setting_row_license),
                testTag = TestTags.SETTING_ROW_LICENSE,
                onTap = { navigator.navigate(Screen.License) }
            )

            Text(
                text = stringResource(Res.string.setting_version, VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = ReBuyTheme.colors.muted,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag(TestTags.SETTING_VERSION)
            )
        }
    }
}

/** 1 行。右端は [value] があればその文言、無ければ「開く」ことを示す ＞。 */
@Composable
private fun SettingRow(
    label: String,
    testTag: String,
    onTap: () -> Unit,
    value: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = ReBuyTheme.colors.ink,
            modifier = Modifier.weight(1f)
        )
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = ReBuyTheme.colors.muted,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ReBuyTheme.colors.muted
        )
    }
    HorizontalDivider()
}
