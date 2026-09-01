#!/usr/bin/env python3
"""check-ios-signing.py の検査。

使い方:
  python3 scripts/test/check-ios-signing_test.py

**なぜ要るか。** あの検査の本体は正規表現で、`SECRET_KEYS` から 1 語落ちても
`is_literal` の条件が反転しても、正常系（＝いまのリポジトリ）は緑のままになる。
「網を広げた変更が、それ自身で新しい穴を作る」事故が繰り返し起きた（log_23）。
人手の確認は続かない。

**終了コードだけでは足りない。** 鳴るべきケースでは**対象のパスと設定名が
報告に載っている**ことまで見る——そうしないと、指摘を 1 件も出さずに落ちる変異や、
行番号・キー名を固定値にする変異が素通りする（実測）。

**ケースは動機の範囲に留める。** 守るのは「Xcode がプロジェクトを開いた拍子に
pbxproj へ焼き込む署名設定をコミットさせない」こと。それを越える形（plist・
fastlane・ワークフロー）は検査側で見ないと決めているので、ここにも置かない。

使い捨ての git リポジトリを毎ケース作り、検査を作業ツリー・index の両モードで
回す。**仕込む値は架空の SENTINEL で、出力に 1 文字も出ないことも毎ケース
確かめる**（値を出さないのがあの検査の設計の核）。
"""

import importlib.util
import json
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
CHECK = os.path.join(ROOT, "scripts", "check-ios-signing.py")

# 実データに似せない。出力に混ざったとき grep 一発で見つかる語にする
SENTINEL = "SENTINEL0001"

PBXPROJ = "iosApp/iosApp.xcodeproj/project.pbxproj"
XCCONFIG = "iosApp/Configuration/Config.xcconfig"
LOCAL = "iosApp/Configuration/Local.xcconfig"

# 署名の実体のうち、**動機の中心にあって外せないもの**。3 点セットの突き合わせは
# 3 か所の一致しか見ないので、**揃えて消す編集**（本書が指示している形）が素通りする。
# 実測で 18 件中 16 件が協調削除で緑のまま通り、`.mobileprovision` と `.p12` を
# 見逃す検査になった。**ここはリテラルで持つ**——モジュールから引くと自己参照になる
REQUIRED = (
    ".mobileprovision",
    ".provisionprofile",
    ".p12",
    ".p8",
    ".pem",
    ".key",
    ".keychain",
    ".keychain-db",
    ".jks",
    ".keystore",
    ".certsigningrequest",
    ".ipa",
    "keystore.properties",
)
# 実体でない拡張子が混ざると、無関係なファイルが軒並み鳴る（`.md` を足すと docs 全体）
NOT_MATERIAL = (".md", ".kt", ".json", ".plist", ".xcconfig", ".pbxproj", ".swift", ".txt")

CLEAN_PBXPROJ ='\t\t\t\tDEVELOPMENT_TEAM = $(TEAM_ID);\n\t\t\t\tPROVISIONING_PROFILE_SPECIFIER = "";\n'
CLEAN_XCCONFIG = 'TEAM_ID =\nCODE_SIGN_STYLE = Automatic\n#include? "Local.xcconfig"\n'

# (名前, 追記先, 追記する行, 報告に出るべき設定名。None なら鳴ってはいけない)
CASES = [
    # --- Xcode が pbxproj へ焼き込む形。ここが動機そのもの ---
    ("素", PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = {SENTINEL};", "DEVELOPMENT_TEAM"),
    ("値の引用符付き", PBXPROJ, f'\t\t\t\tDEVELOPMENT_TEAM = "{SENTINEL}";', "DEVELOPMENT_TEAM"),
    ("条件付きキー", PBXPROJ, f'\t\t\t\t"DEVELOPMENT_TEAM[sdk=iphoneos*]" = {SENTINEL};', "DEVELOPMENT_TEAM"),
    ("条件付きキー 2 つ", PBXPROJ, f'\t\t\t\t"DEVELOPMENT_TEAM[sdk=iphoneos*][arch=arm64]" = {SENTINEL};', "DEVELOPMENT_TEAM"),
    ("TargetAttributes のアンダースコア無し", PBXPROJ, f"\t\t\t\tDevelopmentTeam = {SENTINEL};", "DevelopmentTeam"),
    ("空白なし", PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM={SENTINEL};", "DEVELOPMENT_TEAM"),
    ("1 行に複数", PBXPROJ, f"\t\t\t\tCODE_SIGN_STYLE = Manual; DEVELOPMENT_TEAM = {SENTINEL};", "DEVELOPMENT_TEAM"),
    ("行の途中", PBXPROJ, f"\t\t\t\tbuildSettings = {{DEVELOPMENT_TEAM = {SENTINEL}; }};", "DEVELOPMENT_TEAM"),
    ("手動署名の証明書名", PBXPROJ, f'\t\t\t\tCODE_SIGN_IDENTITY = "Apple Development: N ({SENTINEL})";', "CODE_SIGN_IDENTITY"),
    ("プロファイル名", PBXPROJ, f'\t\t\t\tPROVISIONING_PROFILE_SPECIFIER = "{SENTINEL}";', "PROVISIONING_PROFILE_SPECIFIER"),
    ("旧形式のプロファイル UUID", PBXPROJ, f'\t\t\t\tPROVISIONING_PROFILE = "{SENTINEL}";', "PROVISIONING_PROFILE"),
    ("チーム名", PBXPROJ, f'\t\t\t\tDevelopmentTeamName = "{SENTINEL} Inc.";', "DevelopmentTeamName"),
    ("組織名", PBXPROJ, f'\t\t\t\tORGANIZATIONNAME = "{SENTINEL} Inc.";', "ORGANIZATIONNAME"),
    # 値に = が入る形。伏せ字を引き算で作ると、ここで前半が漏れる
    ("値に = を含む", PBXPROJ, f'\t\t\t\tPROVISIONING_PROFILE_SPECIFIER = "a={SENTINEL}";', "PROVISIONING_PROFILE_SPECIFIER"),
    # pbxproj では `//` を切らない。切ると値ごと消えて見逃す
    ("値が // で始まる", PBXPROJ, f'\t\t\t\tDEVELOPMENT_TEAM = "//{SENTINEL}";', "DEVELOPMENT_TEAM"),
    # 参照の後ろに実値が続く形。先頭 1 文字で判定すると素通りする
    ("$(inherited) の後ろに実値", PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = $(inherited) {SENTINEL};", "DEVELOPMENT_TEAM"),
    ("入れ子参照の後ろに実値", PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = $(A_$(B)) {SENTINEL};", "DEVELOPMENT_TEAM"),
    ("括弧なし参照の後ろに実値", PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = $TEAM_ID {SENTINEL};", "DEVELOPMENT_TEAM"),
    # 空白なしで参照に連結する形。参照の境界が緩いと実値ごと食われる
    ("参照に空白なしで連結", PBXPROJ, f"\t\t\t\tPROVISIONING_PROFILE_SPECIFIER = $(APP_NAME){SENTINEL};", "PROVISIONING_PROFILE_SPECIFIER"),
    # 値は秘密でないが、xcconfig 側の指定が黙って負ける
    ("pbxproj の CODE_SIGN_STYLE", PBXPROJ, "\t\t\t\tCODE_SIGN_STYLE = Manual;", "CODE_SIGN_STYLE"),
    ("pbxproj の ProvisioningStyle", PBXPROJ, "\t\t\t\tProvisioningStyle = Manual;", "ProvisioningStyle"),
    # --- pbxproj で通るべきもの ---
    ("空値", PBXPROJ, '\t\t\t\tDEVELOPMENT_TEAM = "";', None),
    ("参照だけ", PBXPROJ, "\t\t\t\tDEVELOPMENT_TEAM = $(TEAM_ID);", None),
    ("波括弧の参照だけ", PBXPROJ, "\t\t\t\tDEVELOPMENT_TEAM = ${TEAM_ID};", None),
    ("括弧なし参照だけ", PBXPROJ, "\t\t\t\tDEVELOPMENT_TEAM = $TEAM_ID;", None),
    ("入れ子参照だけ", PBXPROJ, "\t\t\t\tDEVELOPMENT_TEAM = $(A_$(B));", None),
    ("$(inherited) だけ", PBXPROJ, "\t\t\t\tDEVELOPMENT_TEAM = $(inherited);", None),
    ("アドホック署名", PBXPROJ, '\t\t\t\tCODE_SIGN_IDENTITY = "-";', None),
    ("Xcode が既定で書く一般名", PBXPROJ, '\t\t\t\t"CODE_SIGN_IDENTITY[sdk=iphoneos*]" = "iPhone Developer";', None),
    ("配布用の一般名", PBXPROJ, '\t\t\t\tCODE_SIGN_IDENTITY = "Apple Distribution";', None),
    ("関係のないキー", PBXPROJ, f"\t\t\t\tOTHER_LDFLAGS = $(inherited) -framework {SENTINEL};", None),
    ("別物の長いキー", PBXPROJ, f"\t\t\t\tAPP_DEVELOPMENT_TEAM_NOTE = {SENTINEL};", None),
    # --- xcconfig。設定の置き場所なので、実値が入ったら仕組みが破れている ---
    ("xcconfig に実値", XCCONFIG, f"TEAM_ID = {SENTINEL}", "TEAM_ID"),
    ("xcconfig の条件付きキー", XCCONFIG, f"TEAM_ID[sdk=iphoneos*] = {SENTINEL}", "TEAM_ID"),
    ("= ; の後ろに実値", XCCONFIG, f"DEVELOPMENT_TEAM = ;{SENTINEL}", "DEVELOPMENT_TEAM"),
    ("行頭コメント内の実値", XCCONFIG, f"// 例: DEVELOPMENT_TEAM = {SENTINEL}", "DEVELOPMENT_TEAM"),
    ("行末コメント内の設定名", XCCONFIG, f"FOO = bar // TEAM_ID = {SENTINEL}", "TEAM_ID"),
    # 左辺も監視対象の形。値を行末まで食うと、1 行 1 マッチになって後半が隠れる
    ("正当な代入＋行末コメント内の実値", XCCONFIG, f"DEVELOPMENT_TEAM = $(TEAM_ID) // 例: DEVELOPMENT_TEAM = {SENTINEL}", "DEVELOPMENT_TEAM"),
    ("一般名＋行末コメント内の実値", XCCONFIG, f"CODE_SIGN_IDENTITY = Apple Development // DEVELOPMENT_TEAM = {SENTINEL}", "DEVELOPMENT_TEAM"),
    # 値は `//` の手前で終わる。行ごと落とすと、この実値を見逃す
    ("実値＋行末コメント", XCCONFIG, f"TEAM_ID = {SENTINEL} // 手元用", "TEAM_ID"),
    # 終端は `//` であって `/` ではない。**実値は `/` の後ろだけに置く**——
    # 前にも置くと、`/` 1 個で切る実装でも前半だけで鳴ってしまい、変異を殺せない
    ("値の / の後ろに実値", XCCONFIG, f"PROVISIONING_PROFILE_SPECIFIER = $(APP_NAME)/{SENTINEL}", "PROVISIONING_PROFILE_SPECIFIER"),
    # --- xcconfig で通るべきもの ---
    ("行末コメント付きの正当な代入", XCCONFIG, "DEVELOPMENT_TEAM = $(TEAM_ID)  // 手元は Local で入れる", None),
    ("空値＋行末コメント", XCCONFIG, "TEAM_ID = // 各自で手元に入れる", None),
    # 一般名は `GENERIC_VALUES` の全量を置く。1 語落としても正常系は緑のままなので
    ("一般名", XCCONFIG, "CODE_SIGN_IDENTITY = Apple Development", None),
    ("一般名（旧配布用）", XCCONFIG, "CODE_SIGN_IDENTITY = iPhone Distribution", None),
    ("一般名（macOS 開発）", XCCONFIG, "CODE_SIGN_IDENTITY = Mac Developer", None),
    ("一般名（Developer ID）", XCCONFIG, "CODE_SIGN_IDENTITY = Developer ID Application", None),
    ("xcconfig の CODE_SIGN_STYLE は正当", XCCONFIG, "CODE_SIGN_STYLE = Manual", None),
    ("設定名に = を続けないコメント", XCCONFIG, "// DEVELOPMENT_TEAM の代入より後で入れても効く", None),
    # --- 見ないと決めた範囲。塞ぐと、直しようのない誤検知が出る側 ---
    # 接頭辞を付けた自作キー。**塞ぐと下の 2 つが道連れになる**（値の中の define も
    # 「識別子の途中の TEAM_ID」なので、キーの前の錨を外すと区別できない）
    ("接頭辞を付けた自作キー", XCCONFIG, f"MY_TEAM_ID = {SENTINEL}", None),
    ("アンダースコア無しの接頭辞", XCCONFIG, f"MYTEAM_ID = {SENTINEL}", None),
    ("値の中の define", XCCONFIG, "OTHER_SWIFT_FLAGS = $(inherited) -DUSE_TEAM_ID=1", None),
    ("値の中の define（pbxproj）", PBXPROJ, '\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = ("$(inherited)", "MYTEAM_ID=1");', None),
    ("何も足さない", None, None, None),
]

failures = []


def fail(name, detail):
    failures.append(f"{name}: {detail}")


def git(cwd, *argv):
    # 手元のグローバル設定に引きずられないよう、署名とフックは明示的に切る
    # （このリポジトリ自身が core.hooksPath を使うので、そのまま commit が落ちる）
    isolate = ("-c", "commit.gpgsign=false", "-c", "core.hooksPath=/dev/null")
    done = subprocess.run(["git", *isolate, *argv], cwd=cwd, capture_output=True)
    if done.returncode != 0:
        raise SystemExit(f"テストの準備に失敗: git {' '.join(argv)}\n{done.stderr.decode()}")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def write(root, path, text):
    full = os.path.join(root, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(text)


def make_repo(tmp, name):
    """毎ケース作り直す。前のケースの汚れが次に漏れない。

    **名前が重複したら落とす**（`exist_ok` を付けない）。使い回すと `git init` は
    再初期化されるだけで前のケースの追跡ファイルが残り、誤って緑になる（実際に踏んだ）。
    """
    root = os.path.join(tmp, name)
    os.makedirs(root)
    write(root, PBXPROJ, CLEAN_PBXPROJ)
    write(root, XCCONFIG, CLEAN_XCCONFIG)
    git(root, "init", "-q", "-b", "main", ".")
    git(root, "config", "user.email", "test@example.invalid")
    git(root, "config", "user.name", "test")
    # 既定（true）を明示する。手元が false だと `git ls-files -z` の必要性を確かめられない
    git(root, "config", "core.quotepath", "true")
    # **`-f` を付ける。** グローバルな excludesFile が仕込みを無視すると偽の合格になる
    git(root, "add", "-Af")
    git(root, "commit", "-qm", "init")
    return root


DIRTY = f"\t\t\t\tDEVELOPMENT_TEAM = {SENTINEL};"


def dirty(root, path=PBXPROJ, text=DIRTY, stage=True):
    """仕込みを 1 行足す。**既定は index にも載せる**——載せ忘れると staged 側が
    綺麗なまま「鳴らないのが正しい」ことになり、偽の合格になる（実際に踏んだ）。
    """
    with open(os.path.join(root, path), "a", encoding="utf-8") as f:
        f.write(text + "\n")
    if stage:
        git(root, "add", "-Af")


def run(root, mode, note, cwd=None, extra=()):
    """**検査の起動はここに集約する。** どの経路でも守るべき 2 つを毎回見るため。

    `note` に既定値を置かない——渡し忘れると、失敗がどのケースのものか分からなくなる。
    """
    if mode not in ("worktree", "staged"):
        raise SystemExit(f"知らないモード: {mode}")
    argv = [sys.executable, CHECK] + (["--staged"] if mode == "staged" else []) + list(extra)
    done = subprocess.run(argv, cwd=cwd or root, capture_output=True, text=True)
    output = done.stdout + done.stderr
    # **値の露出はどの経路でも許さない。** check_case の中だけで見ていたときは、
    # 構造検査や「2 件同時」の経路が素通りしていた（実測）
    if SENTINEL in output:
        fail(note, f"出力に値が出ている\n{output}")
    # **直し方に破壊的なコマンドを出さない**（設計条項。docstring が明示している）
    for danger in ("git checkout", "git restore", "git reset"):
        if danger in output:
            fail(note, f"直し方に破壊的なコマンドが出ている: {danger}\n{output}")
    return done.returncode, done.stdout, done.stderr


# 値が秘密でない CODE_SIGN_STYLE 系は見出しを分ける（「秘密が漏れた」と読ませない）。
# **本体の `STYLE_KEYS`（正規表現の断片）とは別物**なので、名前で区別する
STYLE_KEY_NAMES = ("CODE_SIGN_STYLE", "ProvisioningStyle")


def check_case(root, name, path=None, key=None, line=None, modes=("worktree", "staged")):
    """鳴るなら exit 1 と「パス・行番号・設定名・見出しが報告に載ること」まで見る。

    **終了コードだけでは足りない。** 指摘を 1 件も出さずに落ちる変異や、
    行番号・設定名を固定値にする変異が素通りする。
    """
    for mode in modes:
        code, out, err = run(root, mode, f"[{mode}] {name}")
        wanted = 1 if key else 0
        if code != wanted:
            fail(f"[{mode}] {name}", f"exit={code}（{wanted} が欲しい）\n{out}{err}")
        elif key:
            # **区切りの空白まで見る。** パスが別のパスの接頭辞になると、短い側の
            # 報告が無いときに長い側のブロックを掴んで照合してしまう
            block = next((b for b in err.strip().split("\n\n") if path and b.startswith(path + " ")), None)
            if block is None:
                fail(f"[{mode}] {name}", f"対象のパスで始まる報告が無い\n{err}")
                continue
            wanted_line = f"{line} 行目: {key}" if line else f"行目: {key}"
            if wanted_line not in block:
                fail(f"[{mode}] {name}", f"報告に「{wanted_line}」が出ていない\n{block}")
            style = key in STYLE_KEY_NAMES
            head, other = ("署名方式", "署名の値") if style else ("署名の値", "署名方式")
            if head not in block or other in block:
                fail(f"[{mode}] {name}", f"見出しが「{head}」だけになっていない\n{block}")
            # 直し方の中身。破壊的コマンドの不在だけ見ていると、誘導ごと消せてしまう
            # 方式は Config.xcconfig で、秘密は Local.xcconfig で。**誘導先が違う**。
            # **見るのは最終行だけ**——見出しにも Config.xcconfig が出るので、
            # ブロック全体を見ると誘導を消す変異が見出しで満たされてしまう
            elif (XCCONFIG if style else LOCAL) not in block.rsplit("\n", 1)[-1]:
                fail(f"[{mode}] {name}", f"直し方が置き場所を示していない\n{block}")
        elif mode == "staged" and out:
            # pre-commit では黙る
            fail(f"[{mode}] {name}", f"通ったのに stdout がある\n{out}")
        elif mode == "worktree" and not out:
            # CI では通ったことを出す。無言だと「走ったのか」が分からない
            fail(f"[{mode}] {name}", "通ったのに stdout が空")


def expect_exit(name, code, wanted, detail=""):
    if code != wanted:
        fail(name, detail or f"exit={code}（{wanted} が欲しい）")


if not CASES:
    raise SystemExit("ケースが 0 件です")

with tempfile.TemporaryDirectory() as tmp:
    for index, (name, path, text, key) in enumerate(CASES):
        root = make_repo(tmp, f"case{index}")
        at = None
        if path is not None:
            with open(os.path.join(root, path), encoding="utf-8") as f:
                at = len(f.read().splitlines()) + 1
            dirty(root, path, text)
        check_case(root, name, path, key, at)

    # --- 2 モードの読み分け。実際の事故はこの形で起きた（log_23 2026-09-02） ---

    root = make_repo(tmp, "unstaged")
    dirty(root, stage=False)
    check_case(root, "作業ツリーだけ汚れている", PBXPROJ, "DEVELOPMENT_TEAM", modes=("worktree",))
    check_case(root, "作業ツリーだけ汚れている（index は綺麗）", modes=("staged",))

    root = make_repo(tmp, "staged-only")
    dirty(root)
    write(root, PBXPROJ, CLEAN_PBXPROJ)
    check_case(root, "index だけ汚れている", PBXPROJ, "DEVELOPMENT_TEAM", modes=("staged",))
    check_case(root, "index だけ汚れている（作業ツリーは綺麗）", modes=("worktree",))

    # --- 「見つけたものは全部出す」。1 件目で終わると重い指摘が隠れる ---
    # 走査対象 2 つ ＋ 実体 2 つ。**種類ごとに 2 つ以上**置かないと、
    # ループに `break` を入れる変異も、ブロックの区切り（空行）を壊す変異も素通りする

    root = make_repo(tmp, "many-problems")
    dirty(root)
    dirty(root, XCCONFIG, f"TEAM_ID = {SENTINEL}")
    write(root, "keystore.properties", "x\n")
    write(root, "build/app.ipa", "x\n")
    git(root, "add", "-Af")
    expected = (PBXPROJ, XCCONFIG, "keystore.properties", "build/app.ipa")
    code, out, err = run(root, "worktree", "4 件同時")
    expect_exit("4 件同時", code, 1)
    blocks = err.strip().split("\n\n")
    if len(blocks) != len(expected):
        fail("4 件同時", f"報告が {len(blocks)} ブロックしかない（{len(expected)} が欲しい）\n{err}")
    for path in expected:
        if not any(b.startswith(path + " ") for b in blocks):
            fail("4 件同時", f"{path} で始まるブロックが無い\n{err}")

    # --- 構造の検査 ---

    for order, (name, path, content) in enumerate([
        ("追跡された Local.xcconfig", LOCAL, f"TEAM_ID = {SENTINEL}\n"),
        # `.gitignore` はディレクトリを問わず効く。こちらも basename で見ないと網がずれる
        ("別の場所の Local.xcconfig", "Local.xcconfig", "SOMETHING = 1\n"),
        # 大小の違いは macOS では作れてしまう。basename の照合は小文字に落として比べる
        ("小文字の local.xcconfig", "iosApp/Configuration/local.xcconfig", f"TEAM_ID = {SENTINEL}\n"),
        ("追跡された xcuserdata", "iosApp/iosApp.xcodeproj/xcuserdata/who.xcuserdatad/x", "x\n"),
        # 照合は前後ともパス要素の境界で切る。**先頭境界は直下に置かないと確かめられない**
        ("リポジトリ直下の xcuserdata", "xcuserdata/who.xcuserdatad/x", "x\n"),
        # 実体と xcuserdata の両方に当たる形。報告が 2 つに割れると直し方も重複する
        ("xcuserdata 配下の実体", "iosApp/iosApp.xcodeproj/xcuserdata/who.xcuserdatad/x.p12", "x\n"),
        ("追跡された署名の実体（大文字混じり）", "iosApp/dev.CertSigningRequest", "x\n"),
        ("拡張子を持たない実体", "keystore.properties", "x\n"),
        # 実体名の照合は basename。リポジトリ直下にしか置かないと、取りこぼす実装が通る
        ("サブディレクトリの実体", "androidApp/keystore.properties", "x\n"),
        # 名前側も小文字に落として比べる。大小の網が接尾辞側にしか無いと、ここが抜ける
        ("大文字混じりの実体名", "KeyStore.Properties", "x\n"),
        ("配布物", "build/app.ipa", "x\n"),
    ]):
        # **番号で必ず別のディレクトリにする。** 名前から作ると衝突し、前のケースの
        # 汚れを引き継いで誤って緑になる（実際に踏んだ）
        root = make_repo(tmp, f"structure{order}")
        write(root, path, content)
        git(root, "add", "-f", path)
        for mode in ("worktree", "staged"):
            code, out, err = run(root, mode, f"[{mode}] {name}")
            expect_exit(f"[{mode}] {name}", code, 1, f"exit={code}\n{out}{err}")
            # **ブロックはちょうど 1 つ。** 走査対象からの除外を落とすと、同じファイルが
            # 2 つの見出しで報告され、片方の直し方が「Local.xcconfig へ移せ」になる
            hit = [b for b in err.strip().split("\n\n") if b.startswith(path + " ")]
            if len(hit) != 1:
                fail(f"[{mode}] {name}", f"対象のパスで始まるブロックが {len(hit)} 個ある\n{err}")
            # **見出しと直し方まで見る。** 追跡そのものを咎める形でないと、走査に回って
            # 中身で鳴っただけの状態を「合格」と読んでしまう
            elif "が追跡されています" not in hit[0] or f"git rm --cached {path}" not in hit[0]:
                fail(f"[{mode}] {name}", f"追跡を咎めて外し方を出す形になっていない\n{hit[0]}")

    # **汚れた行がファイルの末尾とは限らない。** 上のケース表は全部「末尾に 1 行足す」形
    # なので、行番号を「総行数」にする変異が素通りしていた（実測）
    root = make_repo(tmp, "first-line")
    write(root, PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = {SENTINEL};\n" + CLEAN_PBXPROJ)
    git(root, "add", "-Af")
    check_case(root, "汚れた行がファイルの先頭", PBXPROJ, "DEVELOPMENT_TEAM", 1)

    # 離れた 2 行。1 件目で打ち切る／重複除去をやめる変異を、辞書順の偶然に頼らず捕まえる
    root = make_repo(tmp, "two-lines")
    write(root, PBXPROJ, f"\t\t\t\tDEVELOPMENT_TEAM = {SENTINEL};\n" + CLEAN_PBXPROJ + f"\t\t\t\tORGANIZATIONNAME = {SENTINEL};\n")
    git(root, "add", "-Af")
    code, out, err = run(root, "worktree", "離れた 2 行")
    expect_exit("離れた 2 行", code, 1)
    for wanted in ("1 行目: DEVELOPMENT_TEAM", "4 行目: ORGANIZATIONNAME"):
        if wanted not in err:
            fail("離れた 2 行", f"「{wanted}」が出ていない\n{err}")

    # 動機の中心にある実体を一通り。**xcuserdata の外に置く**——中に置くと
    # USER_STATE の枝が同じ文言で鳴るので、実体としての判定が消えても気づけない
    # （実測で `.p12` がこれだった）
    root = make_repo(tmp, "materials")
    paths = [f"secrets/dist{s}" if s.startswith(".") else f"secrets/{s}" for s in REQUIRED]
    for path in paths:
        write(root, path, "x\n")
    git(root, "add", "-f", "secrets")
    for mode in ("worktree", "staged"):
        note = f"[{mode}] 署名の実体を一通り"
        code, out, err = run(root, mode, note)
        expect_exit(note, code, 1, f"exit={code}\n{out}{err}")
        blocks = err.strip().split("\n\n")
        for path in paths:
            hit = [b for b in blocks if b.startswith(path + " ")]
            if len(hit) != 1 or "（署名の実体）" not in hit[0]:
                fail(note, f"{path} が署名の実体として 1 ブロックで出ていない\n{err}")

    # **追跡外のファイルは見ない。** これは全開発者の常態（手元の Local.xcconfig と
    # ビルド生成物）なので、ここで鳴る実装は毎コミット鳴って `--no-verify` を覚えさせる。
    # 他のケースは全部 `git add -Af` 済みなので、この形はここでしか再現しない
    root = make_repo(tmp, "untracked-around")
    write(root, LOCAL, f"TEAM_ID = {SENTINEL}\n")
    write(root, "build/x.p12", "x\n")
    check_case(root, "追跡外のファイルは見ない")

    # 見ないと決めた範囲。境界を広げたら、ここが落ちて気づける
    root = make_repo(tmp, "out-of-scope")
    write(root, "iosApp/ExportOptions.plist", f"<plist><dict><key>teamID</key><string>{SENTINEL}</string></dict></plist>\n")
    git(root, "add", "-Af")
    check_case(root, "plist は見ないと決めた範囲")

    # パスに空白と非 ASCII。`git ls-files -z` を外すと引用符付きで出てくる
    root = make_repo(tmp, "odd-path")
    write(root, "docs/仕様 の xcconfig/Odd.xcconfig", f"TEAM_ID = {SENTINEL}\n")
    git(root, "add", "-Af")
    check_case(root, "空白と非 ASCII を含むパス", "docs/仕様 の xcconfig/Odd.xcconfig", "TEAM_ID", 1)

    # xcuserdata の照合は**前後ともパス要素の境界で切る**。片側だけだと似た名前を誤検知する
    root = make_repo(tmp, "not-xcuserdata")
    write(root, "docs/myxcuserdata/x", "x\n")
    write(root, "iosApp/xcuserdata.bak/x", "x\n")
    git(root, "add", "-Af")
    check_case(root, "xcuserdata に似た名前は鳴らない")

    # **使い方は汚れたリポジトリで叩く。** 綺麗な土台だけだと、使い方を出した後に
    # 検査まで走る実装（`--help` が汚れたリポジトリで exit 1 になる）が素通りする
    for order, flag in enumerate(("-h", "--help")):
        root = make_repo(tmp, f"help{order}")
        dirty(root)
        code, out, err = run(root, "worktree", flag, extra=[flag])
        expect_exit(flag, code, 0)
        if "使い方" not in out or err:
            fail(flag, f"使い方だけが出ていない\n{out}{err}")

    root = make_repo(tmp, "extra-xcconfig")
    write(root, "Configuration/Shared.xcconfig", f"DEVELOPMENT_TEAM = {SENTINEL}\n")
    git(root, "add", "-Af")
    check_case(root, "リポジトリ直下の xcconfig も対象", "Configuration/Shared.xcconfig", "DEVELOPMENT_TEAM")

    root = make_repo(tmp, "upper-suffix")
    write(root, "D.XCCONFIG", f"DEVELOPMENT_TEAM = {SENTINEL}\n")
    git(root, "add", "-Af")
    check_case(root, "大文字の拡張子", "D.XCCONFIG", "DEVELOPMENT_TEAM")

    root = make_repo(tmp, "undecodable")
    with open(os.path.join(root, XCCONFIG), "wb") as f:
        f.write(b"TEAM_ID = \xff\xfe\n")
    git(root, "add", "-Af")
    for mode in ("worktree", "staged"):
        code, out, err = run(root, mode, f"[{mode}] UTF-8 として読めない")
        expect_exit(f"[{mode}] UTF-8 として読めない", code, 1, f"exit={code}\n{out}{err}")
        if "読めません" not in err:
            fail(f"[{mode}] UTF-8 として読めない", f"読めない旨が出ていない\n{err}")

    # **必須 2 本は別々に見る。** 片方だけのケースだと、もう片方を必須から外す変異が通る
    for order, target in enumerate((PBXPROJ, XCCONFIG)):
        root = make_repo(tmp, f"untracked-target{order}")
        git(root, "rm", "-q", "--cached", target)
        for mode in ("worktree", "staged"):
            note = f"[{mode}] {target} が追跡から外れた"
            code, out, err = run(root, mode, note)
            expect_exit(note, code, 1, f"exit={code}\n{out}{err}")
            if f"{target} が追跡されていません" not in err:
                fail(note, f"欠落の旨が出ていない\n{err}")

    # コンフリクト中。`git ls-files` は同じパスをステージごとに 3 回出すので、
    # 潰さないと同じ指摘が 3 つ並ぶ。フックからは到達しない（git が未マージのまま
    # コミットを拒む）が、verifier が「差分によらず毎回」回す経路では通る
    root = make_repo(tmp, "conflict")
    git(root, "checkout", "-q", "-b", "other")
    write(root, XCCONFIG, CLEAN_XCCONFIG + "A = 1\n")
    git(root, "add", "-Af")
    git(root, "commit", "-qm", "other")
    git(root, "checkout", "-q", "main")
    write(root, XCCONFIG, CLEAN_XCCONFIG + "B = 2\n")
    git(root, "add", "-Af")
    git(root, "commit", "-qm", "main")
    # merge は失敗するのが狙いなので、失敗で止まる git() は通さない
    subprocess.run(["git", "merge", "other"], cwd=root, capture_output=True)
    code, out, err = run(root, "staged", "コンフリクト中")
    expect_exit("コンフリクト中", code, 1, f"exit={code}\n{out}{err}")
    hit = [b for b in err.strip().split("\n\n") if b.startswith(XCCONFIG + " ")]
    if len(hit) != 1:
        fail("コンフリクト中", f"同じパスの報告が {len(hit)} 個ある\n{err}")
    # **git の言い分を潰さない。** 何でも「index に無い」にすると、コンフリクトが
    # 「削除された」と読めて直し方を誤らせる
    elif "stage 0" not in hit[0]:
        fail("コンフリクト中", f"git の失敗理由が出ていない\n{hit[0]}")

    # 作業ツリーから消えても index には在る。見る面が違うので結果も違うのが正しい
    root = make_repo(tmp, "worktree-missing")
    os.remove(os.path.join(root, PBXPROJ))
    expect_exit("[worktree] 作業ツリーから消えた", run(root, "worktree", "[worktree] 作業ツリーから消えた")[0], 1)
    expect_exit("[staged] 作業ツリーから消えた（index には在る）", run(root, "staged", "[staged] 作業ツリーから消えた")[0], 0)

    # どこから起動しても同じ結果になる（ルートへ移動している）
    root = make_repo(tmp, "subdir")
    dirty(root)
    code, out, err = run(root, "worktree", "サブディレクトリ", cwd=os.path.join(root, "iosApp"))
    expect_exit("サブディレクトリからの起動", code, 1, f"exit={code}\n{out}{err}")
    # **「欠落」が出ていないことまで見る。** ルートへ移動していないと必須 2 本が
    # 見つからず、その報告に対象パスが含まれるので、パスの有無だけでは通ってしまう
    if "欠落" in err or not any(b.startswith(PBXPROJ + " ") for b in err.split("\n\n")):
        fail("サブディレクトリからの起動", f"ルート相対で検出できていない\n{err}")

    root = make_repo(tmp, "bad-arg")
    code, out, err = run(root, "worktree", "知らない引数", extra=["--stagd"])
    expect_exit("知らない引数", code, 1)
    if "知らない引数" not in out + err:
        fail("知らない引数", f"意図した経路で落ちていない\n{out}{err}")

    outside = os.path.join(tmp, "not-a-repo")
    os.makedirs(outside)
    code, out, err = run(outside, "worktree", "リポジトリの外")
    expect_exit("リポジトリの外", code, 1)
    if "git " not in out + err:
        fail("リポジトリの外", f"git の失敗として落ちていない\n{out}{err}")

# --- 3 点セットの不変条件（CLAUDE.md「このリポジトリは public」節）---
# 拡張子は MATERIAL_SUFFIXES・.gitignore・deny の 3 か所に並ぶ。集合の包含を直接見る。
# **ソースを正規表現で読まない**——整形しただけで 0 件になり、無言で通ってしまう
# （実測。import できるのは検査側に import 時の副作用が無いからで、これ自体が
# その性質のテストにもなっている）

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("check_ios_signing", CHECK)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

material = set(module.MATERIAL_SUFFIXES) | set(module.MATERIAL_NAMES)

# **中身も固定する。** 下の突き合わせは 3 か所の一致しか見ないので、揃えて消す／
# 揃えて足す編集が通ってしまう（実測。上の REQUIRED / NOT_MATERIAL を参照）
missing = sorted(set(REQUIRED) - material)
if missing:
    fail("3 点セット", f"実体の一覧から外せないものが抜けている: {missing}")
extra = sorted(set(NOT_MATERIAL) & material)
if extra:
    fail("3 点セット", f"実体でない拡張子が一覧に入っている: {extra}")

# **小文字で持つ**という不変条件。大文字が混じるとどのパスにも一致しなくなる（実際に踏んだ）
upper = [s for s in material if s != s.lower()]
if upper:
    fail("3 点セット", f"小文字で持っていない: {sorted(upper)}")

# **`.gitignore` の署名ブロックとは集合として突き合わせる。**
# 全文への部分文字列検索だと、短い拡張子が長い拡張子に吸収されて（`.cer` ⊂
# `.certsigningrequest`）、消しても緑のまま（実測）。件数の下限で代用するのも同じで、
# 数え間違いが 1 件の脱落を素通りさせる。等値なら「消す」も「置き換える」も鳴る
MARKER = "# 署名鍵とその設定"
lines = read(os.path.join(ROOT, ".gitignore")).splitlines()
heads = [i for i, line in enumerate(lines) if line.startswith(MARKER)]
if len(heads) != 1:
    fail("3 点セット", f".gitignore に「{MARKER}」の節が {len(heads)} 個ある")
else:
    # 節は**次の見出しまで**。空行で切ると、節の中を空行で区切っただけで
    # 「一覧のほうが多い」と誤って報告する（犯人が読めない失敗になる）
    block = set()
    for line in lines[heads[0] + 1:]:
        entry = line.strip()
        if entry.startswith("#"):
            break
        if not entry:
            continue
        # 節に置くのは「`*` 付きの拡張子」か「ファイル名」だけ。除外やパス付きの
        # 狭いパターンが混ざったら、3 点セットの脱落とは別の失敗として報告する
        if entry.startswith("!") or "/" in entry:
            fail("3 点セット", f".gitignore の署名の節に想定外の行がある: {entry}")
        else:
            # 大小は落として比べる（`*.certSigningRequest` のように、Xcode が書く
            # 綴りをそのまま置いている行がある）
            block.add(entry.lstrip("*").lower())
    if block != material:
        fail("3 点セット", f".gitignore と食い違う（一覧のみ: {sorted(material - block)} ／ .gitignore のみ: {sorted(block - material)}）")

# deny は 1 拡張子につき Bash と Read の 2 行。**規則の全体と突き合わせる**——
# 全文検索だと、上と同じ理由で短い拡張子の削除が見えない
denied = {rule.lower() for rule in json.loads(read(os.path.join(ROOT, ".claude", "settings.json")))["permissions"]["deny"]}
for item in sorted(material):
    if f"bash(cat *{item})" not in denied:
        fail("3 点セット", f"deny に `Bash(cat *{item})` が無い")
    reads = (f"read(**/*{item})", f"read(**/{item})")
    if not set(reads) & denied:
        fail("3 点セット", f"deny に {' か '.join(f'`Read({r[5:-1]})`' for r in reads)} が無い")

# **名前で守っている 2 つも見る。** Local.xcconfig と xcuserdata/ は拡張子の節とは
# 別の場所に並ぶので、上の突き合わせの外にあった（deny 側を消しても誰も気づかない）
entries = [line.strip() for line in lines]
LOCAL_NAME = module.LOCAL.rsplit("/", 1)[-1]
for entry in (LOCAL_NAME, module.USER_STATE.lstrip("/")):
    if entry not in entries:
        fail("3 点セット", f".gitignore に {entry} の行が無い")
for rule in (f"bash(cat *{LOCAL_NAME.lower()})", f"read(**/{LOCAL_NAME.lower()})"):
    if rule not in denied:
        fail("3 点セット", f"deny に `{rule}` に当たる規則が無い")

if failures:
    print(f"check-ios-signing の検査で {len(failures)} 件失敗しました:", file=sys.stderr)
    for failure in failures:
        print(f"- {failure}", file=sys.stderr)
    sys.exit(1)

print(f"check-ios-signing は健全です（{len(CASES)} ケース ＋ 構造検査）")
