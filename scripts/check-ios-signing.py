#!/usr/bin/env python3
"""署名の設定が、追跡しているファイルに入っていないかを見る。

**動機は 1 つ。Xcode がプロジェクトを開いた拍子に `project.pbxproj` へ焼き込む
署名設定を、コミットさせないこと。** 焼き込まれると xcconfig 側の値が黙って無効に
なり（pbxproj のほうが強い）、public リポジトリではそのまま公開される。
**ここを越える防御は張らない**——網が広いほど誤検知で `--no-verify` を覚える。

作業ツリーを見るのが CI、index を見るのが pre-commit（`--staged`）。
フックと CI の役割分担・すり抜ける経路は docs/仕様/16_git運用定義書.md §1.4 が正。

エラー（終了コード 1。CI と pre-commit が落ちる）:
  - pbxproj / xcconfig に署名の値が直接書かれている
  - pbxproj に CODE_SIGN_STYLE 系が書かれている（値は秘密でないが、xcconfig 側が黙って負ける）
  - 署名の実体（プロファイル・証明書・鍵）が追跡されている
  - Local.xcconfig や xcuserdata/ が追跡されている（`.gitignore` は `git add -f` を止められない）
  - 検査対象そのものが追跡されていない／読めない（素通りと区別できないので落とす）

**出すのはパス・行番号・列挙した設定名だけ。** 行の中身は持ち帰らない。
パスを原文で出すのは、追跡されている時点でリポジトリ自身が公開しているため。

**コメントの中も見る。** 説明のつもりで実値を書いても公開されるのは同じなので、
誤検知の側に倒す。xcconfig を編集するときの規律はこの 1 つ:

  - **コメントにも実値を書かない。** そのうえで、説明で設定名を出すときは直後に
    `=` を書かない——実値を書いた例示と区別できないので、行頭のコメント
    （`// KEY = 説明`）でも行末コメント（`FOO = bar // KEY = 説明`）でも鳴る

  同じ規律を `iosApp/Configuration/Config.xcconfig` の冒頭にも置いている
  （編集する人が最初に読むのはそちら）。**変えるなら両方**。

  値は `//` の手前で終わる。**コメントを捨てるのではなく、そこから先を続けて読む**——
  捨てると `KEY = 値 // 例: KEY = 実値` の後半を見逃す（左辺が監視対象だと、
  1 マッチが行末まで食って 2 つ目の代入が隠れる）。裏返しとして `KEY = // 実値` は
  値が空に見えるので通る。

見ないと決めた範囲（**Xcode が開いた拍子には書かないもの**）:
  - plist / entitlements（capability を足したときや archive したときの産物）。
    ExportOptions.plist・`.xcarchive/Info.plist` もここ
  - `CODE_SIGN_KEYCHAIN` と `OTHER_CODE_SIGN_FLAGS`。人が手で書くもので、Xcode は
    開いた拍子には書かない（前者の値は `/Users/<アカウント名>/…` になるので、
    ⑤ で CI 署名を書き始めるときに見直す。T-57）
  - fastlane の設定と、そこに書く App Store Connect の API キー ID
  - ワークフロー（`.github/workflows/*.yml`）に直接書いた値
  - 値が次の行に折れている形。値は行内で完結する前提で読む
  - 接頭辞を付けた自作キー（`MY_TEAM_ID = 実値`）。意図的な迂回は動機の外で、
    塞ぐと誤検知を招く（理由は `setting()`）
  - `KEY = // 実値` のように値の位置がまるごとコメントの形（上記の裏返し）
  - XML plist 形式に変換した pbxproj
  - 上に挙げた拡張子・名前に当たらないファイル（`Team.txt` に書いて xcconfig から読む等）

⑤ で配布を自動化するときに、fastlane とワークフローを走査対象へ入れるかを判断する
（[T-57](../docs/案件/23_技術改善バックログ.md#t-57)）。

**網を変えたら scripts/test/check-ios-signing_test.py にケースを足す。**
正常系（＝いまのリポジトリ）は正規表現から 1 語落ちても緑のままなので、
変えたことは何も示さない。
"""

import os
import re
import subprocess
import sys

USAGE = """使い方:
  python3 scripts/check-ios-signing.py            # 作業ツリーを見る（CI）
  python3 scripts/check-ios-signing.py --staged   # index を見る（pre-commit）
  python3 scripts/check-ios-signing.py --help     # これ"""

# この 2 本は必ず在る。追跡から外れたら「検査対象そのものの欠落」として落とす
PBXPROJ = "iosApp/iosApp.xcodeproj/project.pbxproj"
XCCONFIG = "iosApp/Configuration/Config.xcconfig"
LOCAL = "iosApp/Configuration/Local.xcconfig"


def wants_staged(argv):
    """index を見るかどうかを返す。**検査せずに終わる経路はここで終了する**——
    呼び出し側へ終了コードを持ち帰ると、経路を足した人が黙って落ちる。
    """
    if argv in (["-h"], ["--help"]):
        print(USAGE)
        # `sys.exit(文字列)` は stderr へ出して 1 で終わる。使い方は成功なので分ける
        raise SystemExit(0)
    if argv == ["--staged"]:
        return True
    if not argv:
        return False
    sys.exit(f"知らない引数です: {' '.join(argv)}\n{USAGE}")


def git(*argv):
    """git の失敗は握り潰さない。握ると「異常だから鳴らなかった」が合格に見える。"""
    try:
        done = subprocess.run(["git", *argv], capture_output=True)
    except FileNotFoundError:
        sys.exit("git が見つかりません")
    if done.returncode != 0:
        sys.exit(f"git {' '.join(argv)} が失敗しました: {done.stderr.decode(errors='replace').strip()}")
    return done.stdout.decode("utf-8")


# ---- 検査対象 ---------------------------------------------------------------

SETTING_SUFFIXES = (".pbxproj", ".xcconfig")
# 署名の実体。中身を読むまでもなく、追跡されていること自体が誤り。
# **すべて小文字で持つ。** 照合はパスを小文字に落として比べるので、大文字が
# 混じった要素はどんなパスにも一致しない
MATERIAL_SUFFIXES = (
    ".mobileprovision",
    ".provisionprofile",
    ".p12",
    ".pfx",
    ".pkcs12",
    ".p8",  # App Store Connect の API 鍵
    ".cer",
    ".der",
    ".crt",
    ".p7b",
    ".certsigningrequest",
    ".pem",
    ".key",
    ".keychain",
    ".keychain-db",
    ".jks",
    ".keystore",
    ".ipa",  # 署名済みの配布物。中に embedded.mobileprovision を含む
)
# 拡張子を持たない実体。接尾辞照合だけだと取りこぼす
MATERIAL_NAMES = ("keystore.properties",)

# Xcode が**開いただけで**書き、ディレクトリ名に macOS のアカウント名が入る。
# `.gitignore` にはあるが、`git add -f` を止められないのは Local.xcconfig と同じ
USER_STATE = "/xcuserdata/"


def has_suffix(path, suffixes):
    """**照合は小文字に落としてから。** 中身の照合は IGNORECASE なのにここだけ
    区別すると、`D.XCCONFIG` が素通りする（macOS の既定では書けてしまう）。
    """
    return path.lower().endswith(suffixes)


def is_material(path):
    return has_suffix(path, MATERIAL_SUFFIXES) or path.rsplit("/", 1)[-1].lower() in MATERIAL_NAMES


def is_local(path):
    """`.gitignore` の `Local.xcconfig` はディレクトリを問わず効く。**走査除外と
    追跡禁止で同じ広さにする**——ずれると、同じファイルが 2 つの見出しで報告され、
    しかも片方の直し方が「Local.xcconfig へ移せ」になる。
    """
    return path.rsplit("/", 1)[-1].lower() == LOCAL.rsplit("/", 1)[-1].lower()


def targets():
    """**対象はリポジトリ全体の列挙で決める。** 場所を定数で持つと、置き場所が増えた日に黙って網が縮む。"""
    # **重複を潰す。** コンフリクト中のパスはステージごとに 3 回出るので、
    # 潰さないと同じファイルの指摘が 3 つ並ぶ（フックからは到達しないが、手で回すと出る）
    tracked = list(dict.fromkeys(p for p in git("ls-files", "-z").split("\0") if p))
    return (
        tracked,
        sorted(p for p in tracked if has_suffix(p, SETTING_SUFFIXES) and not is_local(p)),
        sorted(p for p in tracked if is_material(p)),
    )


def read_target(path, staged):
    """`(本文, 読めなかった理由)` を返す。検査するのは「これから履歴に入る内容」。

    **読めなくてもここで即終了しない**——1 件の欠落で他の指摘が隠れる。
    """
    if staged:
        # **`:0:` まで書く。** `:パス` はリビジョン構文なので、`0:x.xcconfig` のような
        # 名前だと「ステージ 0 の x.xcconfig」と解釈されて別のファイルを読む（実測）
        done = subprocess.run(["git", "show", f":0:{path}"], capture_output=True)
        if done.returncode != 0:
            # **理由を潰さない。** 何でも「index に無い」にすると、コンフリクト中や
            # オブジェクトの破損が「削除された」と読めて、直し方を誤らせる
            reason = done.stderr.decode(errors="replace").strip()
            return None, f"index から読めない（素通りと区別できないので落とす）: {reason}"
        raw = done.stdout
    else:
        try:
            with open(path, "rb") as f:
                raw = f.read()
        except OSError:
            return None, "作業ツリーに無い（検査対象そのものの欠落）"
    # 2 つの経路で同じ挙動にするため UTF-8 に固定する（ロケールに委ねない）
    try:
        return raw.decode("utf-8"), None
    except UnicodeDecodeError:
        return None, "UTF-8 として読めない（バイナリや UTF-16 は見ない）"


# ---- 何を見るか -------------------------------------------------------------

# 値が秘密か、手元ごとに変わるもの。どちらのファイルに書かれても困る。
# CODE_SIGN_IDENTITY をここに入れているのは、手動署名だと
# `Apple Development: 氏名 (チーム ID)` の完全名が入るため（一般名は下で通す）
SECRET_KEYS = (
    r"TEAM_ID"
    r"|DEVELOPMENT_?TEAM(?:_?NAME)?"
    r"|PROVISIONING_?PROFILE(?:_?SPECIFIER)?"
    r"|CODE_SIGN_IDENTITY"
    r"|ORGANIZATION_?NAME"
)

# 値そのものは秘密ではないが、pbxproj に書かれると xcconfig 側の指定が黙って負ける。
# xcconfig は正当な置き場所なので、そちらでは見ない
STYLE_KEYS = r"CODE_SIGN_STYLE|PROVISIONING_?STYLE"


def setting(keys, terminator):
    """キー・条件・値を同じ部品から組み立てる。ファイルごとに書き分けるとドリフトする。

    `_?` は TargetAttributes の `DevelopmentTeam`（アンダースコア無し）を拾うため。
    行頭に錨を打たないので、1 行に複数並ぶ形も行の途中から始まる形もコメントの中も拾える。
    **キーの前だけは識別子の途中でないことを要求する。** これが無いと
    `-DUSE_TEAM_ID=1` のような値の中の define が鳴り、しかも報告に出る設定名が
    **その行に設定としては存在しない**（`TEAM_ID` という代入は無い）形になるので、
    直しようがない。代償として、接頭辞を付けた自作キーは見ないと決めた範囲に入る。
    """
    return re.compile(
        r'"?(?<![A-Za-z0-9_])(?P<key>' + keys + r")"
        # **条件付きキーは繰り返す。** Xcode は [sdk=…][arch=…] と 2 つ以上並べる
        r"(?:\[[^\]]*\])*"
        r'"?'  # Xcode はキーを条件ごと引用符で囲む（"DEVELOPMENT_TEAM[sdk=iphoneos*]"）
        # **`\s` は使わない。** 改行をまたぐので、空値のキーが次の行を値として拾う
        r"[ \t]*=[ \t]*"
        r"(?P<value>" + terminator + r")",
        re.IGNORECASE,
    )


# pbxproj は `;` で、xcconfig は `//` か行末で値が終わる。
# 共用すると xcconfig で `= ;値` を取りこぼし、pbxproj で `= "//値"` を見逃す
PBX_SETTING = setting(SECRET_KEYS + "|" + STYLE_KEYS, r"[^;\n]*")
CFG_SETTING = setting(SECRET_KEYS, r"(?:(?!//)[^\n])*")

# `$(FOO)` / `${FOO}` / `$FOO` の参照。**先頭 1 文字で判定してはいけない**——
# `$(inherited) 実値` は Xcode が実際に書く形で、参照の後ろに値が続く
REFERENCE = re.compile(r"\$(?:\([^()$]*\)|\{[^{}$]*\}|[A-Za-z_][A-Za-z0-9_]*)")

# 秘密でも手元固有でもない値。弾くと、構成差として書くべきものまで
# 手元ファイルへの複製に追い込むことになる
GENERIC_VALUES = (
    "-",
    "apple development",
    "apple distribution",
    "iphone developer",
    "iphone distribution",
    "mac developer",
    "developer id application",
)


def fold(value):
    """参照を畳んで、直接書かれた部分だけを残す。"""
    while True:
        # `$(A_$(B))` のような入れ子は 1 回では畳めない。1 周ごとに必ず短くなるので止まる
        folded = REFERENCE.sub("", value)
        if folded == value:
            return folded.replace('"', "").strip()
        value = folded


def is_literal(value):
    """値が直接書かれているか。参照だけの値・空値・一般名は通す。"""
    rest = fold(value).lower()
    return bool(rest) and rest not in GENERIC_VALUES


def scan(path, text):
    """(行番号, 設定名) を返す。

    **出力に載るのは `key` グループだけ**で、`key` は列挙した設定名からしか一致しない。
    ワイルドカードを含むキーを足すと、この不変条件が静かに破れる。
    """
    pattern = PBX_SETTING if has_suffix(path, (".pbxproj",)) else CFG_SETTING
    found = []
    for match in pattern.finditer(text):
        key = match.group("key")
        if is_literal(match.group("value")):
            found.append((text.count("\n", 0, match.start()) + 1, key))
    return sorted(set(found))


# ---- 報告 -------------------------------------------------------------------

STYLE_ONLY = re.compile(r"\A(?:" + STYLE_KEYS + r")\Z", re.IGNORECASE)


def report(path, found):
    """**直し方に破壊的なコマンドを出さない。** pbxproj には他の編集も載るので、
    丸ごと戻すと追加したファイル参照まで消える。

    **見出しも分ける。** CODE_SIGN_STYLE 系は値が秘密ではないので、「署名の値が
    書かれた」と読める文言を出すと、push 済みかの確認という不要な対応を誘発する。
    """
    if all(STYLE_ONLY.match(key) for _, key in found):
        # PROVISIONING_STYLE というビルド設定は無い。xcconfig 側の等価物は CODE_SIGN_STYLE
        head = f"{path} に署名方式が直接書かれています（{XCCONFIG} 側の指定が負けます）:"
        tail = f"  署名方式は {XCCONFIG} の CODE_SIGN_STYLE で指定する"
    else:
        head = f"{path} に署名の値が直接書かれています:"
        # **第一指示は実値の削除**。コメント行にも鳴るので `=` の話も要るが、そちらを
        # 先に書くと「= を外して実値は残す」と読めてしまい、検査自身が漏洩を招く
        tail = f"  実値を消す（説明を残すなら、設定名の直後の = も外す）。手元だけの値は {LOCAL} へ"
    return "\n".join([head, *[f"  {line} 行目: {key}" for line, key in found], tail])


def check(staged):
    """指摘の一覧を返す。**見つけたものは全部出す**——1 件目で終了すると、
    いちばん重い指摘が無関係な条件の陰に隠れる。
    """
    blocks = []
    tracked, scanned, materials = targets()

    for required in (PBXPROJ, XCCONFIG):
        if required not in scanned:
            blocks.append(f"{required} が追跡されていません（検査対象そのものの欠落）")

    for path in scanned:
        text, problem = read_target(path, staged)
        if problem:
            blocks.append(f"{path} を読めません: {problem}")
            continue
        found = scan(path, text)
        if found:
            blocks.append(report(path, found))

    for path in materials:
        blocks.append(f"{path} が追跡されています（署名の実体）:\n  git rm --cached {path}")

    # `.gitignore` は `git add -f` を止められない。追跡されていないことを直接見る。
    # **実体として既に出したものは繰り返さない**（`xcuserdata/x.p12` は両方に当たる）
    for path in tracked:
        if path in materials:
            continue
        if is_local(path) or USER_STATE in f"/{path}":
            blocks.append(f"{path} が追跡されています:\n  git rm --cached {path}")

    return blocks


def main(argv):
    staged = wants_staged(argv)
    # パスはリポジトリルートからの相対で扱う。どこから起動されても同じ結果になるように
    os.chdir(git("rev-parse", "--show-toplevel").strip())
    problems = check(staged)
    if problems:
        print("\n\n".join(problems), file=sys.stderr)
        return 1
    # pre-commit では黙る。毎コミット鳴ると、関係のないコミットでも iOS の話が出る
    if not staged:
        print("署名の値は追跡ファイルに入っていません")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
