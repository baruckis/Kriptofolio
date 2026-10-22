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

"""List the blocks in an APK's signing block and fail if the dependency-info block is present.

The Android Gradle plugin writes an encrypted list of the app's dependencies into the APK
signing block (block ID 0x504b4453) unless `dependenciesInfo { includeInApk = false }` is set.
`apksigner` does not show this block, so this script reads the signing block layout directly:
https://source.android.com/docs/security/features/apksigning/v2#apk-signing-block

Usage: python3 tools/apk_signing_blocks.py path/to/app.apk
Exit code 0: no dependency-info block. 1: block found. 2: no signing block (unsigned APK) or
wrong usage. 3: the file cannot be read or is not a valid APK.
"""

import struct
import sys

MAGIC = b"APK Sig Block 42"
KNOWN = {
    0x7109871A: "APK Signature Scheme v2",
    0xF05368C0: "APK Signature Scheme v3",
    0x1B93AD61: "APK Signature Scheme v3.1",
    0x42726577: "verity padding",
    0x504B4453: "dependency info (DEPENDENCY_INFO_BLOCK_ID)",
}
DEPENDENCY_INFO = 0x504B4453


def block_ids(data):
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        raise ValueError("not a ZIP file")
    central_directory = struct.unpack("<I", data[eocd + 16:eocd + 20])[0]
    if data[central_directory - 16:central_directory] != MAGIC:
        return None
    size = struct.unpack("<Q", data[central_directory - 24:central_directory - 16])[0]
    # The block starts with its size (8 bytes, not counted in it), then the ID-value pairs.
    position = central_directory - size
    end = central_directory - 24
    ids = []
    while position < end:
        length = struct.unpack("<Q", data[position:position + 8])[0]
        ids.append(struct.unpack("<I", data[position + 8:position + 12])[0])
        position += 8 + length
    return ids


def main():
    if len(sys.argv) != 2:
        print("Usage: python3 tools/apk_signing_blocks.py path/to/app.apk")
        return 2
    try:
        with open(sys.argv[1], "rb") as apk:
            ids = block_ids(apk.read())
    except (OSError, ValueError, struct.error) as error:
        print("ERROR: cannot read the APK signing block: %s" % error)
        return 3
    if ids is None:
        print("No APK signing block: the APK is unsigned, so there is nothing to check.")
        return 2
    for block in ids:
        print("0x%08x  %s" % (block, KNOWN.get(block, "unknown")))
    if DEPENDENCY_INFO in ids:
        print("FAIL: the dependency-info block is present.")
        return 1
    print("OK: no dependency-info block.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
