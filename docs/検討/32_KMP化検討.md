# KMP 化検討（ロードマップ ③）

- 作成日: 2026-08-29
- 更新日: 2026-08-29
- ステータス: 確定（実装計画へ）
- 位置づけ: ロードマップ ③「KMP 化＋マルチモジュール化」の設計。既存の Android アプリ（Kotlin 44 ファイル・約 3,000 行）を Kotlin Multiplatform＋Compose Multiplatform の新構造へ載せ替える。**Android の挙動は変えない**
- 関連: [ロードマップ](../案件/24_ロードマップ.md) ／ [憲章](../仕様/11_憲章.md) ／ [開発基盤検討](./31_開発基盤検討.md) ／ [技術改善バックログ](../案件/23_技術改善バックログ.md)

## 1. 目的

Android 単一モジュールの現構造を、iOS と共有できる構造へ移す。あわせて **KMP を学ぶ**（ロードマップ §2 のとおり、学習それ自体が ③ の目的の一つ）。移植の完了時点で、Android は移植前と同じように動き、iOS シミュレータで同じ画面が動いている。

機能追加・UX の見直しは ④ で行う。③ に混ぜない。

## 2. オーナーが決めたこと

| 決めたこと | 内容 |
|---|---|
| 共有範囲 | Compose Multiplatform で UI まで共有する。iOS の外枠だけネイティブ |
| モジュール | 層別に `:shared:data` / `:shared:domain` / `:shared:ui` ＋ `:androidApp` ＋ `iosApp/` |
| C-5 の解釈 | 揃えるのは機能であって見た目ではない。iOS の作法は iOS に合わせる（[憲章](../仕様/11_憲章.md) §5・§8 を更新済み） |
| DI | Koin |
| Navigation | Navigation 3 |
| 最低 iOS | iOS 26 |
| テスト | 純粋ロジック・Repository・ViewModel を `commonTest` へ |
| CI | macOS ランナーで共有フレームワークのコンパイルと `commonTest` まで。Xcode ビルドは入れない |
| 段取り | 3 段に切る（§10） |

## 3. モジュール構成（A）

```
:shared:data     Room（Item / Category / DAO / Converter / AppDatabase）とドライバの expect/actual
:shared:domain   ItemRepository / CategoryRepository
:shared:ui       Compose 画面・ViewModel・Navigation・theme
:androidApp      MainActivity / Application / マニフェスト / AboutLibraries 生成
iosApp/          Xcode プロジェクト（SwiftUI の App と Compose ホスト。Gradle モジュールではない）
```

- 依存の向きは `:androidApp`・`iosApp` → `:shared:ui` → `:shared:domain` → `:shared:data` の一方向。Gradle が機械的に強制する
- **`:androidApp` は KMP モジュールにしない**。AGP 9 は KMP モジュールへの Android application プラグイン適用を打ち切っており、分離は選択ではなく前提（[JetBrains の新デフォルト構成](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/)と同じ形）
- `Item` / `Category` は `:shared:data` に置いたまま `:shared:domain` と `:shared:ui` が参照する。Room の `@Entity` を UI まで流している現構造をそのまま移す。domain モデルを別に立ててマッピングを挟むのは ③ の範囲外（④ 以降）
- `applicationId` / `namespace` は `io.github.obaya884.rebuy` のまま変えない
- convention plugin（`build-logic`）は作らない。モジュール 4 つで重複が問題になっていないため。必要が出たら T-XX で起票する

## 4. データ層（B）

- `AppDatabase` ・エンティティ・DAO・Converter を `commonMain` へ移す
- `@ConstructedBy(AppDatabaseConstructor::class)` と `expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>` を置く
- ドライバは `BundledSQLiteDriver`。両 OS に同じ SQLite 実装が載るので挙動差が出ない
- ビルダーには `setDriver(BundledSQLiteDriver())` と **`setQueryCoroutineContext(Dispatchers.IO)`** を必ず対で指定する
- DB ファイルのパス解決だけ `expect/actual`（Android は `Context` 経由、iOS は `NSDocumentDirectory`）
- `app/schemas/` の JSON は `shared/data/schemas/` へ移す。移すだけで中身は変えない
- **`InstantEpochMilliConverter` の `java.time` 依存を外す**。`kotlinx-datetime` または `kotlin.time.Instant` へ置き換える。保存形式はエポックミリ秒の `INTEGER` で、どの実装でも同じ値になるため置き換えで壊れない（T-24 で文字列保存をやめた）
- `ItemStatusConverter` の `value`（0 / 1 / 2）も変えない

## 5. DI（C）

- `:shared:data` / `:shared:domain` / `:shared:ui` がそれぞれ Koin モジュールを公開し、`:androidApp` と `iosApp` が起動時に合成する
- ViewModel は `koinViewModel()` で取得する。`hiltViewModel()` の呼び出し箇所と 1 対 1 で置き換わる
- `koin-compose` と `koin-compose-viewmodel` を `commonMain` に入れる
- Hilt・Hilt 用 KSP・`@HiltAndroidApp` / `@AndroidEntryPoint` はすべて外れる。`di/` の「1 依存 1 ファイル」の粒度は Koin モジュール内の宣言に置き換わる
- コンパイル時の依存グラフ検証は失われる。依存が 5 つで起動時に即壊れるため許容する

## 6. UI と Navigation（D）

- ViewModel は `androidx.lifecycle` の ViewModel をそのまま `commonMain` に置く（`viewModelScope` も動く）。現行の「複数の `MutableStateFlow` を `combine` して 1 つの UiState にし `stateIn` で公開する」パターンは変えない
- ルートの `FlowExt.kt` の自作 `combine`（6 引数）も共有側へ移す
- Navigation は **Navigation 3**。`NavHost` から `NavDisplay` ＋ 自前 backstack へ移る
- backstack を共有側が `StateFlow` で持つ。これが iOS のネイティブシェルと同期できる形になる（§7）
- `ReBuyAppScaffold` / `BottomNavigationBar` は `:shared:ui` に置いたままにし、**Android だけが使う**。iOS はこれを呼ばず SwiftUI 側が外枠を持つ

## 7. iOS シェル（E）

- `iosApp/` に Xcode プロジェクトをコミットする
- 構成は SwiftUI の `App` → `TabView` → 各タブが `NavigationStack` → 各画面が `ComposeUIViewController`
- 共有側の backstack（§6）を SwiftUI が購読して `NavigationStack` の path に反映し、戻るスワイプなど iOS 側の操作は共有 backstack へ返す
- タブバー・ナビゲーションバーは OS が描くので、iOS 26 の Liquid Glass がそのまま乗る。Compose 側で近似実装はしない
- 最低 iOS 26 に絞ったため、Liquid Glass 以前の見た目の確認は不要

## 8. テスト（F）

- `commonTest` に置くもの: Converter などの純粋ロジック、Repository、ViewModel
- **ViewModel のテストは現状 0 件**。段 1 に入る前に Android 側で先に書き、移植の網にする（移植で挙動が変わっていないことを機械で確かめる唯一の手段）
- Room のマイグレーション検証は各プラットフォームの実機テストに残す。現行 `RoomMigrationTest` は Android 側で維持する（[技術改善バックログ](../案件/23_技術改善バックログ.md) T-17 と接続）
- 起票済みの T-14〜T-16 は移植の網を厚くする作業でもある。③ の着手時に優先度を見直す

## 9. CI（G）

- 現行の `docs` / `build` / `instrumented` に **`ios` ジョブ**を足す。macOS ランナーで iOS ターゲットのコンパイルと `commonTest` を回す
- public リポジトリなので macOS ランナーは無料。Xcode ビルドとシミュレータの UI テストは入れない（遅く壊れやすい）
- `build` ジョブは Linux のまま。iOS ターゲットのタスクを含めない形に直す必要がある（§11）

## 10. 段取り

| 段 | やること | 検証 |
|---|---|---|
| 0 | Android 単体のまま `NavHost` → Navigation 3 へ書き換える。モジュールは触らない | Android で移植前と同一挙動。落ちたら原因は Nav3 化だけに絞れる |
| 1 | モジュール 4 分割＋KMP/CMP 移植（Hilt → Koin、Room KMP、テストを `commonTest` へ）。iOS は Compose 全画面を出す薄い SwiftUI ホスト 1 枚 | Android 同一挙動、iOS シミュレータで同じ画面、`commonTest` 緑、CI 緑 |
| 2 | iOS の外枠を SwiftUI の `TabView` ＋ `NavigationStack` に差し替え、共有 backstack と同期する | iOS がネイティブのタブバー・ナビゲーションバーで動く。Android は無変更 |

段 0 を先に置くのがこの段取りの要点。Nav3 化は挙動が変わりやすい書き換えで、KMP 移植と同時にやると落ちたときに切り分けられない。

## 11. ③ で壊れる開発基盤

② の最終レビューで挙がった箇所。実装計画に組み込む。

- `./gradlew build` が全モジュール対象になり、Linux の CI で iOS ターゲットのタスクが落ちる
- Gradle Managed Device のタスク名に `:androidApp:` 修飾が要る。`.claude/settings.json` の allow は完全一致なので現行の記述が外れる
- `app/build/` 決め打ちのレポートパス（`ci.yml`・`verifier`・`test-reviewer`）
- `test-reviewer` の起動契機が `app/src/test/**` なので `commonTest` に効かない
- CLAUDE.md のアーキテクチャ節を `docs/仕様/15_アーキテクチャ定義書.md` へ移すとき、`.claude/agents/` 2 本の参照を同時に直す（inline code のパスは `docs-check` の網の外）
- KMP のタスク名を `.claude/settings.json` の allow に足す

## 12. ③ で学ぶこと

学習それ自体が目的なので、何を学ぶかを明示する。

- `commonMain` / `androidMain` / `iosMain` の分け方と `expect`/`actual`
- Gradle の KMP DSL とマルチモジュールでの依存の向きの強制
- Compose Multiplatform を iOS で動かす（`ComposeUIViewController`、SwiftUI との相互運用）
- コンパイル時 DI（Hilt）と実行時 DI（Koin）の違いを、同じアプリを両方で書いて理解する
- Room KMP と SQLite ドライバ
- Navigation 3 の「backstack を自分で持つ」モデル
- Xcode プロジェクトとシミュレータの扱い（署名・配信は ⑤）

## 13. 未確認事項

着手時に潰す。

- **AboutLibraries が CMP で動くか**。`LicenseScreen` が依存しており、動かなければ iOS 側のライセンス表示を別手段にする
- Navigation 3 の CMP 対応は 1.10 で入ったばかり。iOS 実機・シミュレータでの安定性
- `kotlinx-datetime` と `kotlin.time.Instant` のどちらで `java.time` を置き換えるか
- Room KMP と AGP 9 の built-in Kotlin の組み合わせ（このリポジトリは `org.jetbrains.kotlin.android` を適用していない。CLAUDE.md「ビルド構成の注意点」）

## 14. 完了条件

- Android が移植前と同一挙動（段 0・段 1 でそれぞれ確認する）
- iOS シミュレータで全画面が動き、外枠が SwiftUI のタブバー・ナビゲーションバーになっている
- `commonTest` に純粋ロジック・Repository・ViewModel のテストがあり、両ターゲットで緑
- CI が `docs` / `build` / `instrumented` / `ios` の 4 ジョブで緑
- `docs/仕様/15_アーキテクチャ定義書.md` と `docs/仕様/17_テスト戦略定義書.md` を書き、CLAUDE.md のアーキテクチャ節を 15 へ移している
- §11 の 6 点が塞がっている

## 15. ③ で決めないこと

- 機能追加・UX の見直し・MVP の範囲（④）
- iOS の見た目の作り込み（Liquid Glass 前提の画面設計は ④）
- domain モデルと Room エンティティの分離（④ 以降）
- 署名・配信・ストア（⑤）
- desktop / web ターゲット（当面やらない）

## 16. 決定ログ

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
| 2026-08-29 | ViewModel のテストを段 1 の前に Android 側で書く | 移植で挙動が変わっていないことを機械で確かめる唯一の手段。現状 0 件のまま移植すると網が無い |
