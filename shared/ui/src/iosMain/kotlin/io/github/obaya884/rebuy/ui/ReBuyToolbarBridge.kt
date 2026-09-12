package io.github.obaya884.rebuy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toArgb
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarIcon
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarRenderer
import io.github.obaya884.rebuy.ui.screen.ReBuyAppBarState
import io.github.obaya884.rebuy.ui.theme.ReBuyTheme

/**
 * バーの内容のうち**値として比べられる部分だけ**を集めた鍵。
 *
 * **[ReBuyAppBarState] そのものは鍵に使えない**——`onBack` と `actions` がラムダを持つので
 * `equals` が参照比較に落ち、内容が同じでも「変わった」と判定される（[ReBuyAppBarState] の KDoc）。
 * 素直に繋ぐと**再コンポーズのたびに publish** し、SwiftUI のツールバーが毎フレーム作り直される。
 *
 * **[accentArgb] を含めるのが要**。落とすと**パレットを変えてもツールバーの色が変わらない**。
 *
 * **項目を足すときは 4 か所そろえる**——この型・[ReBuyToolbar]・`toKey`・`toToolbar`。
 * 鍵から落ちたものは、**変わっても渡し直されない**（accent で恐れたのと同じ形）。
 *
 * **網が要るのは、単独で動ける項目だけ。** [title] が必ず一緒に動く [hasBack]、[countText] が
 * 必ず一緒に動く [countAccessibilityId]、[ActionKey.accessibilityId] が必ず一緒に動く
 * [ActionKey.icon] は、鍵から落としても落ちるテストが無い——**共変する相手がいるため**で、
 * これは意図した割り切り。項目を足したら、まずここに当てはまるかを見ること。
 */
internal data class ReBuyToolbarKey(
    val title: String,
    val countText: String?,
    val countAccessibilityId: String?,
    /** ← を出すかどうか。**ラムダ本体は入れない**（比べられない）。 */
    val hasBack: Boolean,
    val actions: List<ActionKey>,
    val accentArgb: Long,
) {
    data class ActionKey(val icon: ReBuyAppBarIcon, val accessibilityId: String)
}

/**
 * バーを [delegate] に描かせたうえで、内容が変わったときだけ Swift へ渡す。
 *
 * **本番の [delegate] は `NoAppBarRenderer`**（バーは SwiftUI が描く）。描かせる余地を残しているのは、
 * テストで Material を挿すと「バーを押せるまま公開の挙動だけを見る」ができるため。
 *
 * **publish は [SideEffect] から出す**——コンポジションが成立してから走るので、組み立ての途中の値を
 * 外へ出さない（`AppBarStateIosTest` の記録用の描き手と同じ作法）。ただし**この `Render` は
 * `Scaffold` の `topBar` スロット（subcompose）の中**なので、成立の時点がレイアウトの最中に
 * 来ることがある。**Step 4 で SwiftUI の状態を同期更新するときに実測すること**。
 *
 * **Swift のコールバックの中から Kotlin を同期で呼び返さないこと**——publish は Compose の
 * メインスレッドから同期で呼ばれるので、その場で再コンポーズを起こすと composition に再入する。
 */
internal class PublishingAppBarRenderer(
    private val delegate: ReBuyAppBarRenderer,
    private val publish: (ReBuyToolbar) -> Unit,
) : ReBuyAppBarRenderer {

    private var lastKey: ReBuyToolbarKey? = null

    @Composable
    override fun Render(appBar: ReBuyAppBarState) {
        delegate.Render(appBar)

        // 配色の正は画面定義書 §5 の 1 か所。ここは読むだけ（§6「色は ARGB で渡す」）
        val accentArgb = ReBuyTheme.colors.accent.toArgb().toLong() and 0xFFFFFFFF
        val key = appBar.toKey(accentArgb)

        // 鍵が同じでもラムダは差し替わりうるので、最新を読む入れ物越しに渡す（`toToolbar`）
        val latest = rememberUpdatedState(appBar)

        SideEffect {
            if (key != lastKey) {
                lastKey = key
                publish(key.toToolbar { latest.value })
            }
        }
    }
}

private fun ReBuyAppBarState.toKey(accentArgb: Long) = ReBuyToolbarKey(
    title = title,
    countText = count?.text,
    countAccessibilityId = count?.testTag,
    hasBack = onBack != null,
    actions = actions.map { ReBuyToolbarKey.ActionKey(it.icon, it.testTag) },
    accentArgb = accentArgb,
)

/**
 * 鍵に、最新の動作を読むクロージャを添えて Swift へ渡す形にする。
 *
 * [latest] は**呼ぶたびに最新の状態を読む**。渡した時点のラムダを抱えると Swift が古い実装を
 * 持ち続け、**04 の ← が古い画面の離脱確認を呼ぶ**、という黙った不具合になる。
 *
 * 動作は添字で引く。**並びと個数は鍵に入っている**ので、鍵が同じなら添字の指す先も同じ。
 * 範囲外は何もしない——鍵が変われば渡し直すので、そこへ来るのは
 * **もう存在しないボタンを押した**ときだけ。
 */
private fun ReBuyToolbarKey.toToolbar(latest: () -> ReBuyAppBarState) = ReBuyToolbar(
    title = title,
    countText = countText,
    countAccessibilityId = countAccessibilityId,
    accentArgb = accentArgb,
    onBack = if (hasBack) ({ latest().onBack?.invoke() }) else null,
    actions = actions.mapIndexed { index, action ->
        ReBuyToolbarAction(
            icon = action.icon,
            accessibilityId = action.accessibilityId,
            onClick = { latest().actions.getOrNull(index)?.onClick?.invoke() },
        )
    },
)
