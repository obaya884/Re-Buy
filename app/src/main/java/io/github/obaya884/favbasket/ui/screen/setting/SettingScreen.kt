package io.github.obaya884.favbasket.ui.screen.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.obaya884.favbasket.R
import io.github.obaya884.favbasket.ui.FavBasketAppScaffold
import io.github.obaya884.favbasket.ui.Screen

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
                    contentDescription = "Localized description"
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
            // アプリについて、利用規約、プライバシーポリシー、ライセンス、バージョン、お問い合わせ、レビュー、アプリをシェア
            SettingScreenItem(
                text = stringResource(id = R.string.setting_row_item_edit)
            ) {
                navController.navigate(Screen.ItemEdit.route)
            }
            SettingScreenItem(
                text = stringResource(id = R.string.setting_row_category_edit)
            ) {
                navController.navigate(Screen.CategoryEdit.route)
            }
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
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                textAlign = TextAlign.Start
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                modifier = Modifier.align(Alignment.Bottom),
                contentDescription = "Localized description"
            )
        }
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp
        )
    }
}
