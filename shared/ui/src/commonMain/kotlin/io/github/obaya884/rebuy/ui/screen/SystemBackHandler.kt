package io.github.obaya884.rebuy.ui.screen

import androidx.compose.runtime.Composable

/**
 * 端末の「戻る」を画面側で受け止める。
 *
 * **プラットフォームごとに別の口を使うので `expect` / `actual` にしている。** Android は
 * `androidx.activity.compose.BackHandler`、iOS は `androidx.navigationevent.compose` の
 * `NavigationBackHandler`。**共通の `androidx.compose.ui.backhandler.BackHandler` は使えない**——
 * Android では `compose.ui` が androidx の実装へ解決され、そこにこのパッケージが無い
 * （compose-ui 1.12.0 で実測）。
 *
 * **iOS の端末の戻りは画面の端から内側へのスワイプ**（画面定義書 §2）。当初は「iOS には端末の
 * 戻るが無い」として何もしない実装にしていたが、**その前提が誤りだった**（FB-18）。
 *
 * @param enabled false のときは受けずに素通しする。シートが開いている間は
 *   シート側が閉じるために使うので、下の画面が横取りしない（画面定義書 §2）
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * シートとダイアログを、iOS の端スワイプで閉じる。**シートやダイアログの呼び出しの直後に置く。**
 *
 * **iOS の `Dialog` と `ModalBottomSheet` は端スワイプで閉じない。** 上流の `OnBackClickEventHandler`
 * は進捗を受けた戻りを消費するだけで `onDismissRequest` を呼ばない（CMP 1.12.1）。配る側は最後に
 * 登録されたハンドラを選ぶので、**直後に置く**とこちらが先に受ける。
 *
 * **Android では何もしない。** `ModalBottomSheet` と `Dialog` が自分で閉じるうえ、ここで受けると
 * シートの予測型の戻りを奪う。
 */
@Composable
expect fun IosEdgeSwipeDismissHandler(onDismiss: () -> Unit)
