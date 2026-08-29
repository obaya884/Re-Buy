# KMP 化検討 決定ログ

- [KMP 化検討](./32_KMP化検討.md) の「なぜそうしたか」を日付順に積む。本文は現在の仕様だけを持つ

| 日付 | 決定 | 理由 |
|---|---|---|
| 2026-08-29 | UI まで Compose Multiplatform で共有する | 既存の Compose コード（UI 27 ファイル）を書き直さずに持っていける。③ の完了条件「iOS シミュレータで同じ画面が動く」に直結する |
| 2026-08-29 | iOS の外枠は SwiftUI のネイティブ view で持つ | オーナー判断。Liquid Glass はシステムが SwiftUI の TabView / NavigationStack に描くもので、Compose の自前描画には乗らない。C-5 は機能の対等性を求めるもので、各 OS で最適な体験を出すことと矛盾しない |
| 2026-08-29 | モジュールを層別 3 つ＋アプリに分ける | 3,000 行に機能別分割は過剰。層別なら依存の向きを Gradle が強制でき、マルチモジュールの学習にもなる |
| 2026-08-29 | モジュール名を `:shared:*` に寄せる | JetBrains の新デフォルト構成（sharedLogic / sharedUI）と語彙を揃えつつ、data と domain の境界を Gradle で強制する |
| 2026-08-29 | 日時をエポックミリ秒で保存する（T-24） | ③ の移植で `java.time` を捨てるとき、文字列形式を 1 バイトも変えないという縛りが移植の自由度を奪う。未公開アプリなので既存 DB との互換より形式の単純さを採る。エポックミリ秒なら実装を替えても値が変わらない |
| 2026-08-29 | DI を Koin にする | KMP で最も普及しており CMP の ViewModel 連携も整っている。③ の主戦場は KMP 移植であり、DI で別の学習正面を作らない |
| 2026-08-29 | Navigation 3 を最初から採る | オーナー判断。どうせ書き換えるなら一回で済ませる。backstack を自分で持つモデルは iOS のネイティブシェルとの同期にも都合が良い |
| 2026-08-29 | Nav3 化を段 0 として Android 単体で先に済ませる | Nav3 化は挙動が変わりやすい書き換えで、KMP 移植と混ぜると落ちたときに切り分けられない |
| 2026-08-29 | 最低 iOS を 26 にする | オーナー判断。Liquid Glass 前提で設計でき、古い見た目の確認が要らない。到達範囲は狭まるが ⑤ までに時間がある |
| 2026-08-29 | CI の iOS ジョブは共有フレームワークのコンパイルと `commonTest` まで | Xcode ビルドとシミュレータ UI テストは遅く壊れやすい。「Android だけ緑で iOS が壊れている」を防ぐ目的には足りる |
| 2026-08-29 | ViewModel のテストを KMP 移植の前に Android 側で書く | 移植で挙動が変わっていないことを機械で確かめる唯一の手段。現状 0 件のまま移植すると網が無い |
| 2026-08-29 | モジュール分割を独立した段にし、以降の段を繰り下げる | オーナー判断。分割と KMP 移植を同じ段でやると、CI とツールの壊れ（§11）と移植の失敗が同時に出て切り分けられない。分割だけなら既存テストがそのまま網になる |
| 2026-08-29 | DI の差し替え（Hilt → Koin）を分割より前の段にする | オーナー判断。Hilt のまま分割すると、各モジュールへの KSP 配線という段 3 で捨てる作業を書くことになる。ViewModel テストは DI に触れていないので、差し替えの網はすでにある |
| 2026-08-29 | `namespace` をモジュールごとに分け、`:shared:ui` が `io.github.obaya884.rebuy` を引き継ぐ | 調査で `android.nonTransitiveRClass=true` によりアプリの `R` にライブラリのリソースが入らないことが分かった。段 2 の合否判定は「既存テストが 1 行も変わらず緑」で、instrumented 2 ファイルが `io.github.obaya884.rebuy.R` を参照している。`applicationId` は不変なので実害は無い |
| 2026-08-29 | AboutLibraries プラグインを `:shared:ui` へ移す | 生成物は適用先モジュール自身の res になるため、`LicenseScreen` と同じモジュールでないと `R.raw.aboutlibraries` が引けない |
| 2026-08-29 | convention plugin の導入を段 3 の最初に置く | オーナー判断。段 2 までの重複は自己修正する（書き忘れるとビルドが落ちる）ので割に合わない。段 3 の KMP ターゲット定義は書き忘れが静かに効くうえ、いま書くと Android 専用の convention を段 3 で書き直すことになる |
| 2026-08-29 | `namespace` を Kotlin package と揃え、`:androidApp` をルートに戻す | 段 2 では既存の `R` の import を守るために `:shared:ui` にルート namespace を渡したが、調査の結果このやり方は Now in Android・architecture-samples・Tivi・DroidKaigi・DuckDuckGo のいずれにも実例が無かった。実害もあり、`:shared:ui` に androidTest を足すとテスト APK の applicationId が `:androidApp` と衝突する。Google の想定する移行手段も「import を書き換える」（Android Studio の Migrate to Non-Transitive R Classes）である |
| 2026-08-29 | Kotlin package に `shared` を入れない | 調査した 5 プロジェクトで `shared` が package に現れた例は 0 件。Gradle 上の入れ物を示す語でモジュールの性格を説明しないため。JetBrains 自身が「namespace にはモジュール名を入れ、Kotlin package には入れない」使い分けをしている |
| 2026-08-29 | 段 3 で iOS を 2 回出す（スタブと全画面） | 最後に 1 回だけ出すと「Xcode の配線が悪いのか CMP が悪いのか」が同時に判明する。`Text` 1 個のスタブを先に接地させれば、そこで潰れるのはアプリと無関係な層（Xcode プロジェクト・framework 埋め込み・Gradle 連携）に限られる。追加コストは commit 1 本 |
| 2026-08-29 | 日付の書式化を `expect/actual` にし、`androidMain` は `java.time` のまま残す | `DateTimeFormatter.ofLocalizedDate(SHORT)` はロケール依存で stdlib に等価物が無い。common で固定書式にすると Android の表示が変わり、段 3 の前提「Android 同一挙動」を崩す |
