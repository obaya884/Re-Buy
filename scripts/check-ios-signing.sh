#!/bin/sh
# iosApp の署名設定が、追跡しているファイルに直接書かれていないかを見る。
#
# 書かれると xcconfig 側の値が黙って無効になる（pbxproj のほうが強い）。
# 設定の出所を 1 つに保つための網で、CI と pre-commit の両方で回る。
#
# 使い方:
#   sh scripts/check-ios-signing.sh            # 作業ツリーを見る（CI）
#   sh scripts/check-ios-signing.sh --staged   # index を見る（pre-commit）
#
# **pre-commit では index を見る。** 手元で書き換わっただけの状態はコミットに
# 入らないので、そこで止めると `--no-verify` を覚えるだけになる。
#
# エラー（終了コード 1。CI と pre-commit が落ちる）:
#   - iosApp 配下の追跡している pbxproj / xcconfig に署名設定の値が直接書かれている
#   - pbxproj に CODE_SIGN_STYLE 系が書かれている（値は秘密でないが、xcconfig 側が黙って負ける）
#   - Local.xcconfig が追跡されている（`.gitignore` は `git add -f` を止められない）
#   - 検査対象そのものが追跡されていない（検査が素通りしたのと区別できないので落とす）
#
# **鳴らすときも値は 1 文字も出さない。** 行から値を消すのではなく、行番号と
# 設定名だけを組み立てて出す——消す方式だと想定外の書き方が来るたびに消し残る。
#
# **網を足したり緩めたりしたら、その場で壊して確かめる。** 正常系（＝いまの
# リポジトリ）は正規表現から 1 語落ちても緑のままなので、変えたことは何も示さない。
set -eu

# 素の `cd "$(git rev-parse --show-toplevel)"` はリポジトリ外で空文字列の cd になり、
# 失敗扱いにならないままカレントディレクトリで走り出す（読めない理由で落ちて原因が分かりにくい）
repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
  echo "Re-Buy リポジトリの中で実行してください" >&2
  exit 1
}
cd "$repo_root"

exec python3 - "$@" <<'PY'
import re
import subprocess
import sys

# この 2 本は必ず在る。追跡から外れたら「検査対象そのものの欠落」として落とす
PBXPROJ = "iosApp/iosApp.xcodeproj/project.pbxproj"
XCCONFIG = "iosApp/Configuration/Config.xcconfig"
LOCAL = "iosApp/Configuration/Local.xcconfig"

args = sys.argv[1:]
if args == ["--staged"]:
    staged = True
elif not args:
    staged = False
else:
    sys.exit(f"認識できない引数: {args[0]}（--staged だけを受け付けます）")


def git(*argv):
    """git の失敗は握り潰さない。握ると「異常だから鳴らなかった」が合格に見える。"""
    done = subprocess.run(["git", *argv], capture_output=True)
    if done.returncode != 0:
        sys.exit(f"git {' '.join(argv)} が失敗しました")
    return done.stdout.decode("utf-8")


def read_target(path):
    """検査するのは「これから履歴に入る内容」。--staged なら index、そうでなければ作業ツリー。"""
    if staged:
        done = subprocess.run(["git", "show", f":{path}"], capture_output=True)
        if done.returncode != 0:
            sys.exit(f"{path} を index から読めません（検査対象そのものの欠落）")
        # 作業ツリー側と揃えて UTF-8 固定にする。`text=True` はロケール依存になり、
        # 同じ内容に対して 2 つの起動経路が違う挙動をする
        return done.stdout.decode("utf-8")
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        sys.exit(f"{path} が見つかりません（検査対象そのものの欠落）")


def targets():
    """**対象は列挙で決める。** 定数 2 本だと、構成別の xcconfig を足した日に黙って網が縮む。"""
    tracked = [p for p in git("ls-files", "-z", "--", "iosApp").split("\0") if p]
    found = [p for p in tracked if p.endswith((".pbxproj", ".xcconfig")) and p != LOCAL]
    for required in (PBXPROJ, XCCONFIG):
        if required not in found:
            sys.exit(f"{required} が追跡されていません（検査対象そのものの欠落）")
    return sorted(found)


# 値が秘密か、手元ごとに変わるもの。どのファイルに書かれても困る
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


def setting(keys):
    """キー・条件・値を同じ部品から組み立てる。2 本を書き分けるとドリフトする。

    `_?` は TargetAttributes の `DevelopmentTeam`（アンダースコア無し）を拾うため。
    行頭に錨を打たないので、1 行に複数並ぶ形も行の途中から始まる形も拾える。
    値は行内で完結する前提（pbxproj は `;`、xcconfig は行末で終わる）。
    """
    return re.compile(
        r'(?<![A-Za-z0-9_])"?(?P<key>' + keys + r")"
        r"(?:\[[^\]]*\])?"  # 条件付きキー（[sdk=iphoneos*] など）
        r'"?'  # Xcode はキーを条件ごと引用符で囲む（"DEVELOPMENT_TEAM[sdk=iphoneos*]"）
        r"\s*=\s*"
        r"(?P<value>[^;\n]*)",
        re.IGNORECASE,
    )


PBX_SETTING = setting(SECRET_KEYS + "|" + STYLE_KEYS)
CFG_SETTING = setting(SECRET_KEYS)

# `$(FOO)` / `${FOO}` の参照。**先頭 1 文字で判定してはいけない**——
# `$(inherited) 実値` は Xcode が実際に書く形で、参照の後ろに値が続く
REFERENCE = re.compile(r"\$[({][^)}]*[)}]")


def is_literal(value):
    """値が直接書かれているか。参照だけの値・空値・アドホック署名の `-` は通す。"""
    rest = REFERENCE.sub("", value).replace('"', "").strip()
    return bool(rest) and rest != "-"


def scan(path):
    is_xcconfig = path.endswith(".xcconfig")
    pattern = CFG_SETTING if is_xcconfig else PBX_SETTING
    hits = []
    for lineno, line in enumerate(read_target(path).splitlines(), 1):
        # xcconfig は `//` 以降がコメント。落とさないと、設定名を含む説明文で鳴る
        if is_xcconfig:
            line = line.split("//")[0]
        for found in pattern.finditer(line):
            if is_literal(found.group("value")):
                hits.append(f"  {lineno}: {found.group('key')} = <伏せ>")
    return hits


def restore(path):
    # --staged のときは作業ツリーだけ戻しても index は戻らない
    return f"git restore --staged --worktree {path}" if staged else f"git restore {path}"


blocks = []

for path in targets():
    hits = scan(path)
    if hits:
        hint = f"  手元だけの値は {LOCAL} へ" if path.endswith(".xcconfig") else f"  {restore(path)}"
        blocks.append("\n".join([f"{path} に署名設定が直接書かれています:", *hits, hint]))

# `.gitignore` は `git add -f` を止められない。追跡されていないことを直接見る
if git("ls-files", "--", LOCAL).strip():
    blocks.append("\n".join([f"{LOCAL} が追跡されています:", f"  git rm --cached {LOCAL}"]))

if blocks:
    print("\n\n".join(blocks), file=sys.stderr)
    sys.exit(1)

# pre-commit では黙る。毎コミット鳴ると、関係のないコミットでも iOS の話が出る
if not staged:
    print("iosApp の署名設定は追跡ファイルに入っていません")
PY
