# iOS 端スワイプの上流報告

- 作成日: 2026-09-07
- 更新日: 2026-09-07
- ステータス: 検討中（**報告はまだ出していない**。最小再現プロジェクトが未作成）
- 位置づけ: [T-62](../案件/23_技術改善バックログ.md#t-62) を上流へ報告するための作業場。**出し終えたらこの文書は削除する**（残す価値のある知見は先に台帳・`log_23` へ移す）
- 発端: [T-44](../案件/23_技術改善バックログ.md#t-44)（③ 段 4）の着手前スパイクで、iOS の端スワイプが戻りにならないことに気づいた
- 判断の軸: **アプリ側では直せない**（OS の版に紐づく）。報告して待つ以外の手が無い

## 1. 報告する内容

**症状**: Compose Multiplatform の iOS で、画面の端から右へスワイプしても戻りにならない。`NavigationBackHandler` が発火しない。

**同一ビルドを 3 つのシミュレータで比べた結果**（CMP 1.12.0・素の構成・iPhone 17 Pro / iPhone 16 Pro）。

| OS | ビルド | 端スワイプ |
|---|---|---|
| iOS 18.2 | 22C150 | 効く |
| iOS 26.0 | 23A5326a（beta） | 効く |
| iOS 26.5 | 23F77 | **効かない** |

**環境**: Compose Multiplatform 1.12.0 ／ Kotlin 2.4.10 ／ Xcode 26.6（17F113）／ macOS 26（Darwin 25.6.0）

**潰した容疑**（すべて実測）:

| # | 容疑 | 結果 |
|---|---|---|
| 1 | 操作方法（マウスで端を当て損ねた） | 純正の設定アプリは同じ操作で戻れる。18.2 と 26.0 ではアプリ自身も戻れる |
| 2 | `NavDisplay` 側で戻りハンドラが有効になっていない | `ComposeUIViewController` の直下に `NavigationBackHandler`（`isBackEnabled = true`・戻り先あり）を置いても発火しない。非推奨の `BackHandler` でも同じ |
| 3 | SwiftUI のホスティングがジェスチャを奪っている | `UIHostingController` を外し `ComposeUIViewController` を `UIWindow.rootViewController` に直置きしても効かない |
| 4 | `androidx.navigationevent` の版 | 1.1.1 / 1.1.2 / 1.2.0-alpha01 のいずれでも効かない |
| 5 | klib の宣言重複（JetBrains フォークと Google 版） | jetbrains 版の klib は `linkdata` にパッケージを 1 つも持たない空の中継。重複していない |
| 6 | CMP 1.12.0 での回帰 | 1.11.1 へ落としても 26.5 では効かない |

**ソースを読んだ結果**: 配線に条件は無い。`ComposeContainer.ios.kt` が `navigationEventDispatcher.addInput(...)` と `onDidMoveToWindow(...)` を無条件で呼び、`UIKitNavigationEventInput` は `onHasEnabledHandlersChanged` で `UIScreenEdgePanGestureRecognizer` の `enabled` を切り替える。有効化の設定項目は無い。

## 2. 出し先

**YouTrack の CMP プロジェクト**（`https://youtrack.jetbrains.com/newIssue?project=CMP`）。GitHub Issues は YouTrack へ移行済みで、[CONTRIBUTING.md](https://github.com/JetBrains/compose-multiplatform/blob/master/CONTRIBUTING.md) も「まず YouTrack に issue を作れ」と書いている。**JetBrains アカウントが要る**（無料）。

**投稿はオーナーが行う**——外向きの行為で、アカウントもオーナーのもの。

## 3. 既存の課題との関係

事前に調べた範囲では**同一症状の公開報告は無い**（2026-09-07 時点）。重複起票にならないよう、出す前にもう一度検索すること。

| 課題 | 関係 |
|---|---|
| [CMP-8791](https://youtrack.jetbrains.com/issue/CMP-8791) | 1.8.0 で「左端スワイプで戻らない」。原因未特定のまま Incomplete でクローズ。**いちばん近いが版も構成も違う** |
| [CMP-9869](https://youtrack.jetbrains.com/issue/CMP-9869) | iOS 26 が UINavigationController の戻りジェスチャを全幅に広げたことへの対応。1.12.0 で修正済み。**今回は UINavigationController を使わない構成でも起きる**ので別件 |
| [CMP-10696](https://youtrack.jetbrains.com/issue/CMP-10696) | 同じ `UIKitNavigationEventInput` が **CMP 1.10.1〜1.11.1 の iOS 26.4.1 実機で動いている**前提の不具合報告。今回の症状と噛み合わない |

## 4. 残っている作業

| | 内容 | 状態 |
|---|---|---|
| a | **最小再現プロジェクトを作る**。CMP のテンプレートに `NavigationBackHandler` を 1 つ置くだけ。**Re-Buy 本体は出さない**（報告に使うには大きすぎる） | 未 |
| b | 報告文を英語で書く（§1 の内容） | 未 |
| c | 出す前に既存課題を再検索する | 未 |
| d | 出したら課題番号を [T-62](../案件/23_技術改善バックログ.md#t-62) の関連へ書き、この文書を削除する | 未 |

## 5. 詰めるかどうかを決める点

- **26.1〜26.4 で効くかは測っていない。** 手元に無く、1 版あたり約 7.3 GB のダウンロードが要る。**起票してから聞かれたら測る**方針にしている（[T-62](../案件/23_技術改善バックログ.md#t-62)）
- **手元の 26.0 は beta ビルド**（23A5326a）。正式版の 26.0 では未確認
- **実機では未確認。** 無料の Apple Developer アカウントの制限で実機ビルドが通らず、Developer Program の加入判断は ⑤（[11](../仕様/11_要求定義書.md) §12）
