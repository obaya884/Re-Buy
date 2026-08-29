package io.github.obaya884.rebuy.ui

import androidx.compose.material3.Text
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS 側の入口。いまは Xcode の配線を確かめるためのスタブで、
 * ステップ 15 で [ReBuyApp] に差し替える。
 */
fun ReBuyViewController(): UIViewController = ComposeUIViewController {
    Text("ReBuy")
}
