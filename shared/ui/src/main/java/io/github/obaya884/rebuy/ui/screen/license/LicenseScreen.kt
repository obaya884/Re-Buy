package io.github.obaya884.rebuy.ui.screen.license

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.obaya884.rebuy.ui.R
import io.github.obaya884.rebuy.ui.TestTags
import io.github.obaya884.rebuy.ui.navigation.Navigator
import io.github.obaya884.rebuy.ui.screen.ReBuyAppScaffold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LicenseScreen(
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    val libraries by produceLibraries(R.raw.aboutlibraries)

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
        LibrariesContainer(
            libraries,
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
