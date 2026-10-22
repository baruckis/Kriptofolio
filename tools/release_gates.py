#!/usr/bin/env python3
# Copyright 2026 Andrius Baruckis www.baruckis.com | kriptofolio.app
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Check the release rules that a machine can check (RELEASING.md, "Release gates").

1. The CoinMarketCap key constant of the full flavor is empty, so no key enters the public history.
2. Every fastlane locale has a changelog for the current versionCode, and it is not empty and not
   longer than 500 characters, the limit of Google Play's "What's new" field.

The third gate, both flavors assembling in release mode, is a Gradle command (RELEASING.md).

Usage: python3 tools/release_gates.py [repository root, default: current directory]
Exit code 0: all gates pass. 1: a gate fails; each failure is printed.
"""

import re
import sys
from pathlib import Path

KEY_FILE = "app/src/full/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt"
BUILD_FILE = "app/build.gradle"
LOCALES_DIR = "fastlane/metadata/android"
CHANGELOG_LIMIT = 500


def check_key(root):
    text = (root / KEY_FILE).read_text(encoding="utf-8")
    match = re.search(r'^const val API_SERVICE_AUTHENTICATION_KEY = "(.*?)"', text, re.M)
    if match is None:
        return ["%s: API_SERVICE_AUTHENTICATION_KEY not found" % KEY_FILE]
    if match.group(1) != "":
        return ["%s: API_SERVICE_AUTHENTICATION_KEY is not empty; remove the key before committing" % KEY_FILE]
    return []


def version_code(root):
    text = (root / BUILD_FILE).read_text(encoding="utf-8")
    codes = re.findall(r"^\s*versionCode\s*=?\s*(\d+)\s*$", text, re.M)
    if len(codes) != 1:
        raise ValueError("%s: expected one versionCode, found %d" % (BUILD_FILE, len(codes)))
    return codes[0]


def check_changelogs(root, code):
    locales = sorted(path for path in (root / LOCALES_DIR).iterdir() if path.is_dir())
    if not locales:
        return ["%s: no locale directories" % LOCALES_DIR]
    failures = []
    for locale in locales:
        changelog = locale / "changelogs" / ("%s.txt" % code)
        relative = changelog.relative_to(root)
        if not changelog.is_file():
            failures.append("%s: missing" % relative)
            continue
        text = changelog.read_text(encoding="utf-8").strip()
        if not text:
            failures.append("%s: empty" % relative)
        elif len(text) > CHANGELOG_LIMIT:
            failures.append("%s: %d characters, the limit is %d" % (relative, len(text), CHANGELOG_LIMIT))
    return failures


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    try:
        code = version_code(root)
        failures = check_key(root) + check_changelogs(root, code)
    except (OSError, ValueError) as error:
        print("FAIL: %s" % error)
        return 1
    for failure in failures:
        print("FAIL: %s" % failure)
    if failures:
        return 1
    print("OK: the key constant is empty and every locale has a changelog for versionCode %s." % code)
    return 0


if __name__ == "__main__":
    sys.exit(main())
