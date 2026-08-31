# 技術改善バックログ完了記録

- 作成日: 2026-08-29
- 目的: 完了した技術活動（T-XX）を原文のまま保管する。ライブ台帳は[技術改善バックログ](./23_技術改善バックログ.md)（未着手・進行中・保留のみを持つ）
- エントリは完了への状態更新と同じコミットで本書へ移す。**並びは完了日の古い順**（[一覧](#一覧)・[詳細](#詳細)とも末尾に積む）——移送は `sh scripts/ledger-move.sh` が担い、追記先が末尾で固定されているため。**手で並べ替えない**

## 一覧

| ID | タイトル | 種別 | 優先度 | 状態 | 詳細 |
|---|---|---|---|---|---|
| T-01 | GitHub リポジトリの作成と初回 push | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-01) |
| T-02 | docs の番号付き構造と台帳の新設 | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-02) |
| T-03 | docs-check.sh の移植 | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-03) |
| T-04 | ledger-move.sh の移植 | ツール整備 | 中 | 完了 2026-08-29 | [詳細](#t-04) |
| T-05 | Gradle Managed Device の導入 | テスト | 高 | 完了 2026-08-29 | [詳細](#t-05) |
| T-06 | CI と Dependabot | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-06) |
| T-07 | サブエージェント 4 本 | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-07) |
| T-08 | .claude/settings.json | ツール整備 | 中 | 完了 2026-08-29 | [詳細](#t-08) |
| T-09 | CLAUDE.md の全面改訂と README | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-09) |
| T-10 | リポジトリの public 化 | ツール整備 | 高 | 完了 2026-08-29 | [詳細](#t-10) |
| T-13 | assertEquals の引数順（期待値, 実測値） | テスト | 低 | 完了 2026-08-29 | [詳細](#t-13) |
| T-18 | ③ 段 0 Navigation 3 化 | 内部設計 | 高 | 完了 2026-08-29 | [詳細](#t-18) |
| T-14 | toInstant の異常系と UTC 保存条項のテスト | テスト | 中 | 完了 2026-08-29 | [詳細](#t-14) |
| T-16 | InstantDateFormatStringConverterTest の構造 | テスト | 低 | 完了 2026-08-29 | [詳細](#t-16) |
| T-15 | ItemStatusConverter のテスト新設 | テスト | 中 | 完了 2026-08-29 | [詳細](#t-15) |
| T-23 | 日時の読み出しを ResolverStyle.STRICT にする | 内部設計 | 中 | 完了 2026-08-29 | [詳細](#t-23) |
| T-24 | 日時をエポックミリ秒で保存する | 内部設計 | 中 | 完了 2026-08-29 | [詳細](#t-24) |
| T-22 | ViewModel が entry ごとにスコープされることのテスト | テスト | 中 | 完了 2026-08-29 | [詳細](#t-22) |
| T-26 | ViewModel のユニットテストを書く | テスト | 高 | 完了 2026-08-29 | [詳細](#t-26) |
| T-19 | ③ 段 1 DI を Koin にする | 内部設計 | 高 | 完了 2026-08-29 | [詳細](#t-19) |
| T-20 | ③ 段 2 モジュール 4 分割 | 内部設計 | 高 | 完了 2026-08-29 | [詳細](#t-20) |
| T-29 | パッケージをモジュール境界に揃える | 内部設計 | 中 | 完了 2026-08-29 | [詳細](#t-29) |
| T-28 | Composite Build と convention plugin の導入 | ツール整備 | 中 | 完了 2026-08-30 | [詳細](#t-28) |
| T-32 | テストが 0 件で緑になるのを機械で止める | ツール整備 | 中 | 完了 2026-08-30 | [詳細](#t-32) |
| T-31 | ③ 段 3 KMP/CMP 移植 | 内部設計 | 高 | 完了 2026-08-30 | [詳細](#t-31) |
| T-42 | iOS の画面を機械で操作・検証できるようにする | テスト | 高 | 完了 2026-08-31 | [詳細](#t-42) |
| T-48 | iOS のテストで DB を差し替えられるようにする | テスト | 中 | 完了 2026-08-31 | [詳細](#t-48) |
| T-35 | iOS の DB と DI の経路にスモークテストを置く | テスト | 中 | 完了 2026-08-31 | [詳細](#t-35) |

## 詳細

### T-01

- 背景: リモートが無く、CI もバックアップも無い
- 対応方針: `obaya884/Re-Buy` を private で作成して push。squash のみ・ブランチ自動削除を設定
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §3

### T-02

- 背景: docs が `docs/憲章.md` `docs/ロードマップ.md` の平置きで、台帳が無い
- 対応方針: hitosuji の番号体系（1X 仕様 / 2X 案件 / 3X 検討）へ再編し、16 git 運用・22 要件バックログ・23 本書とそれぞれの `closed_` を新設
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §4

### T-03

- 背景: docs の表構造・リンク切れ・台帳の書式を機械で守る手段が無い
- 対応方針: hitosuji の `scripts/docs-check.sh` を移植し、台帳定義を 22・23 に絞る
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §7

### T-04

- 背景: 台帳の完了エントリを一覧と詳細の 2 か所から `closed_` へ移す作業を手でやると片方を置き忘れる
- 対応方針: hitosuji の `scripts/ledger-move.sh` を移植し、対象を T-XX（23）のみにする
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §7

### T-05

- 背景: インストルメンテーションテスト（`RoomMigrationTest`）を回すのに手動でエミュレータを起動する必要があり、CI で回せない
- 対応方針: `app/build.gradle.kts` に Gradle Managed Device `pixel6Api35`（aosp-atd）を定義し、`./gradlew pixel6Api35DebugAndroidTest` の 1 コマンドにする
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §8

### T-06

- 背景: CI が無い
- 対応方針: GitHub Actions に `docs`（docs-check）と `verify`（build＋GMD）の 2 ジョブ。Dependabot は gradle と github-actions を weekly。**この作業だけは PR を通して CI の動作を検証する**
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §8

### T-07

- 背景: 検証・レビューをメインセッションが全部抱えるとコンテキストが実装に使えない
- 対応方針: `.claude/agents/` に verifier / code-quality-reviewer / spec-reviewer / test-reviewer を Android 向けに書く
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §6

### T-08

- 背景: 権限の allow / ask / deny が無く、毎回確認が出るか、逆に危険な操作が素通りする
- 対応方針: `./gradlew` `scripts` `gh pr` を allow、マージ・公開設定を ask、署名鍵・証明書を deny
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §9

### T-09

- 背景: 今の CLAUDE.md は技術メモで、運用（docs 先行・agent・git ルーティング・public 配慮）が書かれていない。README が無い
- 対応方針: hitosuji の構成で全面改訂。README を新規作成しライセンス方針を明記
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §3・§5

### T-10

- 背景: ② の完了条件
- 対応方針: 履歴に資格情報・到達手段が無いことを確認して public に切り替え、ロードマップ ② を完了にする
- 着手条件: T-01〜T-09 が完了していること
- 関連: [開発基盤検討](../検討/31_開発基盤検討.md) §3・§12

### T-13

- 背景: `InstantDateFormatStringConverterTest` の `assertEquals` 4 か所が `(実測値, 期待値)` の順で書かれている。JUnit4 の第 1 引数は期待値なので、失敗時のメッセージが「expected: 実測値 but was: 期待値」と逆に出る。合否は変わらないが、落ちたときに読み違える
- 対応方針: 4 か所の引数を入れ替える
- 関連: ③ の冒頭でレビュアー 4 本を実戦で回す題材（[開発基盤検討](../検討/31_開発基盤検討.md) §12）

### T-18

- 背景: ③ で iOS の外枠を SwiftUI が持つため、共有側は backstack を自分で持つ形である必要がある。現行の `NavHost` は backstack を内部に隠しており、SwiftUI の `NavigationStack` と二重管理になる
- 対応方針: Android 単体のまま Navigation 3 へ移行する。KMP 移植と同時にやると落ちたときに切り分けられないので段を分ける
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §10 ／ [実装計画](./archive_23_kmp段0実装計画.md)

### T-14

- 背景: `InstantDateFormatStringConverterTest` は書き出し方向（`toString`）だけを正常 2 件・例外 2 件で押さえており、読み出し方向（`toInstant`）は正常 2 件のみで異常系が 0 件。Room が DB から読むのはこちらの向きで、非準拠文字列が通る経路が未検証。さらにテスト入力の `OffsetDateTime` が全件 UTC 由来のため、実装の `withZone(ZoneOffset.UTC)` を `ZoneId.systemDefault()` に変えても TZ=UTC の CI では緑のまま通り、「UTC 文字列として保存する」条項がどのテストにも固定されていない
- 対応方針: `toInstant` に空文字・時刻部欠落・末尾余剰・ゼロ埋めなし・月が範囲外・4 桁年の範囲外を足す。非 UTC オフセット由来の `Instant` を 1 件入れて UTC 変換を固定する。往復（`toInstant(toString(x)) == x`）も 1 件
- 優先度の根拠: 条項が守られていない箇所があり、実装を壊してもテストが気づかない
- 関連: T-13 のレビューで test-reviewer が指摘。CLAUDE.md「アーキテクチャ / データ層」

### T-16

- 背景: `InstantDateFormatStringConverterTest` は `.let {}.also {}` のチェーンで書かれており、実測値が `it` に隠れて期待値との区別が目で付かない（T-13 の引数順ミスが生まれた素地）。加えて 1 つの `@Test` に 4 ケースを直列に詰めているため最初の失敗で残りが走らず、例外系の `assertTrue(... is DateTimeException)` は失敗時に真偽しか出ない。テストメソッド名と KDoc の有無も 2 つの `@Test` で揃っていない
- 対応方針: 実測値に名前を付ける素直な形へ直し、ケースごとに `@Test` を分け、例外系は `assertThrows` にする
- 関連: T-13 のレビューで code-quality-reviewer と test-reviewer が指摘

### T-15

- 背景: `ItemStatusConverter` にテストが 1 件も無い。CLAUDE.md は「enum の `value` は既存 DB と互換を壊さない限り変更しない」と定めているが、この条項を守るテストが存在しない
- 対応方針: `NO_DEAL→0` / `IN_SHOPPING_LIST→1` / `CHECKED_IN_SHOPPING_LIST→2` の対応と、未知の値を渡したときの挙動を固定する
- 優先度の根拠: 値がずれると既存 DB のデータが別の状態として読まれる。壊れ方が静か
- 関連: T-13 のレビューで test-reviewer が指摘

### T-23

- 背景: `InstantDateFormatStringConverter` は `ResolverStyle` を指定しておらず、既定の SMART で解決される。そのため `2024-02-30` が例外にならず `2024-02-29` として、`24:00:00` が翌日として黙って読まれる。「非準拠の文字列を黙って別の日時として読まない」という前提が実際には成り立っていなかった
- 対応方針: `withResolverStyle(ResolverStyle.STRICT)` を足し、存在しない日付と 24 時を例外にする。書き込むのはこの converter だけで常に準拠した文字列を書くため、既存データが読めなくなる経路は無い
- 優先度の根拠: 壊れた値を近い日付として読むと、間違いに気づけないまま最終購入日がずれる。オーナー判断で「落ちて気づけるほう」を採った（CLAUDE.md のデータ層の条項を先に更新済み）
- 関連: T-14 のレビューで test-reviewer が発見

### T-24

- 背景: 日時を `YYYY-MM-DD HH:MM:SS` の UTC 文字列で保存しているが、テキストは保存形式としてタイムゾーン・ゼロ埋め・存在しない日付の解決規則という解釈の余地を抱える。T-23 で `ResolverStyle.STRICT` を足したのもその後始末だった。③ の KMP 移植では `java.time` を捨てるため、この形式を 1 バイトも変えずに移すという縛りが移植の自由度を奪う
- 対応方針: `INTEGER` のエポックミリ秒に変える。`AppDatabase` の `version` を 2 に上げ、**マイグレーションは書かない**——未公開アプリなので既存インストールは入れ直す。`fallbackToDestructiveMigration` も足さず、旧 DB を持つ端末は起動時に落とす（黙ってデータが消えるより気づけるほうを採る）
- 優先度の根拠: 保存形式は後から変えるほど高くつく。③ の段 1 に入る前に済ませる（当時の段番号。段取りは後に 5 段へ改訂され、ここで言う段 1＝KMP 移植は現在の段 3）
- 関連: T-14 のレビューを機にオーナーが方針を決めた（[KMP 化検討](../検討/32_KMP化検討.md) §4 と[その決定ログ](../検討/log_32_KMP化検討.md)）。T-17 の着手条件がこれで変わる

### T-22

- 背景: Navigation 3 では `rememberViewModelStoreNavEntryDecorator` が効いていないと `hiltViewModel()` が Activity の `ViewModelStore` にフォールバックし、全画面の ViewModel が Activity スコープに昇格する（Navigation 2 では画面ごとのスコープだった）。挙動が変わるのに、現在のテストはこれが壊れても全件緑になる
- 対応方針: ViewModel の一時状態を外から観測して確かめる。たとえばカテゴリー一覧で追加ダイアログを開き、戻ってから再訪したときにダイアログが閉じていること
- 優先度の根拠: 画面を離れても状態が残るという分かりにくい壊れ方をする。③ の段 1 で DI を Koin に替えるときに同じ穴を踏む
- 関連: T-18 のレビューで test-reviewer が指摘

### T-26

- 背景: ViewModel のテストが 1 件も無い。③ の段 1 で Hilt を Koin に、`java.time` を `kotlinx-datetime` に、Room を Room KMP に替えるが、UI の挙動が移植前と同じであることを機械で確かめる手段がこれしかない（インストルメンテーションテストは遷移だけを見ており、画面の中身は手動確認に残っている）
- 対応方針: CLAUDE.md「アーキテクチャ / UI 層」の ViewModel テストの条項に従って書く
- 優先度の根拠: 段 1 の前提。網が無いまま移植すると、壊れたことに気づくのが実機を触ったときになる
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §8 が「段 1 に入る前に Android 側で先に書く」と定めている。T-22 を同じ作業で閉じる

### T-19

- 背景: DI が Hilt。③ の段 3 で `commonMain` へ移すときに Koin へ替えると spec §5 で決めているが、Hilt のままモジュールを分割すると、各モジュールに `hilt-android` と `ksp(hilt-compiler)` を配線する作業が発生し、それを段 3 で丸ごと捨てることになる
- 対応方針: [段 1 実装計画](./archive_23_kmp段1実装計画.md)。Koin モジュールを層ごとに 3 本に分け、段 2 でファイルを移すだけで済む形にする
- 優先度の根拠: 段 2 の前提。差し替えの網（T-26 の ViewModel テスト 90 件）はすでにある
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §5・§10

### T-20

- 背景: 単一モジュール `:app`。③ の目標構成は層別 4 モジュールで、依存の向きを Gradle に強制させる
- 対応方針: [段 2 実装計画](./archive_23_kmp段2実装計画.md)。Android のまま分割し、KMP 化は段 3 に残す
- 着手条件: T-19 の完了
- 優先度の根拠: 段 3（KMP 移植）の前提。ここで CI とツールの壊れ（spec §11）を先に塞いでおくと、段 3 では移植そのものだけを見ればよくなる
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §3・§11

### T-29

- 背景: 段 2 の合否判定を「既存のテストが 1 行も変わらず緑」に置いたため、**Kotlin の package を一切動かさずにモジュールを割った**。分割が挙動を変えていないことの証明としては正しく機能したが、構造としては中途半端で、package を見てもどのモジュールのコードか分からない。`io.github.obaya884.rebuy.di` は 4 モジュールに、ルートの `io.github.obaya884.rebuy` は 3 モジュールに散っている（split package）。`:androidApp` は namespace が `.app` なのに中の package はルートのまま
- 対応方針: package と `namespace` をモジュール境界に揃える。対応表は [KMP 化検討](../検討/32_KMP化検討.md) §3 が正。**テストの package と import が変わる**ので、段 2 とは別の PR にする——段 2 の機械判定を濁らせない
- 着手条件: T-20 のマージ直後。段 3 に入る前に済ませる
- 優先度の根拠: 依存の向きを人が意識する手がかりが Gradle のビルドファイルにしか無い状態。split package は段 3 の KMP 化でも扱いが面倒になる。段 3 の `commonMain` 移行と同時でも安いが、先に済ませたほうが移行時に見るものが減る
- 関連: T-20 のレビューで code-quality-reviewer が split package を指摘し、オーナーが構造の不揃いを問題として挙げた

### T-28

- 背景: 4 モジュールが `repositories` ・SDK レベル・`compileOptions` を各自に書いている。段 2 までは書き忘れると必ずビルドが落ちる自己修正する重複なので許容してきたが、段 3 で KMP のターゲット定義と `commonMain` / `androidMain` / `iosMain` の source set が 3 モジュールに入ると、**書き忘れが静かに効く**ようになる（ターゲットが 1 つ足りなくてもビルドは通り、iOS で動かないことに後で気づく）
- 対応方針: `build-logic` を included build（Composite Build）にし、convention plugin を置く
- 着手条件: 段 3（KMP 移植）の着手時。**2 つに割って行う**——convention plugin で KMP のターゲット定義を書くには KMP DSL の実物を知っている必要があり、「最初にまとめて」は順序が成立しない
  - **T-28a**（段 3 の最初）: build-logic を立て、`repositories` と `compileOptions` と SDK レベルだけを寄せる。Android 専用ではないので書き直しにならない。**完了**（2026-08-29）
  - **T-28b**（`:shared:data` を KMP 化した直後）: ターゲット定義と source set を抽出する。書き忘れが起きうるのは 2 モジュール目以降なので、価値が出るのもここから
- 優先度の根拠: 段 3 の作業量に直接効く。ただし段 2 までは無くても困らない
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §3。T-20 のレビューで code-quality-reviewer が重複を指摘し、オーナーが導入時期を決めた

### T-32

- 背景: KMP ライブラリプラグインはホストテストが既定で無効で、開け忘れると件数が 0 のままビルドが緑になる。段 3 のステップ 5 では、3 モジュールが KMP になったことで `./gradlew testDebugUnitTest` が一致するモジュールを失い、**0 件で緑**になった（入口を `testAndroidHostTest` へ移して対処済み）。いま 122 件を守っているのは「人が数えている」ことだけで、[段 3 実装計画](./archive_23_kmp段3実装計画.md)の残りステップは source set の付け外しが最も多い区間に入る
- 対応方針: ホストテストの source set を持つモジュールで実行件数が 0 なら落とす検査を `rebuy.android.base` に置く。**モジュールごとの期待件数は書かない**——書くと今度はその数字の更新漏れが起きる。`src/androidHostTest` があるのに 0 件、という条件なら `:shared:domain`（テストを持たない）を自動的に除外できる。`afterSuite` は設定キャッシュで使えないので、結果 XML を読む形になる
- 着手条件: 段 3 の**ステップ 13 の前**。ステップ 13 の後は `androidHostTest` が `KoinModulesTest` 1 件だけになり、`commonTest` が 0 件になっても両タスクが緑で通ってしまう（T-31 のステップ 10 のレビューで test-reviewer が指摘）。それ以前に入れると移送中の一時的な 0 件で作業が止まるので、ちょうどこの位置
- 優先度の根拠: 段 3 で 2 回踏んだ形の事故を機械で止める。ただし段 3 の間は人が件数を数える運用が続くので急がない
- 関連: T-31 のステップ 5 のレビューで test-reviewer が指摘

### T-31

- 背景: `:shared:*` の 3 モジュールはまだ `com.android.library` で、コードは Android 専用のまま。③ の目的（iOS へ到達する）にはここを KMP 化して `commonMain` へ移す必要がある
- 対応方針: [段 3 実装計画](./archive_23_kmp段3実装計画.md)。各モジュールで「KMP 化」と「`commonMain` への移送」を別のコミットに分ける——前者は同じコードが同じターゲットでコンパイルされるだけなので Android の挙動が構造的に変わりようがなく、壊れたときに原因が Gradle の構成かコードの移送かに必ず切り分かる。iOS はスタブと全画面の 2 回出す
- 着手条件: T-28a（build-logic の立ち上げ）が済んでいること。実際には計画のステップ 1 がそれにあたる
- 優先度の根拠: ③ の本体。段 4（iOS シェル）の前提
- 関連: [KMP 化検討](../検討/32_KMP化検討.md) §3〜§9・§11・§13。T-28a / T-28b を内包する

### T-42

- 背景: Android には GMD の instrumented テストと `adb` のハーネス（`uiautomator dump` ＋ `screencap` の 21 画面走査）があるが、**iOS には画面を触る手段が何も無い**。ステップ 15 では macOS の合成クリック（`CGEvent`）でしのいだが、**人が別のウィンドウを触るとクリックが他のアプリへ飛ぶ**（実際に Chrome へ飛んだ）。誤操作の危険があり、成否も安定しない。**ステップ 15 で見つけた落とし穴 22 のような iOS 固有の不具合は、Android のテストでは 1 件も捕まらない**
- 対応方針: 次の 3 つを比べる。**本命は (c)**——instrumented テストを common へ寄せられれば、Android と iOS の網が 1 本になる
  - (a) `idb`（fb-idb）を入れる。`idb ui tap` がフォーカスに依存せずシミュレータへ直接送るので `adb shell input tap` と同じ形になり、画素比較の走査を iOS でもそのまま回せる。導入は簡単だが CI には載せにくい
  - (b) `iosApp/` に XCUITest ターゲットを足す。`xcodebuild test` で CI に載る正攻法だが、テストを Swift で二重に書くことになる
  - (c) Compose Multiplatform の `runComposeUiTest` を `commonTest` に置く。UI 階層ではなく Compose のセマンティクスを直接叩くので、**`NavigationTest` など既存の instrumented テストを `commonTest` へ移せる可能性がある**。文言を `Res.string.*` から引けるので、既存の方針をそのまま延長できる
  - (d) Maestro。YAML の flow 1 本が Android と iOS の両方で回り、Swift も Kotlin も書かない。**ただし iOS ではネイティブのアクセシビリティツリーを見るので、1 枚の `UIView` に描く Compose の階層が見えるかが分かれ目**（Maestro 側に issue #1549 がある。CMP 1.8.0 で同期が遅延化され `testTag` が `accessibilityIdentifier` に対応づくとされており、本リポジトリの 1.12.0 では直っている可能性が高いが**未実測**）。採るなら **`testTag` で選び `testTag` で assert する**——YAML から `Res.string.*` は引けないので、文言で assert すると「文言を変えたらテストが古い値を主張する」状態に戻る
- 分担の見立て: **(c) と、(b)/(d) は競合ではない。** `NavigationStateRestorationTest`（プロセス death からの復元）と `LicenseLibrariesTest`（資産が APK に入っているか）は実物を起動しないと見られないので、厚みを (c) に置き、実物を起動する薄い層を (b) か (d) で持つ形になる
- **結果（2026-08-31）: (c) `runComposeUiTest` を採り、`shared/ui/src/iosTest` に `NavigationIosTest`（10 件）と `IosTestKoinTest`（1 件）を置いた。** 6 画面の遷移・戻る矢印・ボトムナビ・空状態・品目がある行・カゴタブでの文言の出し分けを見る。DAO は [T-48](#t-48) の差し替えで `FakeDatabase` を使う。CI は `ios` ジョブがそのまま拾うので変更していない。**(d) Maestro は候補として生きている**（アクセシビリティ階層が見えることは実測済み）が、(c) で足りたので採らなかった。実測の内訳と判断の経緯は log_23
- **達成していないこと**: **落とし穴 22 は再現せず、この網が止めるとは言えない**（[T-41](./23_技術改善バックログ.md#t-41)）
- **残った穴**: 本物の Room を通らない（[T-35](#t-35)）／実物の `.app` を起動しない（[T-46](./23_技術改善バックログ.md#t-46)）／ライセンス一覧の中身を見ていない（[T-39](./23_技術改善バックログ.md#t-39)）
- 関連: T-31 のステップ 15。[T-35](#t-35)（iOS の DB と DI のスモークテスト）とあわせて考える

### T-48

- 背景: [T-42](#t-42) で置いた `NavigationIosTest` は Koin をテスト自身が起動するため、`AppDatabase` が `NSDocumentDirectory` の**実ファイル**になっていた。**1 本の DB をテスト間でも実行と実行の間でも共有する**ので、品目が要る経路を書けず、書き込むテストを足した瞬間に状態が漏れる。**[T-21](./23_技術改善バックログ.md#t-21) では代わりにならない**——あちらは本物の `ReBuyApplication` が起動した Koin を差し替える Android 限定の手で、テスト自身が `startKoin` する iOS には当てはまらない。**着手より先に CI で露見した**——新品のシミュレータには `data/Documents` が無く、T-42 の PR で 8 件すべてが `Unable to open database` で落ちた
- **結果（2026-08-31）: T-42 と同じ PR で入れた。** `startTestKoin()` が Koin 起動の直後に `ItemDao` / `CategoryDao` を `FakeDatabase` のものへ差し替え、テストごとに空へ戻す。Room に触らないのでファイルを作らない。**iOS で本物の Room が動くことは見ていない**（[T-35](#t-35)）。作りの理由と実測は log_23 と `IosTestKoin.kt` の KDoc
- 関連: [T-21](./23_技術改善バックログ.md#t-21)（Android 側の同じ問題。手は違う）／ [T-35](#t-35) ／ [T-41](./23_技術改善バックログ.md#t-41)

### T-35

- 背景: iOS で走るテストは Converter の純粋関数だけで、`AppDatabaseConstructor` の `actual` が KSP で生成されているか、`BundledSQLiteDriver` と Documents パスの配線が通るかは**シミュレータで起動するまで分からない**。CLAUDE.md の「Koin は依存グラフをコンパイル時に検証しない」から `KoinModulesTest` / `KoinGraphTest` を置いた理屈が、iOS 側では丸ごと抜けている
- 対応方針: `shared/data/src/iosTest` に 1 件。`Room.inMemoryDatabaseBuilder<AppDatabase>()`（native にはある。common には無いので `commonTest` には置けない）＋ `BundledSQLiteDriver` で DB を組み、`insertItem` → `getAllItems().first()` の往復を見る。生成 `actual`・driver・`TypeConverters` が本物の SQLite を通ることまで一度に固定できる
- **結果（2026-08-31）: `shared/data/src/iosTest` に `AppDatabaseIosTest`（3 件）と `DataModuleIosTest`（1 件）を置いた。** 前者は in-memory の Room で往復・`@Relation` の join・null 分岐を、後者は**本番の `dataModule` から `AppDatabase` と DAO が解けること**を見る（クエリは投げない）
- **残る穴は「本番の DB ファイルを実際に開くこと」だけ**（[T-46](./23_技術改善バックログ.md#t-46)）。本番の DB 設定そのものも守れていない——判断と実測は log_23
- 優先度の根拠: 段 3 を通して iOS 側は手動確認しか無い。ステップ 15 でシミュレータの往復（カテゴリー追加・削除）が通ることは人の目で見たが、**それを繰り返せる形にはなっていない**
- 関連: T-31 のステップ 8 のレビューで test-reviewer が指摘。iOS の網という意味では [T-42](./closed_23_技術改善バックログ.md#t-42) と地続き
