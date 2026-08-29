import SwiftUI
import UIKit
import ReBuyUi

/// Kotlin 側の `ReBuyViewController()` を SwiftUI に載せる。
/// クラス名の `ReBuyViewControllerKt` は Kotlin のファイル名から決まる。
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ReBuyViewControllerKt.ReBuyViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
