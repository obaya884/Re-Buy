package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/**
 * 一覧の 1 行（01 と 04 で共通）。
 *
 * **タップは ✓ の付け外し**——01 はカゴの出し入れ、04 はチェック——なので、
 * どちらも [Role.Checkbox] を名乗る。[highlighted] が立っている行は面の色を変え、
 * 中身が置く ✓ と合わせて**2 通りで分かる**ようにする。
 *
 * @param onLongPress 長押しの行き先。無い画面（04）は null
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReBuyRowCard(
    highlighted: Boolean,
    onTap: () -> Unit,
    testTag: String,
    onLongPress: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) ReBuyTheme.colors.accentSoft else ReBuyTheme.colors.card
        ),
        modifier = Modifier
            .fillMaxWidth()
            // **clip を先に置く**。後ろに置くとリップルがカードの角丸からはみ出る
            .clip(CardDefaults.shape)
            .combinedClickable(role = Role.Checkbox, onClick = onTap, onLongClick = onLongPress)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )
    }
}
