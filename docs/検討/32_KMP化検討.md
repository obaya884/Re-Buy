# KMP 化検討（ロードマップ ③）

- 作成日: 2026-08-29
- 更新日: 2026-08-30
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
| 段取り | 5 段に切る（§10） |

## 3. モジュール構成（A）

```
:shared:data     Room（Item / Category / DAO / Converter / AppDatabase）とドライバの expect/actual
:shared:domain   ItemRepository / CategoryRepository
:shared:ui       Compose 画面・ViewModel・Navigation・theme・画面文言・AboutLibraries 生成
:androidApp      MainActivity / Application / マニフェスト / ランチャーアイコン
iosApp/          Xcode プロジェクト（SwiftUI の App と Compose ホスト。Gradle モジュールではない）
```

- 依存の向きは `:androidApp`・`iosApp` → `:shared:ui` → `:shared:domain` → `:shared:data` の一方向。Gradle が機械的に強制する
- **`:androidApp` は KMP モジュールにしない**。AGP 9 は KMP モジュールへの Android application プラグイン適用を打ち切っており、分離は選択ではなく前提（[JetBrains の新デフォルト構成](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/)と同じ形）
- `Item` / `Category` は `:shared:data` に置いたまま `:shared:domain` と `:shared:ui` が参照する。Room の `@Entity` を UI まで流している現構造をそのまま移す。domain モデルを別に立ててマッピングを挟むのは ③ の範囲外（④ 以降）
- **`applicationId` は `io.github.obaya884.rebuy` のまま変えない。`namespace` はモジュールごとに分ける**

| モジュール | `namespace` | Kotlin package |
|---|---|---|
| `:androidApp` | `io.github.obaya884.rebuy` | `io.github.obaya884.rebuy` |
| `:shared:data` | `io.github.obaya884.rebuy.data` | `io.github.obaya884.rebuy.data` |
| `:shared:domain` | `io.github.obaya884.rebuy.domain` | `io.github.obaya884.rebuy.domain` |
| `:shared:ui` | `io.github.obaya884.rebuy.ui` | `io.github.obaya884.rebuy.ui` |

  **`namespace` は Kotlin package と揃え、`:androidApp` は `applicationId` とも揃える**（公式が「同じにしておけば心配ない」と書く形）。`namespace` はモジュール間で一意である必要があるが（AGP 9 は重複をビルドエラーにする）、`shared` を入れなくても一意になるので入れない。

  **`shared` は Kotlin package に入れない。** Gradle 上の入れ物を示す語で、モジュールの性格を説明していないため。Ivy Wallet（`:shared:data:model` → `com.ivy.data.model`）・Tivi・DroidKaigi・JetBrains の KMP テンプレートのいずれも入れていない。

  `R` と `BuildConfig` の FQN は `namespace` で決まるので、**リソースを使う側は所有モジュールの `R` を import する**（`io.github.obaya884.rebuy.ui.R`）。Now in Android も同じ形で、feature モジュールが別モジュールの `R` を import している。`applicationId` は `io.github.obaya884.rebuy` のまま変えないので、端末上のパッケージ名・`context.packageName`・DB のファイルパスは不変
- **AboutLibraries プラグインは `:shared:ui` に適用する**。プラグインは生成した `res/raw/aboutlibraries.json` を適用先モジュール自身の res ソースとして登録するため、`LicenseScreen` と同じモジュールに置く必要がある。ただし収集範囲は適用先モジュールの依存グラフなので、`:androidApp` にしか宣言していない依存はライセンス一覧から落ちる——移設の前後で `aboutlibraries.json` を diff して確かめる
- **convention plugin（`build-logic` の included build）は段 3 の最初に導入する**（T-28）。段 2 までの重複は `repositories` と SDK レベルと `compileOptions` の 4 行程度で、しかも書き忘れると必ずビルドが落ちる自己修正する重複なので割に合わない。段 3 で各 `:shared:*` に KMP のターゲット定義と `commonMain` / `androidMain` / `iosMain` の source set が入ると、**書き忘れが静かに効く**（ターゲットが 1 つ足りなくてもビルドは通り、iOS で動かないことに後で気づく）ため、そこが導入点になる。Android 専用の convention を先に書くと段 3 で書き直しになる

## 4. データ層（B）

- `AppDatabase` ・エンティティ・DAO・Converter を `commonMain` へ移す
- `@ConstructedBy(AppDatabaseConstructor::class)` と `expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>` を置く
- ドライバは `BundledSQLiteDriver`。両 OS に同じ SQLite 実装が載るので挙動差が出ない
- ビルダーには `setDriver(BundledSQLiteDriver())` と **`setQueryCoroutineContext(Dispatchers.IO)`** を必ず対で指定する
- DB ファイルのパス解決だけ `expect/actual`（Android は `Context` 経由、iOS は `NSDocumentDirectory`）
- `androidApp/schemas/` の JSON は `shared/data/schemas/` へ移す。移すだけで中身は変えない
- **`InstantConverter` の `java.time` 依存を外す**。`kotlinx-datetime` または `kotlin.time.Instant` へ置き換える。保存形式はエポックミリ秒の `INTEGER` で、どの実装でも同じ値になるため置き換えで壊れない（T-24 で文字列保存をやめた）
- `ItemStatusConverter` の `value`（0 / 1 / 2）も変えない

## 5. DI（C）

- `:shared:data` / `:shared:domain` / `:shared:ui` がそれぞれ Koin モジュールを公開し、`:androidApp` と `iosApp` が起動時に合成する
- ViewModel は `koinViewModel()` で取得する。`hiltViewModel()` の呼び出し箇所と 1 対 1 で置き換わる
- `koin-compose` と `koin-compose-viewmodel` を `commonMain` に入れる
- Hilt・Hilt 用 KSP・`@HiltAndroidApp` / `@AndroidEntryPoint` はすべて外れる。`di/` の「1 依存 1 ファイル」の粒度は Koin モジュール内の宣言に置き換わる
- コンパイル時の依存グラフ検証は失われる。依存が 5 つで起動時に即壊れること、全画面を開く `NavigationTest` が起動時解決を拾うことから許容する

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
- **ViewModel のテストは移植の前に Android 側で書く**（移植で挙動が変わっていないことを機械で確かめる唯一の手段）。T-26 で 90 件を追加済み
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
| 1 | DI を Hilt から Koin へ差し替える。**単一モジュールのまま** | ViewModel のユニットテストが 1 行も変わらず緑（DI に触れていないため）。全画面を開く instrumented が起動時解決を拾う |
| 2 | モジュールを 4 つに分ける。**Android のまま**で、KMP 化はしない | Android 同一挙動、既存テストが 1 行も変わらず緑、CI 緑。§11 の開発基盤の壊れはここで塞ぐ |
| 3 | KMP/CMP 移植（Room KMP、`java.time` の置き換え、テストを `commonTest` へ）。iOS は Compose 全画面を出す薄い SwiftUI ホスト 1 枚 | Android 同一挙動、iOS シミュレータで同じ画面、`commonTest` 緑、CI が 4 ジョブで緑 |
| 4 | iOS の外枠を SwiftUI の `TabView` ＋ `NavigationStack` に差し替え、共有 backstack と同期する | iOS がネイティブのタブバー・ナビゲーションバーで動く。Android は無変更 |

**1 段で 1 つのことだけを変える**のがこの段取りの要点。Nav3 化・モジュール分割・KMP 移植はどれも挙動が変わりやすく、混ぜると落ちたときに原因を切り分けられない。

DI の差し替えを分割より前に置くのは、**Hilt のまま分割すると段 3 で捨てる配線を書くことになる**ため。各モジュールに `hilt-android` と `ksp(hilt-compiler)` を足し、`@InstallIn` のモジュールが境界をまたいで集約されるように配線する作業が丸ごと無駄になる。Koin なら KSP もアノテーション処理も要らないので、分割時に配線が要る KSP は Room の分（`:shared:data`）だけで済む。

差し替えの網はすでにある。ViewModel のユニットテスト（T-26 の 90 件）は ViewModel を直接生成しており DI に触れていないので、`@HiltViewModel` を外しても無変更で緑のまま。Koin で失われるコンパイル時のグラフ検証（§5）は、全画面を開く `NavigationTest` が起動時解決の形で拾う。**代わりに、モジュール分割の最中はコンパイル時のグラフ検証が無い状態で作業することになる**——依存が 5 つで instrumented が拾うため許容する。

分割を独立させるのは、それが「Gradle の構成変更」だけで完結し、既存のテストがそのまま網になるため。ここで CI とツール（§11）の壊れを先に片づけておけば、移植の段では KMP 化そのものだけを見ればよくなる。

iOS のホストを移植の段に置いて後ろへ送らないのは、§13 の未確認事項（AboutLibraries が CMP で動くか、Navigation 3 の CMP 対応の安定性）が **iOS で 1 回動かすまで潰せない**ため。移植を全部終えてから「CMP が iOS で動かない」と判明する経路を残さない。

## 11. ③ で壊れる開発基盤

② の最終レビューで挙がった箇所。実装計画に組み込む。

**6 点すべて済**（内訳は [段 3 実装計画](../案件/archive_23_kmp段3実装計画.md) の §11 の表）。

- ~~`./gradlew build` が全モジュール対象になり、Linux の CI で iOS ターゲットのタスクが落ちる~~ → **済**（ステップ 16a）。タスクの明示列挙ではなく、macOS ランナーの `ios` ジョブで塞いだ
- ~~Gradle Managed Device のタスク名に `:androidApp:` 修飾が要る~~ → **済**（段 2）
- ~~`androidApp/build/` 決め打ちのレポートパス~~ → **済**（ステップ 16a）
- ~~`test-reviewer` の起動契機が `androidApp/src/test/**` なので `commonTest` に効かない~~ → **済**（ステップ 16a）。`commonTest` / `androidHostTest` / `iosTest` へ
- ~~CLAUDE.md のアーキテクチャ節を `docs/仕様/15_アーキテクチャ定義書.md` へ移すとき、`.claude/agents/` 2 本の参照を同時に直す~~ → **済**（ステップ 16b）
- ~~KMP のタスク名を `.claude/settings.json` の allow に足す~~ → **済**（ステップ 3 と 16a）

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

段 3 の計画（[実装計画](../案件/archive_23_kmp段3実装計画.md)）を書くにあたって調べ、次の 4 点は結論が出た。

- **AboutLibraries は CMP で動く。** 15.2.0 が Compose 1.12.x / AGP 9 / Kotlin 2.4 をサポート版として明記している。`aboutLibraries { export { outputFile = ... composeResources/files/... } }` に変え、`Res.readBytes` で読む
- **Navigation 3 の CMP 対応は入っている。** ただし**多相シリアライズの明示登録が必須**で、`sealed class Screen : NavKey` を `@Serializable sealed interface` にし、`SavedStateConfiguration` に `polymorphic(NavKey::class)` を登録する。登録漏れは **Android では動いたまま iOS だけプロセス復元時に落ちる**
- **`java.time` の置き換えは `kotlin.time.Instant`。** kotlinx-datetime 0.7 で `Instant` / `Clock` は stdlib へ移管され、`kotlinx.datetime.Instant` は typealias になった。選択の問題ではなくなっている。**日付の書式化（ロケール依存）だけ**は stdlib に等価物が無いので `expect/actual` にし、`androidMain` は現在の `java.time` の実装をそのまま使う（Android の表示を変えないため）
- **KMP モジュールでは built-in Kotlin は関与しない。** KGP を明示適用するため。`:androidApp` だけが built-in Kotlin のまま残る

残る未確認事項は着手時に潰す。

- **Gradle 9.7.1 の埋め込み Kotlin 2.2 で、KGP 2.4.10 を使う convention plugin をコンパイルできるか**（T-28a の最初に判定。ダメなら precompiled script plugin をやめて素の `Plugin` クラスへ退避）
- AGP 9.3.2 の KMP ライブラリ DSL のブロック名（`kotlin { android { } }` か `androidLibrary { }` か）と host test の source set 名
- `androidx.room` Gradle プラグインの `schemaDirectory` がバリアント別サブディレクトリを掘らないか（掘られると既存 JSON の場所が変わり、`RoomMigrationTest` の assets 指定と同時に壊れる）
- Compose Multiplatform のどの版が Kotlin 2.4.10 と Compose BOM 2026.08.00 に対応するか

## 14. 完了条件

- Android が移植前と同一挙動（各段の終わりでそれぞれ確認する）
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

[log_32_KMP化検討.md](./log_32_KMP化検討.md) に日付順で積む。
