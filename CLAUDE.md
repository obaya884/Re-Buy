# CLAUDE.md — Re-Buy 開発ガイド

## プロジェクト概要

Re-Buy（仮称）は「くりかえし使える買い物リスト」。品目をプールに常駐させ、買い物のたびにカゴへ入れ → 店でチェック → 終了でプールに戻す（最終購入日付き）。対象は一人で買い物する人。端末内で完結し、Android と iOS に出す。**判断に迷ったら [要求定義書](./docs/仕様/11_要求定義書.md) の価値 C-1〜C-5 とスコープ外表に照らす。** 現在地は台帳（[要件バックログ](./docs/案件/22_要件バックログ.md)・[技術改善バックログ](./docs/案件/23_技術改善バックログ.md)）の現況節。

アプリ内の表示文言・コードコメント・docs・コミットメッセージは日本語。

## 最重要ルール: 仕様変更は docs/ 反映が先

挙動の契約はドキュメント群にある。実装中に挙動レベルの仕様判断が発生した場合、**コードより先に該当ドキュメントを更新する**。判断はオーナーとの対話でのみ決め、AI が単独で決めない。

- 価値・スコープ・収益・機能要求（F-XXX 一覧）: `docs/仕様/11_要求定義書.md`
- 要件・画面・データモデル: [要件定義書](./docs/仕様/12_要件定義書.md)・[画面定義書](./docs/仕様/13_画面定義書.md)・[データモデル定義書](./docs/仕様/14_データモデル定義書.md)
- 挙動を変えない技術活動（リファクタ・依存追随・CI/ツール整備）: `docs/案件/23_技術改善バックログ.md`（**T-XX**。挙動が絡むなら要求軸、絡まないならこちら）
- 未実装要求の案件管理: `docs/案件/22_要件バックログ.md`
- 実機で使って出た指摘: `docs/案件/21_FB台帳.md`（**FB-XX**）

## docs の構造と書き方の規約

ファイル名は**全体で一意な 2 桁番号**を持つ（1X = 仕様 / 2X = 案件 / 3X = 検討。新規文書は該当ブロックの次番号）。`log_` / `closed_` / `guide_` / `plan_` の付属ファイルは親の番号を引き継ぐ。欠番: 24〜26（ロードマップと実装計画の名残。再利用しない）。

- **番号は「文書」に振り、「案件」には振らない。** 案件は増え続けるので、1 案件 1 番号にすると 2 桁が尽きる。個々の実装計画は台帳の付属文書として `plan_<親番号>_<名前>.md` に置く（挙動を変えない技術活動なら親は 23、要求なら 22）
- **役目を終えた文書は削除する**。検討（3X）は内容が仕様書群へ移った時点で、実装計画（plan_）は完了した時点で消す。**残す価値のある知見は、消す前に CLAUDE.md・仕様書・log_ へ移す**。経緯と全文は git 履歴が持つので、アーカイブは作らない

- **詳細は下位文書に 1 か所だけ書く**。上位文書は 1〜2 行の要約＋参照リンク
- **判断の経緯（「なぜそうしたか」）は本文に書かず、同階層の `log_<文書名>.md` へ日付順に積む**。本文は現在の仕様だけ。決定ログを持たない文書は最初のエントリが出た時点で作る
- **台帳（22・23）の完了エントリは、状態更新と同じコミットで同階層の `closed_<文書名>.md` へ移す**（`sh scripts/ledger-move.sh T-XX`）
- **文書の分離・改番・節の移動をしたら、旧参照を grep で洗い出して追随させてから閉じる**。参照は docs 内だけでなく本書・README・`.claude/` にも散る
- 表のセル内に `|` を書かない（`docs-check` が列を数えられなくなる）
- 機械検査は `sh scripts/docs-check.sh`（表構造・台帳の書式・リンクとアンカーの実在。CI の `docs` ジョブが同じものを回す）

## 技術スタックとビルド

Kotlin + Jetpack Compose、4 モジュール構成 `:androidApp` / `:shared:ui` / `:shared:domain` / `:shared:data`（③ の段 3 で KMP へ）。Room / Koin / Navigation 3 / AboutLibraries。

- `applicationId`: `io.github.obaya884.rebuy`（逆ドメイン部分は ⑤ の公開前に再検討）
- **`namespace` は Kotlin package と揃え、モジュールごとに分ける**（`:androidApp` = `io.github.obaya884.rebuy` ＝ `applicationId` ／ `:shared:data` = `...rebuy.data` ／ `:shared:domain` = `...rebuy.domain` ／ `:shared:ui` = `...rebuy.ui`）。Gradle のパスにある `shared` は入れ物を示す語なので package にも namespace にも入れない
- **画面文言と drawable は Compose Resources に置く**（`shared/ui/src/commonMain/composeResources/`）。引き方と `publicResClass` の理由は [アーキテクチャ定義書](./docs/仕様/15_アーキテクチャ定義書.md) §4.4
- **`:shared:ui` の Kotlin から Android の `R` を引く場所はもう無いが、`androidResources { enable = true }` は外さない**。Compose Resources が道連れになり、ビルドは緑のまま全画面が落ちる（③ の段 3 で実際に踏んだ）
- minSdk 31 / compileSdk 37 / targetSdk 35 / Java・JVM target 17
- AGP 9 / Gradle 9。JDK は 17 以上（Android Studio 同梱の JBR 25 で動作確認済み）
- ビルドスクリプトは Kotlin DSL。依存は必ず `gradle/libs.versions.toml` 経由で追加する

### ビルド構成の注意点

- **Kotlin は AGP の built-in Kotlin でコンパイルされる**（KMP 化していないモジュール）。`org.jetbrains.kotlin.android` プラグインは適用していないので、足さないこと。**KMP 化したモジュール**（`:shared:data`）は `org.jetbrains.kotlin.multiplatform` を明示適用するので built-in Kotlin は関与しない。
- **KMP モジュールには `compileOptions` が無い。** `jvmTarget` を指定しないとバイトコード版がビルド環境の JDK で決まり、手元と CI で別物が出る。`rebuy.android.base` が KMP 用の枝で入れているので通常は意識しなくてよいが、**convention plugin を通さない設定を書くときは `javap -v <class> | grep "major version"` が 61 かで確かめる**。
- そのため Kotlin / KSP のバージョンは、ルート `build.gradle.kts` の `buildscript { dependencies { classpath ... } }` で引き上げている。AGP 同梱の KGP より新しいものを使いたい場合はここを直す（version catalog の `kotlin` / `ksp` が実体）。
- `kotlinOptions {}` は使えない。コンパイラオプションが必要なら `kotlin { compilerOptions {} }` を使う。JVM target は `compileOptions.targetCompatibility` から引き継がれるので通常は指定不要。
- `ksp {}` と `android.sourceSets {}` はトップレベル / `android {}` 直下に置くこと。Groovy DSL 時代は `defaultConfig {}` の中に書いても暗黙の委譲で動いていたが、Kotlin DSL では解決できない。
- リポジトリ・SDK レベル・`compileOptions`（KMP モジュールでは `jvmTarget`）・KMP のホストテストの有効化は `build-logic` の convention plugin `rebuy.android.base` が入れる。**モジュールを増やしたら `plugins {}` に `id("rebuy.android.base")` を足す**と、これらを各モジュールに書かなくてよくなる。ただし `rebuy.android.base` は AGP を適用せず、適用済みかどうかを見て設定するだけなので、`alias(libs.plugins.android.application)` / `alias(libs.plugins.android.library)` は引き続きモジュール側に要る。プラグインの解決は `settings.gradle.kts` の `pluginManagement {}` で宣言している。ルートの `settings.gradle.kts` では `dependencyResolutionManagement {}` を使わない（Gradle 9.7 時点でも `@Incubating` で Kotlin DSL だと警告が出る）。
- **`build-logic` は included build なので、ルートの `buildscript { classpath }` を継承しない。** convention plugin のコンパイルに要る AGP / KGP は `build-logic/build.gradle.kts` で自前に宣言する。**実行時に効くのはルート側の版**なので、`compileOnly` にしたうえで catalog と同じ版に固定する——ずれるとコンパイルは通って実行時に `NoSuchMethodError` になる。同じ理由で、convention plugin の中では `plugins { id(...) }` ではなく `plugins.withId(...)` で書く。
- **`clean` の直後の 1 回目の `build` が `lintAnalyzeDebug` で落ちることがある**（`partialResultsDirectory` の `NoSuchFileException`）。`clean` が消したディレクトリを、それ以前に作られた設定キャッシュのエントリが参照するために起きる。もう一度叩けば通る。
- AGP 10 以降に上げるときは、`gradle.properties` に `android.builtInKotlin` / `android.newDsl` を足して退避する手は使えなくなる（すでに移行済みなので問題ない）。

## アーキテクチャ

**正は [アーキテクチャ定義書](./docs/仕様/15_アーキテクチャ定義書.md)。** UI (Compose) → ViewModel → Repository → DAO → Room の単方向レイヤ構成で、DI は Koin。`:shared:*` は KMP で、**既定の置き場所は `commonMain`**——`androidMain` / `iosMain` に置くのはプラットフォーム API を直に触るものだけ。

テストの段と「何をどこで守るか」は [テスト戦略定義書](./docs/仕様/17_テスト戦略定義書.md)。

## サブエージェント運用

実装サイクルの各段階で以下へ委譲し、メインセッションのコンテキストを実装そのものに集中させる。読み取り中心・出力が長くなる作業を委譲し、**コードの編集はメインセッションが行う**。

| 段階 | 委譲先 | 内容 |
|---|---|---|
| 着手前の調査 | 組み込み `Explore` | 既存コードの把握・影響範囲の特定（結論だけ受け取る） |
| 実装方式の設計 | 組み込み `Plan` | 実装方針の立案・トレードオフ整理 |
| 実装の一区切り | `verifier` | build（lint・unit test）・GMD でのインストルメンテーションテスト・docs 検査 |
| 実装の一区切り | `code-quality-reviewer` | 可読性・重複・命名・簡潔さ（verifier と並列起動可） |
| docs の条項に触れたとき | `spec-reviewer` | docs との整合＋設計原則チェック |
| テストファイルに触れたとき | `test-reviewer` | テスト観点の網羅性・正しさ（spec-reviewer と並列起動可） |

- **挙動レベルの仕様判断は委譲しない**。オーナーとの対話で決め、docs/ を先に更新する
- **実装は原則メインセッションが行う**。仕様が文書で完結している自己完結的な純粋ロジックに限り、条項を指定して `general-purpose` に委譲してよい
- レビュー・検証の 4 本は互いに独立なので**バックグラウンドで並列起動**し、結果を待つ間に次の作業を進めてよい
- 軽微な変更（typo・文言・1 ファイルの小修正）ではエージェントを起動せず、メインセッションで直接 build する
- 指摘への対処（修正）はメインセッションが行い、必要なら同じエージェントを新しく起動して再検証させる

### 起動のコスト

サブエージェントは 1 本ごとに独立したリクエストを持つ。読み取り中心の調査は `Explore` を既定にし、`general-purpose` は編集を伴う自己完結タスクに限る。レビュー系のモデル指定（verifier・spec-reviewer = sonnet ／ code-quality-reviewer・test-reviewer = opus）は変えない——落とすと見逃しが「指摘なし」として返り、正常な合格と区別できない。タスクの切れ目で文脈を切る。

### 起動の契機

レビュー 4 本の起動は「作業の種類」ではなく「**差分が何に触れたか**」で引く（`git diff --name-only` で機械的に決まる）。

- **着手前** — 影響範囲が読み切れないとき: `Explore`
- **実装方式で迷うとき** — トレードオフのある設計判断: `Plan`
- **実装が一区切りしたら（コミット前）** — `verifier` と `code-quality-reviewer` を並列でバックグラウンド起動。軽微な変更では起動しない
- **差分にテストファイル（`shared/*/src/{commonTest,androidHostTest,iosTest}/**`・`androidApp/src/androidTest/**`）が含まれるとき** — `test-reviewer`
- **差分が docs の条項に触れる／条項で定まる挙動を実装したとき** — `spec-reviewer`
- **差分に `build-logic/**`・`.github/workflows/**`・`scripts/**` が含まれるとき** — `code-quality-reviewer` と、検査そのものを足したなら `test-reviewer`。**「本番コードではないから軽微」と自分で判断しない**——T-32 はこの判断でレビューを飛ばした結果、動機とした 2 つの事故のどちらも塞げていない実装がマージされた（log_23 2026-08-30）。**ここには自動テストの網が無い**ので、レビューが唯一の網になる

## 実装完了後のフロー（レビュー → 動作確認 → コミット/push）

実装が一区切りしたら次の順序を必ず踏む。**サブエージェントのレビューとオーナーの動作確認の両方を経るまで「完了」扱いにしない。**

1. **サブエージェントによるレビュー**: `verifier`・`code-quality-reviewer` は常に、`test-reviewer`・`spec-reviewer` は「起動の契機」の条件に当たれば並列起動し、指摘があればメインセッションで対処する
2. **オーナーによる動作確認**: メインセッションが `./gradlew installDebug`（実機）または `android emulator start` → `installDebug`（エミュレータ）で入れ、**確認手順を提示する**。オーナーが実機で挙動を確認する
3. **コミット / push**: 上記 2 つを通してからコミットする。push・マージはオーナーの合図を待つ

軽微な変更は 1・2 を省略してよい。

## git 運用

**正は `docs/仕様/16_git運用定義書.md`。** 常時使う判断は次の 2 つ:

- **リスク別ルーティング**: docs・軽微・ツール整備 → `main` 直コミット／コード・挙動の変更 → ブランチ → PR → CI 緑 → 動作確認 → **合図で squash マージ**。迷ったら PR 側
- **ブランチ命名**: `<id>-<slug>`（例 `t-12-kover`、`f-101-basket-badge`）。**1 ブランチ＝1 PR、マージ後は削除**。push 済みの main から切る

## 開発コマンド

- `./gradlew build` — lint・unit test・debug/release の assemble
- `./gradlew testAndroidHostTest` — 全モジュールのユニットテスト（JVM）。**`testDebugUnitTest` は使わない**——`:shared:*` が KMP になったので一致するモジュールが無くなり、**0 件のまま緑で終わる**。**単一クラスを指定するときはモジュールを修飾する**（`./gradlew :shared:data:testAndroidHostTest --tests "io.github.obaya884.rebuy.data.InstantConverterTest"`）。無修飾で `--tests` を渡すと、一致しない側のモジュールが `No tests found` でビルドを落とす
- `./gradlew :androidApp:pixel6Api35DebugAndroidTest` — インストルメンテーションテスト（Gradle Managed Device。androidTest を持つのは `:androidApp` だけ。エミュレータの手動起動は不要。初回はイメージのダウンロードで数分）
- `./gradlew installDebug` — 端末・エミュレータへインストール
- `./gradlew clean` — KSP（Room。`:shared:data` だけで回る）の生成コードが壊れたとき
- `sh scripts/docs-check.sh` — docs・本書・README・`.claude/` の機械検査
- `sh scripts/check-ios-signing.sh [--staged]` — iOS の署名設定が追跡ファイルに入っていないか（`--staged` は index を見る。pre-commit が使う）
- `sh scripts/ledger-move.sh T-XX [--status '完了 YYYY-MM-DD']` — 台帳 23 のエントリを完了記録へ移す
- `android emulator list` / `android emulator start <name>` — エミュレータ（`android` CLI）

## このリポジトリは public

`obaya884/Re-Buy` は公開リポジトリ。**到達手段・実データ・資格情報につながる記述はコミットしない**。コミットの author（氏名・メールアドレス）だけは例外として受け入れている（要求定義書 §11）。

- 署名鍵（`*.jks` `*.keystore`）・`keystore.properties`・Play Console / App Store Connect の内部 ID・API キーは値も所在も書かない（`.claude/settings.json` の deny で読み取りも塞いである）
- **iOS のビルド設定は `iosApp/Configuration/` の xcconfig に置き、`project.pbxproj` には入れない**。**clone したら 1 回だけ `git config core.hooksPath .githooks` を実行する**——`sh scripts/check-ios-signing.sh` が pre-commit と CI で見る
- オーナーの生活が読み取れる実データ（品目名・カテゴリ名）を docs の例示に使わない
- 一度 push した内容は履歴から消せない。迷ったら書かない
- ソースは閲覧のみ可（README「ライセンス」）。他者の PR は受け付けない

## 規約

- **コードの変更は原則 Edit / Write ツールで行う**。**判断を伴う変更は 1 箇所ずつ Edit する**——どこを直すかを都度考えることに意味があり、まとめて置換すると文脈に応じた直し（周りのコメントの追随など）が抜ける
- **例外は「意味を変えない機械的な一括置換」**（識別子の改名など）。この場合は python / sed 等を使ってよいが、**実行直後に `git diff` を提示する**——部分一致で意図しない箇所まで変わっていないかをオーナーが見られるようにする
- 変異テスト（コードを壊してテストが落ちるか確かめる）はメインセッションが Edit で 1 か所ずつ行い、`verifier` には変異後の実行だけを頼む。git で退避しない
