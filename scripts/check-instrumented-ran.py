#!/usr/bin/env python3
"""instrumented のテストを持つモジュールが、それぞれ 1 件以上走らせたかを見る。

`pixel6Api35Check` の直後に呼ぶ。前の実行の XML が残っていても区別しないので、守りになるのは
ビルド出力が毎回空から始まる CI だけ。設計は docs/仕様/17_テスト戦略定義書.md §4。
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEST_SOURCE_PATTERNS = ("androidApp/src/androidTest", "shared/*/src/androidDeviceTest")
# `connected/` は実機での結果なので数えない
RESULTS_DIR = "build/outputs/androidTest-results/managedDevice"


def modules_with_tests() -> list[Path]:
    """テストのソースを持つモジュールを、Gradle の設定を見ずにディレクトリから引く。"""
    return sorted(
        source_dir.parent.parent
        for pattern in TEST_SOURCE_PATTERNS
        for source_dir in ROOT.glob(pattern)
        if any(source_dir.rglob("*.kt"))
    )


def executed_test_count(module: Path) -> int:
    """実行した件数。`@Ignore` は数えない。

    外側の `<testsuites>` にも同じ件数があるので、`<testsuite>` だけを足す。
    """
    return sum(
        int(suite.get("tests", "0")) - int(suite.get("skipped", "0"))
        for result in (module / RESULTS_DIR).rglob("*.xml")
        for suite in ET.parse(result).getroot().iter("testsuite")
    )


def main() -> int:
    modules = modules_with_tests()
    if not modules:
        print(
            "instrumented のテストを持つモジュールが見つからない。置き場所が変わっていないか確かめること",
            file=sys.stderr,
        )
        return 1

    counts = {module.relative_to(ROOT): executed_test_count(module) for module in modules}
    for module, count in counts.items():
        print(f"{module}: {count} 件")

    not_run = [str(module) for module, count in counts.items() if count == 0]
    if not_run:
        print(
            f"instrumented のテストが 1 件も走っていない: {', '.join(not_run)}"
            f"（見た場所は各モジュールの {RESULTS_DIR}）",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
