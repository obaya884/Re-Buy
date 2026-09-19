package io.github.obaya884.rebuy.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 外枠（iOS の SwiftUI のナビゲーションバー）が中身に**被さっている**高さ。
 *
 * **既定は 0**。Android は本物の `TopAppBar` を持ち、中身の上に被さらないので 0 のまま——
 * **Android 用のコードを 1 行も書かない**のがこの形の眼目。実値を渡すのは
 * iOS の入口（`ReBuyIosApp`）ただ 1 か所で、そこには分岐を置かない。
 *
 * **`static` にしない。** 描き手（`LocalReBuyAppBarRenderer`）と違い、この値は回転や OS の版で
 * バー高が変われば動く。
 *
 * **テストはここに任意の値を注入して本番 iOS の構成を再現できる**——`runComposeUiTest` に
 * safe area は入らないが、本番の構成は「描かない描き手 ＋ 非ゼロの上端」の 2 つでしかない。
 */
internal val LocalBarOverlapTop = compositionLocalOf { 0.dp }

/**
 * `Scaffold` が中身へ渡す inset の辺。**上端は渡さない**——被さっているぶんは
 * [ReBuyContentPadding.barOverlapTop] として手渡し、一覧が内側に持つ。
 *
 * **下端と左右は落とさないこと。** 下端を落とすと **Android でも**下部 CTA が
 * ホームインジケータを避けなくなる（`docs/仕様/13_画面定義書.md` §6）。
 */
internal val ContentInsetSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom

/**
 * 画面が受け取る余白。**上端が 2 つに割れている**のが特徴で、[scaffold] は `Scaffold` が渡す枠、
 * [barOverlapTop] は**外枠が被さっているぶん**。
 *
 * 被さっているぶんの受け取り方は 2 通りあり、**画面はどちらかを選ぶ**（`docs/仕様/13_画面定義書.md` §6）。
 *
 * - [insideScroll]: スクロールの**内側**に持つ。行がバーの背後を通る（04・09・ライセンス）
 * - [Modifier.belowBar]: **外側**で足す。バーの下から始まる（01・07・08。一覧が上端まで届かない画面）
 *
 * どちらも **Android では 0 が足される**ので、今までと同じ値になる。
 */
data class ReBuyContentPadding(
    val scaffold: PaddingValues,
    val barOverlapTop: Dp,
) {
    /**
     * スクロールの内側に持つ余白（一覧の `contentPadding` や、スクロールの内側の `padding`）。
     * **上端にだけ [barOverlapTop] を足す**——下端に足すと 01・04 の下部 CTA が
     * ホームインジケータを避ける余白（§6）が動く。
     */
    fun insideScroll(all: Dp): PaddingValues =
        PaddingValues(start = all, top = all + barOverlapTop, end = all, bottom = all)
}

/**
 * 外枠の下から中身を始める。**流す一覧を持たない画面だけが使う**（01・07・08。§6）。
 *
 * **名前を付けているのは、手で重ねると誤りが書けるから**——`padding(barOverlapTop)` と
 * 全辺に足すと、下部 CTA がホームインジケータを避ける余白まで動く。
 */
fun Modifier.belowBar(contentPadding: ReBuyContentPadding): Modifier =
    padding(contentPadding.scaffold).padding(top = contentPadding.barOverlapTop)

@Composable
fun ReBuyAppScaffold(
    appBar: ReBuyAppBarState,
    snackbarHostState: SnackbarHostState,
    content: @Composable (ReBuyContentPadding) -> Unit,
) {
    Scaffold(
        topBar = { LocalReBuyAppBarRenderer.current.Render(appBar) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // **上端だけ渡さない。** 被さっているぶんは [ReBuyContentPadding.barOverlapTop] として
        // 画面へ手渡し、一覧が内側に持つ。**Android には影響しない**——`Scaffold` はバーがあれば
        // 測った高さを使い、この値の top を 1 度も読まない（バーが 0×0 のときだけ読む）。
        //
        // **この 1 行だけはテストで測れない**（`runComposeUiTest` は safe area が 0）。
        // 外すと**実機の iOS で上端が二重になる**——外枠からも一覧の内側からも同じ高さを
        // 受け取るので、シミュレータの実物で見るしかない
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(ContentInsetSides)
    ) { innerPadding ->
        content(ReBuyContentPadding(innerPadding, LocalBarOverlapTop.current))
    }
}
