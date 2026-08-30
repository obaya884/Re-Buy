package io.github.obaya884.rebuy.ui.screen.license

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.obaya884.rebuy.ui.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun LicenseContent(modifier: Modifier) {
    val libraries by produceLibraries(R.raw.aboutlibraries)

    LibrariesContainer(libraries, modifier)
}
