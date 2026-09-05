package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
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
 * 一覧の 1 行（01・04・05・09 で共通）。
 *
 * [highlighted] が立っている行は面の色を変え、中身が置く ✓ や添え文言と合わせて
 * **2 通りで分かる**ようにする。
 *
 * 高さは行が持つ（`docs/仕様/13_画面定義書.md` §5）。**`content` 側で高さを作る必要はない。**
 *
 * @param onTap タップの行き先。**null なら面そのものは押せない**——09 は行のタップに
 *   意味が無く、押せるのはハンドル（ドラッグ）と名前（長押し）だけ
 * @param role 01 と 04 は ✓ の付け外しなので [Role.Checkbox]、
 *   選んで閉じるだけの 05 は [Role.Button]
 * @param enabled false のときは押せない。**リップルも読み上げの操作も出さない**ので、
 *   「押せそうに見えて何も起きない」行にならない（05 の追加済み）
 * @param onLongPress 長押しの行き先。無い画面（04・05・09）は null
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReBuyRowCard(
    highlighted: Boolean,
    onTap: (() -> Unit)?,
    testTag: String,
    role: Role = Role.Checkbox,
    enabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) ReBuyTheme.colors.accentSoft else ReBuyTheme.colors.card
        ),
        modifier = modifier
            .fillMaxWidth()
            // **clip を先に置く**。後ろに置くとリップルがカードの角丸からはみ出る
            .clip(CardDefaults.shape)
            .then(
                if (onTap == null) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        role = role,
                        onClick = onTap,
                        onLongClick = onLongPress
                    )
                }
            )
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // **最低高はここに置く**。`Card` の外側に置くと、高さが伸びるのは Card の中の
            // `Column`（`Arrangement.Top`）で、この `Row` は元の高さのまま上端に貼り付く
            modifier = Modifier
                .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
                .padding(horizontal = 16.dp, vertical = ROW_PADDING),
            content = content
        )
    }
}

/**
 * 一覧に並ぶものの内側の余白（上下）。[ROW_MIN_HEIGHT] の内訳でもあるので 1 か所から引く。
 *
 * **[ROW_MIN_HEIGHT] より先に宣言すること**——トップレベルの `val` は宣言順に初期化されるので、
 * 後ろに置くと `Dp` の既定値 0dp で計算され、最低高が黙って 24dp 足りなくなる。
 */
internal val ROW_PADDING = 12.dp

/**
 * 一覧に並ぶものの最低高。**いちばん高い行（2 段の 01）に他を合わせた値**で、
 * 内訳は `bodyLarge` の行高 24sp ＋ `labelMedium` の行高 16sp ＋ 上下の余白。
 *
 * **一覧の末尾に並ぶ [DashedAddRow] もここから引く**——同じ一覧に混ざるので、
 * 行だけ揃えても末尾で段差が出る（`docs/仕様/13_画面定義書.md` §5）。
 *
 * 行高は sp なので、**文字を大きくする設定では 2 段の行だけがこの値を超えて再び割れる**。
 * 揃えたいなら 4 画面とも 2 段ぶんの高さを持つ形に変えることになる（FB-06 の残したもの）。
 */
internal val ROW_MIN_HEIGHT = 24.dp + 16.dp + ROW_PADDING * 2
