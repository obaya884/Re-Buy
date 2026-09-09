package io.github.obaya884.rebuy.ui.screen

/**
 * アプリバーに出すもの。**Kotlin が決め、描く側は描くだけ**（`docs/仕様/13_画面定義書.md` §6）。
 *
 * **Compose の slot ではなくデータで持つ。** iOS では外枠が SwiftUI に出るので、
 * 「何を出すか」が Compose の composable のままだと境界を越えられない。
 *
 * **並びは画面の左から右**。[count] を [actions] と分けているのは**置く順が決まっている**ため——
 * 数は必ずアイコンの左に来る（01 は「全 n 件」の右隣に ＋、04 は数がバーの右端）。
 *
 * **[onBack] と [actions] は押したときの動作を持つので、`equals` が構造比較にならない**（ラムダは
 * 参照で比べる）。[title] と [count] は素直に比べられるので、**テストは項目ごとに見ること**。
 * 同じ理由で、**`remember` や `LaunchedEffect` のキーにこの型を使わない**——毎回発火する。
 */
data class ReBuyAppBarState(
    /** 戻る。**「1 つ戻る」とは限らず、何をするかは画面が決める**（§6）。null なら ← を出さない。 */
    val onBack: (() -> Unit)? = null,
    val title: String,
    val count: Count? = null,
    val actions: List<Action> = emptyList(),
) {
    /**
     * バーに添える数（01 の「全 n 件」・04 の進捗「x / n」）。
     *
     * [testTag] はテスト用の印。**値は Kotlin が決め、付け方は描く側が知る**（Android は `testTag`、
     * iOS は `accessibilityIdentifier`）。**[Action] と違って省略できる**のは 01 が印を持たないため——
     * 誰も assert しないタグを増やさないほうを採った。
     */
    data class Count(
        val text: String,
        val testTag: String? = null,
    )

    /** バーの押せるもの（01 の ＋ と ⚙）。**戻るは [onBack] が持つ**のでここには入らない。 */
    data class Action(
        val icon: ReBuyAppBarIcon,
        val testTag: String,
        val onClick: () -> Unit,
    )
}

/**
 * バーのアイコンの**種類**。
 *
 * **実体は渡さず、描く側が対応表を持つ**（`docs/仕様/13_画面定義書.md` §6）——Android は Material の
 * `ImageVector`、iOS は SF Symbols で、共通の型が無い。**戻るの ← は入らない**（[ReBuyAppBarState.onBack]
 * の有無で決まり、画面ごとに変わらない）。
 */
enum class ReBuyAppBarIcon {
    /** 品目を足す（01）。 */
    ADD,

    /** 設定へ（01）。 */
    SETTINGS,
}
