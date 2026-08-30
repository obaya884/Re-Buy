import SwiftUI
import ReBuyUi

@main
struct iOSApp: App {
    init() {
        // 最初の画面が描かれる前に呼ぶ必要がある
        ReBuyViewControllerKt.setupKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
