import SwiftUI
import UIKit
import ReBuyUi

/// Kotlin から渡ってきたアプリバーの内容を持つ。
///
/// Compose 側は**内容が変わったときだけ**渡してくる（Kotlin の `PublishingAppBarRenderer`）ので、
/// ここへの代入がそのままツールバーの作り直しになる。
@Observable
final class ToolbarModel {
    /// 起動直後の 1 フレームは nil。最初に渡るのは Compose の最初のコンポーズの後。
    var toolbar: ReBuyToolbar?
}

/// Kotlin 側の `ReBuyViewController(onAppBar:)` を SwiftUI に載せる。
/// クラス名の `ReBuyViewControllerKt` は Kotlin のファイル名から決まる。
struct ComposeView: UIViewControllerRepresentable {
    let onAppBar: (ReBuyToolbar) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ReBuyViewControllerKt.ReBuyViewController(onAppBar: onAppBar)
    }

    /// **Kotlin へ渡るのは `make` の時点の [onAppBar] 1 つだけ**——更新の口を開けていないので、
    /// 渡す先は参照型（`ToolbarModel`）でなければならない。値を捕まえると 2 度目以降が届かない。
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var model = ToolbarModel()

    var body: some View {
        NavigationStack {
            ComposeView { model.toolbar = $0 }
                .ignoresSafeArea()
                .navigationTitle(model.toolbar?.title ?? "")
                // 見出しは inline に固定する（`docs/仕様/13_画面定義書.md` §6）
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    if let toolbar = model.toolbar {
                        toolbarContent(for: toolbar)
                    }
                }
        }
        .tint(model.toolbar.map { Color(argb: $0.accentArgb) })
    }

    @ToolbarContentBuilder
    private func toolbarContent(for toolbar: ReBuyToolbar) -> some ToolbarContent {
        // ← は「1 つ戻る」ではなく画面が決めた動作を呼ぶ（`docs/仕様/13_画面定義書.md` §6）。
        // `NavigationStack` は 1 段しか積まないので、戻るはシステムではなくここが出す
        if let onBack = toolbar.onBack {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.backward")
                }
            }
        }
        // 数はアクションの左。ガラスの地は付けない（`docs/仕様/13_画面定義書.md` §6）
        if let countText = toolbar.countText {
            ToolbarItem(placement: .topBarTrailing) {
                Text(countText)
                    .accessibilityIdentifier(ifPresent: toolbar.countAccessibilityId)
            }
            .sharedBackgroundVisibility(.hidden)
        }
        // **空の群を作らない。** アクションを持つのは 01 だけで、残りの画面では
        // 中身の無いガラスのカプセルが出かねない
        if !toolbar.actions.isEmpty {
            ToolbarItemGroup(placement: .topBarTrailing) {
                ForEach(toolbar.actions, id: \.accessibilityId) { action in
                    Button(action: action.onClick) {
                        Image(systemName: action.icon.symbolName)
                    }
                    .accessibilityIdentifier(action.accessibilityId)
                }
            }
        }
    }
}

private extension View {
    /// 印の無いものに空文字の識別子を付けない——`""` で拾えてしまう。
    /// Kotlin 側の `MaterialAppBarRenderer` にある `testTagIfPresent` と同じ判断。
    @ViewBuilder
    func accessibilityIdentifier(ifPresent identifier: String?) -> some View {
        if let identifier {
            accessibilityIdentifier(identifier)
        } else {
            self
        }
    }
}

private extension ReBuyAppBarIcon {
    /// SF Symbols の対応表。**描く側が持つ**（`docs/仕様/13_画面定義書.md` §6）。
    /// Kotlin 側は `MaterialAppBarRenderer` が同じ形で Material の表を持つ。
    ///
    /// Kotlin の enum は Objective-C ではクラスとして出るので、`switch` が網羅にならない。
    /// 足し忘れは**デバッグでだけ落として気づけるようにし**、リリースでは ? を出して落とさない。
    var symbolName: String {
        switch self {
        case .add: return "plus"
        case .settings: return "gearshape"
        default:
            assertionFailure("SF Symbols の対応表に無いアイコン: \(name)")
            return "questionmark"
        }
    }
}

private extension Color {
    /// Kotlin から `0xAARRGGBB` で渡ってくる（`docs/仕様/13_画面定義書.md` §6）。
    init(argb: Int64) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}
