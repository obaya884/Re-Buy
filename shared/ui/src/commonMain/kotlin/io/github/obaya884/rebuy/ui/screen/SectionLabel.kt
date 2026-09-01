package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/** 一覧の中の区切り見出し（04 の「どこでも買えるもの」、05 の各セクション）。 */
@Composable
fun SectionLabel(text: String, testTag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ReBuyTheme.colors.muted,
        modifier = Modifier.padding(top = 8.dp).testTag(testTag)
    )
}
