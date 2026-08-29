package io.github.obaya884.favbasket.ui.screen.setting

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.obaya884.favbasket.BuildConfig
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.ui.Screen
import io.github.obaya884.favbasket.ui.screen.FavBasketAppScaffold

@Composable
fun SettingScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    FavBasketAppScaffold(
        topBarTitle = stringResource(id = R.string.setting_title),
        topBarNavigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
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
            // OSSライセンス
            SettingScreenItem("ライセンス") { navController.navigate(Screen.License.route) }
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
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    HorizontalDivider(
        color = Color.LightGray,
        thickness = 1.dp
    )
}
