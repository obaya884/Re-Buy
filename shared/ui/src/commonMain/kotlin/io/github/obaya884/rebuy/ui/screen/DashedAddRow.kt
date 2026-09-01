package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/**
 * 一覧の末尾に置く「＋ ◯◯」の行（04 の 05 への入口、09 の追加）。
 *
 * **破線で囲う**（画面 04・09）——中身の行と同じ面に見えると、押すと中身が変わるものに見える。
 */
@Composable
fun DashedAddRow(label: String, testTag: String, onTap: () -> Unit) {
    val outline = ReBuyTheme.colors.muted
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = outline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            // リップルと破線の角丸を 1 か所から引く。別々に書くと枠と波紋の形がずれる
            .clip(RoundedCornerShape(CORNER))
            .clickable(role = Role.Button, onClick = onTap)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx())
                        )
                    ),
                    cornerRadius = CornerRadius(CORNER.toPx())
                )
            }
            .padding(vertical = 16.dp)
            .testTag(testTag)
    )
}

/** 破線の刻みと角丸。密度で見え方が変わらないよう dp で持つ。 */
private val DASH_ON = 6.dp
private val DASH_OFF = 4.dp
private val CORNER = 12.dp
