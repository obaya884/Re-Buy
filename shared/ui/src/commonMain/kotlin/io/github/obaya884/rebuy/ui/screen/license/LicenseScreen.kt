package io.github.obaya884.rebuy.ui.screen.license

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIconButton
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold

@Composable
fun LicenseScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    ReBuyAppScaffold(
        topBarTitle = "ライセンス",
        topBarNavigationIcon = {
            ReBuyAppBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = { navigator.goBack() },
                modifier = Modifier.testTag(TestTags.BACK_BUTTON)
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        // 一覧の元データは AboutLibraries の Gradle プラグインが commonMain のリソースへ
        // 直接書き出している（配線は shared/ui/build.gradle.kts）。
        // 読み終わるまで libraries は null で、その間 LibrariesContainer は空を描く
        val libraries by produceLibraries {
            Res.readBytes(ABOUT_LIBRARIES_PATH).decodeToString()
        }
        // json は両プラットフォーム分を持つので、このプラットフォームに載る依存だけへ絞る
        val platformLibraries = remember(libraries) { libraries?.forCurrentPlatform() }

        LibrariesContainer(
            libraries = platformLibraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * ライセンス一覧の元データの、リソース内のパス。
 *
 * `Res.readBytes` は文字列で引くので、`Res.string.xxx` と違ってタイプミスをコンパイラが
 * 止められない——**この画面を開いたときに初めて落ちる**。公開しているのは
 * `LicenseLibrariesTest`（instrumented）にこの定数を参照させるため。書き写させると、
 * 書き写しどうしが一致するだけで実装のパス誤りを止められなくなる。
 */
const val ABOUT_LIBRARIES_PATH = "files/aboutlibraries.json"
