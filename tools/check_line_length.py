#!/usr/bin/env python3
"""Report code lines longer than the project limit.

The project limit applies to code only: the character count starts at the first
non-whitespace character, so leading indentation never eats into the budget.
Documentation lines (KDoc, block comments, `//` comments) are not code and are
ignored, as are Markdown files and the `guidelines/` directory.

Usage:
    tools/check_line_length.py                      # scan the repository
    tools/check_line_length.py jobs models          # scan given paths
    tools/check_line_length.py --max-length 100 src
    tools/check_line_length.py --no-fail            # report, always exit 0
    tools/check_line_length.py --count-only         # one number per file
"""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass
from pathlib import Path

DEFAULT_MAX_LENGTH = 110
DEFAULT_SUFFIXES = (".kt", ".kts")
SKIPPED_DIRECTORIES = {
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    ".qodana",
    "build",
    "out",
    "node_modules",
    "generated",
}
SKIPPED_DIRECTORY_NAMES = {"guidelines"}


@dataclass(frozen=True)
class Violation:
    path: Path
    line_number: int
    length: int
    content: str


def is_scannable(path: Path, suffixes: tuple[str, ...]) -> bool:
    return path.suffix in suffixes


def walk(root: Path, suffixes: tuple[str, ...]) -> list[Path]:
    if root.is_file():
        return [root] if is_scannable(root, suffixes) else []

    files: list[Path] = []
    for current, directories, names in os.walk(root):
        directories[:] = [
            directory
            for directory in directories
            if directory not in SKIPPED_DIRECTORIES
            and directory not in SKIPPED_DIRECTORY_NAMES
        ]
        files.extend(
            Path(current, name)
            for name in sorted(names)
            if is_scannable(Path(name), suffixes)
        )
    return files


def code_lines(path: Path):
    """Yield (line_number, code_content) for every line carrying code.

    A line is code when it holds something else than indentation and comment
    markers once block comments are accounted for.
    """
    in_block_comment = False
    for line_number, raw_line in enumerate(
        path.read_text(encoding="utf-8", errors="replace").splitlines(), start=1
    ):
        content = raw_line.strip()
        if not content:
            continue

        if in_block_comment:
            if "*/" in content:
                in_block_comment = False
                content = content.split("*/", 1)[1].strip()
                if not content:
                    continue
            else:
                continue

        if content.startswith("//"):
            continue
        if content.startswith("/*"):
            if "*/" not in content:
                in_block_comment = True
            continue

        yield line_number, content


def collect(paths: list[Path], max_length: int, suffixes: tuple[str, ...]) -> list[Violation]:
    violations: list[Violation] = []
    for root in paths:
        for path in walk(root, suffixes):
            for line_number, content in code_lines(path):
                if len(content) > max_length:
                    violations.append(Violation(path, line_number, len(content), content))
    return violations


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Report code lines longer than the project limit "
        "(leading indentation excluded)."
    )
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=[Path(".")],
        help="files or directories to scan (default: current directory)",
    )
    parser.add_argument(
        "--max-length",
        type=int,
        default=DEFAULT_MAX_LENGTH,
        help=f"maximum characters of code per line (default: {DEFAULT_MAX_LENGTH})",
    )
    parser.add_argument(
        "--suffixes",
        default=",".join(DEFAULT_SUFFIXES),
        help=f"comma separated file suffixes to scan (default: {','.join(DEFAULT_SUFFIXES)})",
    )
    parser.add_argument(
        "--count-only",
        action="store_true",
        help="print one `count path` line per file instead of every violation",
    )
    parser.add_argument(
        "--no-fail",
        action="store_true",
        help="always exit with 0, even when violations are found",
    )
    arguments = parser.parse_args(argv)

    suffixes = tuple(suffix.strip() for suffix in arguments.suffixes.split(",") if suffix.strip())
    violations = collect(arguments.paths, arguments.max_length, suffixes)

    if arguments.count_only:
        counts: dict[Path, int] = {}
        for violation in violations:
            counts[violation.path] = counts.get(violation.path, 0) + 1
        for path, count in sorted(counts.items(), key=lambda item: (-item[1], str(item[0]))):
            print(f"{count:4d}  {path}")
    else:
        for violation in violations:
            print(f"{violation.path}:{violation.line_number}: {violation.length} chars")
            print(f"    {violation.content}")

    total = len(violations)
    print(
        f"\n{total} code line(s) over {arguments.max_length} characters "
        f"in {len({violation.path for violation in violations})} file(s)."
    )

    if total and not arguments.no_fail:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
