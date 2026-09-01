package io.github.obaya884.rebuy.ui.screen.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.Screen
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.VERSION_NAME
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.*
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    ReBuyAppScaffold(
        topBarTitle = stringResource(Res.string.setting_title),
        topBarNavigationIcon = {
            IconButton(
                modifier = Modifier.testTag(TestTags.BACK_BUTTON),
                onClick = { navigator.goBack() }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            // アプリについて
            // 利用規約
            SettingScreenItem("利用規約") { }
            // プライバシーポリシー
            SettingScreenItem("プライバシーポリシー") { }
            // 暫定: カテゴリの管理（09）は F-012。プールのアプリバーから外したのでここに置く
            SettingScreenItem(stringResource(Res.string.setting_row_category_edit)) {
                navigator.navigate(Screen.CategoryEdit)
            }
            // テーマ
            SettingScreenItem(stringResource(Res.string.theme_title)) {
                navigator.navigate(Screen.Theme)
            }
            // OSSライセンス
            SettingScreenItem("ライセンス") { navigator.navigate(Screen.License) }
            // お問い合わせ・機能リクエスト
            SettingScreenItem("お問い合わせ・機能リクエスト") { }
            // レビュー(1stリリース後)
            // アプリをシェア(1stリリース後)
            // バージョン
            VersionCell()
        }
    }
}

@Composable
fun SettingScreenItem(
    text: String,
    onTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
    HorizontalDivider(
        color = Color.LightGray,
        thickness = 1.dp
    )
}

@Composable
fun VersionCell() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "バージョン",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = VERSION_NAME,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    HorizontalDivider(
        color = Color.LightGray,
        thickness = 1.dp
    )
}
