package io.github.obaya884.rebuy.ui.screen.license

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold

@Composable
fun LicenseScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    ReBuyAppScaffold(
        topBarTitle = "ライセンス",
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
        LicenseContent(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * ライセンス一覧の中身。
 *
 * ここだけ `expect/actual` なのは、AboutLibraries の読み込みが
 * `produceLibraries(R.raw.aboutlibraries)` という Android 専用 API だから。
 * TopAppBar と戻るボタンは共通なので [LicenseScreen] に置いてある——
 * 画面ごと分けると、片方だけ直す事故が起きる（instrumented は Android しか見ない）。
 *
 * **ステップ 14 で composeResources 経由に移すと、この `expect/actual` は消えて共通の 1 実装になる。**
 */
@Composable
expect fun LicenseContent(modifier: Modifier)
