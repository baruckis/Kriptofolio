#!/usr/bin/env python3
"""Capture and validate one local, synthetic Android UI scenario."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import signal
import struct
import subprocess
import sys
import tempfile
import time
import zlib
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from xml.etree import ElementTree


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_ROOT = ROOT / "app" / "build" / "outputs" / "android-scenario-evidence"
REPOSITORY = "baruckis/Kriptofolio"
SCENARIO_ID = "mainlist-add-search"
TEST_CLASS = "com.baruckis.kriptofolio.ui.mainlist.MainListFragmentTest"
TEST_METHOD = "clickAddFab_opensAddCryptoUi"
APP_ID = "com.baruckis.kriptofolio.demo"
MAX_MANIFEST_BYTES = 64_000
MAX_BUNDLE_BYTES = 20_000_000
MAX_VIDEO_BYTES = 10_000_000
MAX_FRAME_BYTES = 1_000_000
MAX_FRAME_PIXELS = 2_000_000
MAX_VIDEO_DURATION_MS = 90_000
MAX_LOCAL_BYTES = 100_000_000
RUN_ID_RE = re.compile(r"run-[A-Za-z0-9-]{1,80}\Z")
SHA1_RE = re.compile(r"[0-9a-fA-F]{40}\Z")
SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")
ID_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}\Z")
TEXT_PATH_RE = re.compile(r"(?<![A-Za-z0-9_])(?:/(?:[^\s/]+/)+[^\s/]+|[A-Za-z]:[\\/][^\s]+|\\\\[^\s]+|~/[^\s]+)")
TEXT_COMMAND_RE = re.compile(
    r"(?i)(?:^|[;\n|&])\s*(?:sh\s+-c|bash\s+-c|zsh\s+-c|"
    r"python(?:3)?\s+|node\s+|npm\s+(?:ci|install|run|exec)|"
    r"npx\s+|yarn\s+|pnpm\s+|bun\s+|cat\s+|rm\s+|git\s+|"
    r"adb\s+|gradle\s+|curl\s+|wget\s+)"
)
TOP_LEVEL_FIELDS = {
    "schema_version", "repository", "pull_request", "tested_commit_sha",
    "base_commit_sha", "dirty", "run", "environment", "capture_status",
    "scenarios", "steps", "assertions", "videos", "frames", "limitations",
}


class EvidenceError(ValueError):
    """Raised when capture or local evidence validation fails."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise EvidenceError(message)


def exact_fields(value: dict, expected: set[str], where: str) -> None:
    require(set(value) == expected,
            f"{where}: unexpected or missing fields: " + ", ".join(sorted(set(value) ^ expected)))


def bounded_text(value, where: str, maximum: int = 500) -> str:
    require(isinstance(value, str) and 0 < len(value.strip()) <= maximum,
            f"{where}: expected 1–{maximum} characters of text")
    require("\x00" not in value and "<" not in value and ">" not in value,
            f"{where}: NUL and HTML are not allowed")
    require("://" not in value and TEXT_PATH_RE.search(value) is None,
            f"{where}: URLs and absolute paths are not allowed")
    require(TEXT_COMMAND_RE.search(value) is None and
            not any(token in value for token in ("$(", "`", "&&", "||")),
            f"{where}: shell text is not allowed")
    return value


def bounded_list(value, where: str, maximum: int, minimum: int = 0) -> list:
    require(isinstance(value, list) and minimum <= len(value) <= maximum,
            f"{where}: list length must be {minimum}–{maximum}")
    return value


def exact_int(value, where: str, minimum: int, maximum: int) -> int:
    require(type(value) is int and minimum <= value <= maximum,
            f"{where}: integer must be {minimum}–{maximum}")
    return value


def reject_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise EvidenceError(f"JSON contains a duplicate key: {key}")
        result[key] = value
    return result


def reject_nonstandard_constant(value):
    raise EvidenceError(f"JSON contains an invalid number: {value}")


def read_manifest(path: Path) -> dict:
    require(not path.is_symlink() and path.is_file(), "manifest is not a regular file")
    require(path.stat().st_size <= MAX_MANIFEST_BYTES, "manifest is too large")
    try:
        return json.loads(path.read_text(encoding="utf-8"),
                          object_pairs_hook=reject_duplicate_keys,
                          parse_constant=reject_nonstandard_constant)
    except (OSError, UnicodeError, json.JSONDecodeError, RecursionError) as error:
        raise EvidenceError(f"could not read manifest: {error}") from error


def safe_relative_path(value, where: str, prefix: str, name: str,
                       extension: str) -> PurePosixPath:
    bounded_text(value, where, 240)
    path = PurePosixPath(value)
    require(not path.is_absolute() and "\\" not in value and
            all(part not in {"", ".", ".."} for part in path.parts),
            f"{where}: expected a safe relative POSIX path")
    require(path.as_posix() == value and len(path.parts) == 2 and
            path.parts[0] == prefix and path.name == name + extension,
            f"{where}: path must be {prefix}/{name}{extension}")
    return path


def package_file(root: Path, relative: PurePosixPath, where: str) -> Path:
    current = root
    for part in relative.parts:
        current = current / part
        require(not current.is_symlink(), f"{where}: symlinks are not allowed")
    require(current.is_file(), f"{where}: missing file {relative.as_posix()}")
    return current


def validate_png(data: bytes, where: str) -> tuple[int, int]:
    require(data.startswith(b"\x89PNG\r\n\x1a\n"), f"{where}: not a PNG file")
    offset = 8
    chunks = []
    compressed = bytearray()
    width = height = color_type = bit_depth = None
    seen_end = False
    while offset < len(data):
        require(offset + 12 <= len(data), f"{where}: truncated PNG chunk")
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        end = offset + 12 + length
        require(length <= MAX_FRAME_BYTES and end <= len(data), f"{where}: invalid PNG chunk length")
        kind = data[offset + 4:offset + 8]
        value = data[offset + 8:offset + 8 + length]
        checksum = struct.unpack(">I", data[offset + 8 + length:end])[0]
        require(zlib.crc32(kind + value) & 0xFFFFFFFF == checksum,
                f"{where}: PNG chunk checksum mismatch")
        chunks.append(kind)
        if kind == b"IHDR":
            require(width is None and length == 13, f"{where}: invalid PNG IHDR")
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", value)
            require(compression == 0 and filtering == 0 and interlace == 0,
                    f"{where}: unsupported PNG encoding")
        elif kind == b"IDAT":
            compressed.extend(value)
        elif kind == b"IEND":
            require(length == 0 and end == len(data), f"{where}: invalid PNG IEND")
            seen_end = True
            break
        offset = end
    require(width is not None and height is not None and seen_end and
            chunks[0] == b"IHDR" and chunks[-1] == b"IEND" and b"IDAT" in chunks,
            f"{where}: PNG structure is incomplete")
    require(1 <= width <= 8192 and 1 <= height <= 8192 and width * height <= MAX_FRAME_PIXELS,
            f"{where}: PNG dimensions exceed the evidence limit")
    channels = {2: 3, 6: 4}.get(color_type)
    require(bit_depth == 8 and channels is not None,
            f"{where}: expected an 8-bit RGB or RGBA PNG")
    expected = height * (width * channels + 1)
    inflater = zlib.decompressobj()
    try:
        decoded = inflater.decompress(bytes(compressed), expected + 1)
    except zlib.error as error:
        raise EvidenceError(f"{where}: invalid PNG image data") from error
    require(len(decoded) == expected and inflater.eof and not inflater.unconsumed_tail and
            not inflater.unused_data, f"{where}: PNG pixel data does not match dimensions")
    row_bytes = width * channels + 1
    require(all(decoded[index] <= 4 for index in range(0, len(decoded), row_bytes)),
            f"{where}: PNG row filter is invalid")
    return width, height


def mp4_duration_ms(data: bytes, where: str) -> int:
    require(len(data) >= 32 and data[4:8] == b"ftyp" and b"mdat" in data,
            f"{where}: missing MP4 ftyp or media data")
    marker = data.find(b"mvhd")
    require(marker >= 0 and marker + 24 <= len(data), f"{where}: missing MP4 movie header")
    version = data[marker + 4]
    if version == 0:
        timescale = struct.unpack(">I", data[marker + 16:marker + 20])[0]
        duration = struct.unpack(">I", data[marker + 20:marker + 24])[0]
    elif version == 1 and marker + 36 <= len(data):
        timescale = struct.unpack(">I", data[marker + 24:marker + 28])[0]
        duration = struct.unpack(">Q", data[marker + 28:marker + 36])[0]
    else:
        raise EvidenceError(f"{where}: unsupported MP4 movie header")
    require(timescale > 0 and duration > 0, f"{where}: invalid MP4 duration")
    value = round(duration * 1000 / timescale)
    exact_int(value, where + ".duration_ms", 1, MAX_VIDEO_DURATION_MS)
    return value


def walk_package(root: Path, declared_paths: set[str]) -> None:
    require(not root.is_symlink() and root.is_dir(), "package directory is invalid")
    expected = {"scenario-evidence.json", *declared_paths}
    found = {"scenario-evidence.json"}
    for directory, directory_names, file_names in os.walk(root, followlinks=False):
        current = Path(directory)
        for name in directory_names:
            path = current / name
            require(not path.is_symlink(), "package directory symlink is not allowed")
            require(path.relative_to(root).as_posix() in {"videos", "frames"},
                    f"package contains an unexpected directory: {path.relative_to(root)}")
        for name in file_names:
            path = current / name
            require(not path.is_symlink() and path.is_file(),
                    f"package contains an invalid file: {path.relative_to(root)}")
            found.add(path.relative_to(root).as_posix())
    require(found == expected,
            "package has missing or undeclared files: " + ", ".join(sorted(found ^ expected)))


def validate_manifest(manifest: dict, root: Path, expected_head: str,
                      manifest_size: int) -> tuple[int, int, int]:
    require(isinstance(manifest, dict), "manifest must be an object")
    exact_fields(manifest, TOP_LEVEL_FIELDS, "manifest")
    require(type(manifest["schema_version"]) is int and manifest["schema_version"] == 1,
            "unsupported schema_version")
    require(manifest["repository"] == REPOSITORY, "repository identity does not match")
    require(manifest["pull_request"] is None, "local Android evidence must not claim a PR upload")
    for field in ("tested_commit_sha", "base_commit_sha"):
        require(isinstance(manifest[field], str) and SHA1_RE.fullmatch(manifest[field]) is not None,
                f"{field}: expected a full commit SHA")
    require(manifest["tested_commit_sha"].lower() == expected_head.lower(),
            "tested_commit_sha does not match --expected-head")
    require(manifest["dirty"] is False, "PR evidence must be captured from a clean worktree")
    require(manifest["capture_status"] == "complete",
            "only a complete capture is valid review evidence")

    run = manifest["run"]
    require(isinstance(run, dict), "run must be an object")
    exact_fields(run, {"id", "kind", "started_at", "ended_at"}, "run")
    require(isinstance(run["id"], str) and RUN_ID_RE.fullmatch(run["id"]),
            "run.id is invalid")
    require(run["kind"] == "local", "run.kind must be local")
    def timestamp(value, where):
        bounded_text(value, where, 40)
        require(re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z", value),
                f"{where}: expected UTC timestamp")
        try:
            return datetime.fromisoformat(value[:-1] + "+00:00")
        except ValueError as error:
            raise EvidenceError(f"{where}: invalid timestamp") from error
    require(timestamp(run["ended_at"], "run.ended_at") >=
            timestamp(run["started_at"], "run.started_at"),
            "run ended before it started")

    environment = manifest["environment"]
    require(isinstance(environment, dict), "environment must be an object")
    exact_fields(environment, {"platform", "profile", "tool_versions", "viewport", "device",
                               "synthetic_data"}, "environment")
    require(environment["platform"] == "android" and environment["synthetic_data"] is True,
            "environment must describe synthetic Android data")
    bounded_text(environment["profile"], "environment.profile", 120)
    bounded_text(environment["device"], "environment.device", 160)
    require(environment["viewport"] is None, "Android viewport must be null")
    versions = environment["tool_versions"]
    require(isinstance(versions, dict) and 1 <= len(versions) <= 8,
            "environment.tool_versions must contain 1–8 tools")
    for name, value in versions.items():
        bounded_text(name, "environment.tool_versions name", 80)
        bounded_text(value, "environment.tool_versions version", 100)

    limitations = bounded_list(manifest["limitations"], "limitations", 20)
    for index, value in enumerate(limitations):
        bounded_text(value, f"limitations[{index}]")
    scenarios = bounded_list(manifest["scenarios"], "scenarios", 1, minimum=1)
    scenario = scenarios[0]
    require(isinstance(scenario, dict), "scenario must be an object")
    exact_fields(scenario, {"id", "title", "acceptance_criteria_ids", "status", "limitations"},
                 "scenarios[0]")
    require(scenario["id"] == SCENARIO_ID, "scenario ID is not the supported Android pilot")
    bounded_text(scenario["title"], "scenario.title", 160)
    criteria = bounded_list(scenario["acceptance_criteria_ids"],
                            "scenario.acceptance_criteria_ids", 5, minimum=1)
    require(all(isinstance(item, str) and ID_RE.fullmatch(item) for item in criteria),
            "scenario acceptance criterion ID is invalid")
    require(scenario["status"] in {"passed", "failed"}, "scenario.status is invalid")
    for index, value in enumerate(bounded_list(scenario["limitations"],
                                                "scenario.limitations", 10)):
        bounded_text(value, f"scenario.limitations[{index}]")

    videos = bounded_list(manifest["videos"], "videos", 1, minimum=1)
    video = videos[0]
    require(isinstance(video, dict), "video must be an object")
    exact_fields(video, {"scenario_id", "path", "mime", "bytes", "sha256", "duration_ms"}, "video")
    require(video["scenario_id"] == SCENARIO_ID and video["mime"] == "video/mp4",
            "video identity or MIME type is invalid")
    video_path = safe_relative_path(video["path"], "video.path", "videos", SCENARIO_ID, ".mp4")
    video_size = exact_int(video["bytes"], "video.bytes", 1, MAX_VIDEO_BYTES)
    require(isinstance(video["sha256"], str) and SHA256_RE.fullmatch(video["sha256"]),
            "video.sha256 is invalid")
    video_data = package_file(root, video_path, "video")
    data = video_data.read_bytes()
    require(len(data) == video_size and hashlib.sha256(data).hexdigest() == video["sha256"],
            "video size or SHA-256 does not match the file")
    duration = mp4_duration_ms(data, "video")
    declared_duration = exact_int(video["duration_ms"], "video.duration_ms", 1,
                                  MAX_VIDEO_DURATION_MS)
    require(declared_duration == duration, "video.duration_ms does not match the MP4 header")

    steps = bounded_list(manifest["steps"], "steps", 10, minimum=1)
    step_by_id = {}
    previous_time = -1
    for index, step in enumerate(steps):
        where = f"steps[{index}]"
        require(isinstance(step, dict), f"{where} must be an object")
        exact_fields(step, {"id", "scenario_id", "action", "expected", "at_ms",
                            "checkpoint_frame_id"}, where)
        require(isinstance(step["id"], str) and ID_RE.fullmatch(step["id"]) and
                step["id"] not in step_by_id and step["scenario_id"] == SCENARIO_ID,
                f"{where} identity is invalid")
        bounded_text(step["action"], where + ".action")
        bounded_text(step["expected"], where + ".expected")
        at_ms = exact_int(step["at_ms"], where + ".at_ms", 0, duration)
        require(at_ms > previous_time, f"{where}.at_ms must increase")
        previous_time = at_ms
        frame_id = step["checkpoint_frame_id"]
        require(frame_id is None or (isinstance(frame_id, str) and ID_RE.fullmatch(frame_id)),
                f"{where}.checkpoint_frame_id is invalid")
        step_by_id[step["id"]] = frame_id

    assertions = bounded_list(manifest["assertions"], "assertions", 5, minimum=1)
    assertion_ids = set()
    failed_assertion = False
    for index, assertion in enumerate(assertions):
        where = f"assertions[{index}]"
        require(isinstance(assertion, dict), f"{where} must be an object")
        exact_fields(assertion, {"id", "scenario_id", "criterion_id", "check", "result",
                                 "expected", "actual", "source"}, where)
        require(isinstance(assertion["id"], str) and ID_RE.fullmatch(assertion["id"]) and
                assertion["id"] not in assertion_ids and assertion["scenario_id"] == SCENARIO_ID,
                f"{where} identity is invalid")
        assertion_ids.add(assertion["id"])
        require(assertion["criterion_id"] in criteria, f"{where}.criterion_id is not declared")
        for field in ("check", "expected", "actual"):
            bounded_text(assertion[field], where + "." + field)
        require(assertion["result"] in {"passed", "failed"}, f"{where}.result is invalid")
        bounded_text(assertion["source"], where + ".source", 80)
        failed_assertion = failed_assertion or assertion["result"] == "failed"
    require((scenario["status"] == "failed") == failed_assertion,
            "scenario status does not match its assertions")

    frames = bounded_list(manifest["frames"], "frames", 2, minimum=2)
    frame_ids = set()
    frame_paths = set()
    frame_bytes = 0
    for index, frame in enumerate(frames):
        where = f"frames[{index}]"
        require(isinstance(frame, dict), f"{where} must be an object")
        exact_fields(frame, {"id", "scenario_id", "path", "mime", "bytes", "sha256",
                             "at_ms", "step_id"}, where)
        identifier = frame["id"]
        require(isinstance(identifier, str) and ID_RE.fullmatch(identifier) and
                identifier not in frame_ids and frame["scenario_id"] == SCENARIO_ID,
                f"{where} identity is invalid")
        frame_ids.add(identifier)
        path_value = safe_relative_path(frame["path"], where + ".path", "frames", identifier, ".png")
        require(frame["mime"] == "image/png", f"{where}.mime must be image/png")
        size = exact_int(frame["bytes"], where + ".bytes", 1, MAX_FRAME_BYTES)
        require(isinstance(frame["sha256"], str) and SHA256_RE.fullmatch(frame["sha256"]),
                f"{where}.sha256 is invalid")
        file_path = package_file(root, path_value, where)
        image = file_path.read_bytes()
        require(len(image) == size and hashlib.sha256(image).hexdigest() == frame["sha256"],
                f"{where} size or SHA-256 does not match the file")
        validate_png(image, where)
        at_ms = exact_int(frame["at_ms"], where + ".at_ms", 0, duration)
        require(frame["step_id"] in step_by_id and step_by_id[frame["step_id"]] == identifier,
                f"{where}.step_id does not link to its checkpoint")
        require(any(step["id"] == frame["step_id"] and step["at_ms"] == at_ms for step in steps),
                f"{where}.at_ms does not match its step")
        frame_paths.add(path_value.as_posix())
        frame_bytes += size
    require({step["checkpoint_frame_id"] for step in steps if step["checkpoint_frame_id"]}
            == frame_ids, "every frame must be linked to exactly one checkpoint")
    declared = {video_path.as_posix(), *frame_paths}
    walk_package(root, declared)
    total = manifest_size + video_size + frame_bytes
    require(total <= MAX_BUNDLE_BYTES, "package exceeds the total byte limit")
    return 1, len(frames), total


def validate(manifest_path: Path, expected_head: str) -> tuple[int, int, int]:
    require(isinstance(expected_head, str) and SHA1_RE.fullmatch(expected_head) is not None,
            "expected-head must be a full commit SHA")
    manifest = read_manifest(manifest_path)
    return validate_manifest(manifest, manifest_path.parent, expected_head, manifest_path.stat().st_size)


def command(args, cwd=ROOT, timeout=30, capture=True):
    try:
        result = subprocess.run(args, cwd=cwd, text=True,
                                capture_output=capture, timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise EvidenceError(f"command failed: {Path(args[0]).name}: {error}") from error
    if result.returncode != 0:
        detail = (result.stderr or result.stdout or "").strip()[-1200:] if capture else ""
        raise EvidenceError(f"command exited {result.returncode}: {Path(args[0]).name}: {detail}")
    return result


def adb_command(adb: str, serial: str, *args, capture=True, timeout=30):
    return command([adb, "-s", serial, *args], timeout=timeout, capture=capture)


def adb_text(adb: str, serial: str, *args) -> str:
    return adb_command(adb, serial, *args).stdout.strip()


def local_git_value(*args) -> str:
    return command(["git", *args]).stdout.strip()


def repo_identity() -> None:
    remote = local_git_value("remote", "get-url", "origin")
    normalized = remote.removesuffix(".git")
    require(normalized.lower().endswith("baruckis/kriptofolio"),
            "origin must be the public baruckis/Kriptofolio repository")
    require(Path(local_git_value("rev-parse", "--show-toplevel")).resolve() == ROOT.resolve(),
            "run this command from the Kriptofolio checkout")


def capture_device(adb: str, serial: str) -> dict:
    require(serial.startswith("emulator-"), "scenario evidence capture is limited to a local emulator")
    require(adb_text(adb, serial, "get-state") == "device", "emulator is not ready")
    require(adb_text(adb, serial, "shell", "getprop", "sys.boot_completed") == "1",
            "emulator has not completed boot")
    avd_result = subprocess.run([adb, "-s", serial, "emu", "avd", "name"],
                                capture_output=True, text=True, timeout=10, check=False)
    avd_name = avd_result.stdout.strip().splitlines()[0] if avd_result.returncode == 0 and avd_result.stdout.strip() else "local-emulator"
    api = adb_text(adb, serial, "shell", "getprop", "ro.build.version.sdk")
    require(api.isdigit(), "could not read emulator API level")
    require(api == "34", "the supported Android pilot is pinned to emulator API 34")
    wm_size = adb_text(adb, serial, "shell", "wm", "size")
    sizes = re.findall(r"(?:Physical|Override) size:\s*([1-9][0-9]{0,4})x([1-9][0-9]{0,4})", wm_size)
    require(sizes, "could not read emulator screen size")
    width, height = map(int, sizes[-1])
    require((width, height) == (720, 1600) and width * height <= MAX_FRAME_PIXELS,
            "the supported evidence profile is 720x1600 pixels")
    wm_density = adb_text(adb, serial, "shell", "wm", "density")
    densities = re.findall(r"(?:Physical|Override) density:\s*([1-9][0-9]{0,4})", wm_density)
    require(densities, "could not read emulator screen density")
    require(densities[-1] == "280", "the supported evidence profile is 280 dpi")
    locale = adb_text(adb, serial, "shell", "getprop", "persist.sys.locale") or \
        adb_text(adb, serial, "shell", "getprop", "ro.product.locale")
    require(locale == "en-US", "the supported evidence profile locale is en-US")
    animations = []
    for name in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        value = adb_text(adb, serial, "shell", "settings", "get", "global", name)
        require(re.fullmatch(r"(?:0|[0-9]+(?:\.[0-9]+)?)", value) is not None,
                f"emulator animation setting is not explicit: {name}")
        require(float(value) == 1.0,
                f"emulator animation setting must be 1.0 for this scenario: {name}")
        animations.append(value)
    model = adb_text(adb, serial, "shell", "getprop", "ro.product.model")
    return {
        "api": api,
        "device": f"AVD {avd_name}; SDK/API {api}; {width}x{height} px; density {densities[-1]} dpi; locale {locale}; animations {'/'.join(animations)}",
        "width": width,
        "height": height,
        "locale": locale,
        "model": model,
        "wifi": adb_text(adb, serial, "shell", "dumpsys", "wifi").splitlines()[0],
    }


def screenrecord_uptime_ms(adb: str, serial: str) -> int:
    value = adb_text(adb, serial, "shell", "cat", "/proc/uptime").split()[0]
    return round(float(value) * 1000)


def app_checkpoint(adb: str, serial: str, run_id: str, checkpoint: str):
    package_path = f"cache/scenario-evidence-{run_id}-{checkpoint}"
    timestamp = subprocess.run(
        [adb, "-s", serial, "exec-out", "run-as", APP_ID, "cat", package_path + ".ms"],
        capture_output=True, text=True, timeout=20, check=False)
    timestamp_text = timestamp.stdout.strip()
    if timestamp.returncode != 0 or not timestamp_text.isdigit():
        return None
    image = subprocess.run(
        [adb, "-s", serial, "exec-out", "run-as", APP_ID, "cat", package_path + ".png"],
        capture_output=True, timeout=20, check=False)
    if image.returncode != 0 or not image.stdout.startswith(b"\x89PNG\r\n\x1a\n"):
        return None
    return image.stdout, int(timestamp_text)


def recent_test_case(started_at: float):
    result_root = ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected"
    reports = [path for path in result_root.rglob("TEST-*.xml")
               if path.stat().st_mtime >= started_at - 1]
    if not reports:
        return None, ""
    latest = max(reports, key=lambda path: path.stat().st_mtime)
    try:
        suite = ElementTree.parse(latest).getroot()
    except (OSError, ElementTree.ParseError) as error:
        raise EvidenceError(f"Android test report is invalid: {error}") from error
    for case in suite.findall(".//testcase"):
        if case.attrib.get("classname") == TEST_CLASS and case.attrib.get("name") == TEST_METHOD:
            failure = "\n".join((node.attrib.get("message", "") + "\n" + (node.text or ""))
                                  for node in list(case) if node.tag in {"failure", "error"})
            return case, failure
    return None, ""


def parse_git_identity() -> tuple[str, str]:
    status = local_git_value("status", "--porcelain", "--untracked-files=normal")
    require(not status, "capture requires a clean worktree")
    head = local_git_value("rev-parse", "HEAD")
    base = local_git_value("merge-base", "HEAD", "origin/master")
    require(SHA1_RE.fullmatch(head) is not None and SHA1_RE.fullmatch(base) is not None,
            "could not resolve the exact tested commit and base")
    return head, base


def gradle_apk_facts() -> tuple[str, str]:
    apk = ROOT / "app" / "build" / "outputs" / "apk" / "demo" / "debug" / "app-demo-debug.apk"
    require(apk.is_file(), "demo debug APK is missing; run the documented build command first")
    gradle = command(["./gradlew", "--version"], timeout=30).stdout
    version = re.search(r"(?m)^Gradle\s+([^\s]+)", gradle)
    plugin_text = (ROOT / "versions.gradle").read_text(encoding="utf-8")
    plugin = re.search(r"(?m)^versions\.gradle\s*=\s*['\"]([^'\"]+)", plugin_text)
    require(version is not None and plugin is not None, "could not read Gradle tool versions")
    return hashlib.sha256(apk.read_bytes()).hexdigest(), version.group(1) + "; AGP " + plugin.group(1)


def owned_run_directory(run_id: str) -> Path:
    require(RUN_ID_RE.fullmatch(run_id) is not None, "run ID is invalid")
    require(not OUTPUT_ROOT.is_symlink(), "evidence output root cannot be a symlink")
    root = OUTPUT_ROOT.resolve()
    path = OUTPUT_ROOT / run_id
    require(path.parent.resolve() == root and not path.is_symlink(), "run directory is not owned")
    return path


def cleanup_run(run_id: str) -> bool:
    path = owned_run_directory(run_id)
    if not path.exists():
        return False
    manifest_path = path / "scenario-evidence.json"
    manifest = read_manifest(manifest_path)
    require(isinstance(manifest, dict), "refusing to remove an invalid evidence manifest")
    run = manifest.get("run")
    require(manifest.get("repository") == REPOSITORY and isinstance(run, dict) and
            run.get("id") == run_id,
            "refusing to remove a package that is not owned by this tool")
    validate(manifest_path, manifest.get("tested_commit_sha", ""))
    shutil.rmtree(path)
    return True


def cleanup_expired(now=None) -> list[str]:
    if not OUTPUT_ROOT.exists():
        return []
    require(not OUTPUT_ROOT.is_symlink(), "evidence output root cannot be a symlink")
    removed = []
    cutoff = (time.time() if now is None else now) - 24 * 60 * 60
    for path in OUTPUT_ROOT.iterdir():
        if path.is_dir() and not path.is_symlink() and path.stat().st_mtime < cutoff:
            if cleanup_run(path.name):
                removed.append(path.name)
    return removed


def local_occupied_bytes() -> int:
    if not OUTPUT_ROOT.exists():
        return 0
    return sum(path.stat().st_size for path in OUTPUT_ROOT.rglob("*")
               if path.is_file() and not path.is_symlink())


def screenrecord_pids(adb: str, serial: str) -> list[str]:
    result = subprocess.run([adb, "-s", serial, "shell", "pidof", "screenrecord"],
                            capture_output=True, text=True, timeout=10, check=False)
    return result.stdout.split() if result.returncode == 0 else []


def capture(run_id: str, serial: str, probe_failure: bool) -> tuple[int, Path]:
    repo_identity()
    head, base = parse_git_identity()
    package_directory = owned_run_directory(run_id)
    require(not package_directory.exists(), "this run ID already has a local package")
    adb = os.environ.get("ADB") or shutil.which("adb")
    require(adb is not None, "adb was not found; set ADB to the local Android SDK platform-tools binary")
    device = capture_device(adb, serial)
    require(len(device["device"]) <= 160, "emulator description exceeds the manifest limit")
    command(["./gradlew", "--no-daemon", "--console=plain",
             ":app:assembleDemoDebug", ":app:assembleDemoDebugAndroidTest"], timeout=300)
    apk_sha, gradle_version = gradle_apk_facts()
    adb_version = command([adb, "version"]).stdout.splitlines()[0]
    wifi_was_enabled = "Wi-Fi is enabled" in device["wifi"]
    if local_occupied_bytes() + 20_000_000 > MAX_LOCAL_BYTES:
        cleanup_expired()
        require(local_occupied_bytes() + 20_000_000 <= MAX_LOCAL_BYTES,
                "local evidence storage limit would be exceeded; clean an owned old run first")

    run_started = datetime.now(timezone.utc).replace(microsecond=0)
    remote_video = f"/sdcard/Download/kriptofolio-scenario-evidence-{run_id}.mp4"
    recorder = None
    recorder_pid = None
    local_directory = None
    local_directory_created = False
    test_status = None
    test_failure = ""
    test_exit = 1
    test_started = 0.0
    record_start_uptime = 0
    gradle_process = None
    gradle_log = None
    checkpoint_results = {}
    try:
        removed = cleanup_expired()
        if removed:
            print("Removed owned expired runs: " + ", ".join(removed))
        adb_command(adb, serial, "shell", "svc", "wifi", "disable")
        adb_command(adb, serial, "shell", "rm", "-f", remote_video)
        require(not screenrecord_pids(adb, serial), "an unrelated screenrecord process is already running")
        recorder = subprocess.Popen(
            [adb, "-s", serial, "shell", "screenrecord", "--time-limit", "90",
             "--size", f"{device['width']}x{device['height']}", "--bit-rate", "750000", remote_video],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
        time.sleep(0.5)
        require(recorder.poll() is None, "Android screenrecord did not start")
        pids = screenrecord_pids(adb, serial)
        require(len(pids) == 1 and pids[0].isdigit(), "could not identify the task-owned screenrecord process")
        recorder_pid = pids[0]
        record_start_uptime = screenrecord_uptime_ms(adb, serial)

        gradle_args = ["./gradlew", "--no-daemon", "--console=plain",
                       ":app:connectedDemoDebugAndroidTest",
                       f"-Pandroid.testInstrumentationRunnerArguments.class={TEST_CLASS}#{TEST_METHOD}",
                       f"-Pandroid.testInstrumentationRunnerArguments.scenarioEvidenceRunId={run_id}"]
        if probe_failure:
            gradle_args.append("-Pandroid.testInstrumentationRunnerArguments.scenarioEvidenceProbe=assertion-failure")
        test_started = time.time()
        gradle_log = tempfile.TemporaryFile(mode="w+t")
        gradle_process = subprocess.Popen(gradle_args, cwd=ROOT, stdout=gradle_log,
                                          stderr=subprocess.STDOUT, text=True, start_new_session=True)
        checkpoint_ids = ("before-main-list", "add-search-screen")
        test_deadline = time.monotonic() + 180
        while gradle_process.poll() is None and len(checkpoint_results) < len(checkpoint_ids) and \
                time.monotonic() < test_deadline:
            for checkpoint in checkpoint_ids:
                if checkpoint not in checkpoint_results:
                    value = app_checkpoint(adb, serial, run_id, checkpoint)
                    if value is not None:
                        checkpoint_results[checkpoint] = value
            if len(checkpoint_results) < len(checkpoint_ids):
                time.sleep(0.1)
        try:
            gradle_process.communicate(timeout=max(0.1, test_deadline - time.monotonic()))
        except subprocess.TimeoutExpired:
            os.killpg(gradle_process.pid, signal.SIGTERM)
            try:
                gradle_process.communicate(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(gradle_process.pid, signal.SIGKILL)
                gradle_process.communicate()
        gradle_log.seek(0, os.SEEK_END)
        log_size = gradle_log.tell()
        gradle_log.seek(max(0, log_size - 8_000))
        gradle_output = gradle_log.read()
        test_exit = gradle_process.returncode or 0
        case, test_failure = recent_test_case(test_started)
        if case is None:
            print(gradle_output[-2500:], file=sys.stderr)
            raise EvidenceError("the exact Espresso test produced no fresh test result")
        require(set(checkpoint_results) == set(checkpoint_ids),
                "the Espresso process removed its app before both checkpoints could be collected")
        failures = list(case.findall("failure")) + list(case.findall("error"))
        if not failures:
            test_status = "passed"
        elif probe_failure and "Scenario evidence intentional failure probe" in test_failure:
            test_status = "failed"
        elif "Scenario evidence requires the empty demo screen" in test_failure:
            raise EvidenceError("the demo portfolio is not empty; recording was discarded to protect device data")
        else:
            raise EvidenceError("the Espresso test failed outside the intentional failure probe; recording discarded")
        if probe_failure:
            require(test_status == "failed" and test_exit != 0,
                    "failure probe did not report its intentional assertion")
        else:
            require(test_status == "passed" and test_exit == 0,
                    "the Espresso test did not pass")

        if recorder.poll() is None:
            pids = screenrecord_pids(adb, serial)
            require(recorder_pid in pids, "task-owned screenrecord process disappeared")
            adb_command(adb, serial, "shell", "kill", "-INT", recorder_pid)
        try:
            output = recorder.communicate(timeout=10)[0] or ""
        except subprocess.TimeoutExpired:
            recorder.kill()
            output = recorder.communicate()[0] or ""
            raise EvidenceError("screenrecord did not stop cleanly")
        require(recorder.returncode == 0, "screenrecord did not finish successfully: " + output[-500:])
        require(adb_text(adb, serial, "shell", "test", "-s", remote_video) == "",
                "screenrecord did not create a video")

        local_directory = package_directory
        local_directory.mkdir(parents=True, exist_ok=False)
        local_directory_created = True
        video_directory = local_directory / "videos"
        frame_directory = local_directory / "frames"
        video_directory.mkdir()
        frame_directory.mkdir()
        local_video = video_directory / f"{SCENARIO_ID}.mp4"
        adb_command(adb, serial, "pull", remote_video, str(local_video), capture=True)
        checkpoints = {}
        for checkpoint in ("before-main-list", "add-search-screen"):
            target = frame_directory / f"{checkpoint}.png"
            image, device_timestamp = checkpoint_results[checkpoint]
            target.write_bytes(image)
            checkpoints[checkpoint] = max(0, device_timestamp - record_start_uptime)
        require(checkpoints["add-search-screen"] > checkpoints["before-main-list"],
                "checkpoint timestamps did not increase")
        duration = mp4_duration_ms(local_video.read_bytes(), "video")
        require(checkpoints["add-search-screen"] <= duration,
                "the final checkpoint is outside the recorded video")

        frame_steps = [
            {"id": "step-empty-main-list", "scenario_id": SCENARIO_ID,
             "action": "Open the empty demo portfolio screen", "expected": "The add action is visible",
             "at_ms": checkpoints["before-main-list"], "checkpoint_frame_id": "before-main-list"},
            {"id": "step-add-search", "scenario_id": SCENARIO_ID,
             "action": "Tap the add action", "expected": "The add-search screen opens",
             "at_ms": checkpoints["add-search-screen"], "checkpoint_frame_id": "add-search-screen"},
        ]
        criterion_ids = ["AC-ANDROID-ADD-SEARCH"]
        assertions = [{
            "id": "assert-add-search-visible", "scenario_id": SCENARIO_ID,
            "criterion_id": "AC-ANDROID-ADD-SEARCH",
            "check": "Espresso verifies the add-search container is visible",
            "result": "passed", "expected": "The add-search container is visible",
            "actual": "The add-search container is visible", "source": "android_espresso_test",
        }]
        scenario_limits = ["Demo flavor only; no CoinMarketCap request is made while Wi-Fi is disabled.",
                           "This navigation check does not prove crypto search results or network behavior."]
        if probe_failure:
            criterion_ids.append("AC-ANDROID-FAILURE-PROBE")
            assertions.append({
                "id": "assert-intentional-failure-probe", "scenario_id": SCENARIO_ID,
                "criterion_id": "AC-ANDROID-FAILURE-PROBE",
                "check": "The harness records an intentional failing assertion",
                "result": "failed", "expected": "The deliberate failure is present in the test report",
                "actual": "The deliberate failure is present in the test report", "source": "android_espresso_test",
            })
            scenario_limits.append("The final failed assertion is an intentional local harness probe.")
        video_data = local_video.read_bytes()
        frame_values = []
        for checkpoint, step_id in (("before-main-list", "step-empty-main-list"),
                                    ("add-search-screen", "step-add-search")):
            frame_path = frame_directory / f"{checkpoint}.png"
            frame_data = frame_path.read_bytes()
            frame_values.append({
                "id": checkpoint, "scenario_id": SCENARIO_ID,
                "path": f"frames/{checkpoint}.png", "mime": "image/png",
                "bytes": len(frame_data), "sha256": hashlib.sha256(frame_data).hexdigest(),
                "at_ms": checkpoints[checkpoint], "step_id": step_id,
            })
        run_ended = datetime.now(timezone.utc).replace(microsecond=0)
        manifest = {
            "schema_version": 1, "repository": REPOSITORY, "pull_request": None,
            "tested_commit_sha": head, "base_commit_sha": base, "dirty": False,
            "run": {"id": run_id, "kind": "local",
                    "started_at": run_started.strftime("%Y-%m-%dT%H:%M:%SZ"),
                    "ended_at": run_ended.strftime("%Y-%m-%dT%H:%M:%SZ")},
            "environment": {
                "platform": "android", "profile": f"demo-debug-espresso-offline-apk-{apk_sha}",
                "tool_versions": {"adb": adb_version, "gradle": gradle_version,
                                   "android_api": device["api"]},
                "viewport": None, "device": device["device"], "synthetic_data": True,
            },
            "capture_status": "complete",
            "scenarios": [{"id": SCENARIO_ID, "title": "Open crypto search from an empty demo portfolio",
                           "acceptance_criteria_ids": criterion_ids,
                           "status": test_status, "limitations": scenario_limits}],
            "steps": frame_steps,
            "assertions": assertions,
            "videos": [{"scenario_id": SCENARIO_ID, "path": f"videos/{SCENARIO_ID}.mp4",
                        "mime": "video/mp4", "bytes": len(video_data),
                        "sha256": hashlib.sha256(video_data).hexdigest(), "duration_ms": duration}],
            "frames": frame_values,
            "limitations": ["Local emulator only; this recording does not establish physical-device coverage."],
        }
        manifest_path = local_directory / "scenario-evidence.json"
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
                                 encoding="utf-8")
        validate(manifest_path, head)
        return (2 if test_status == "failed" else 0), local_directory
    except BaseException:
        if local_directory_created and local_directory is not None and local_directory.exists():
            shutil.rmtree(local_directory)
        raise
    finally:
        if gradle_process is not None and gradle_process.poll() is None:
            try:
                os.killpg(gradle_process.pid, signal.SIGTERM)
                gradle_process.communicate(timeout=10)
            except Exception:
                try:
                    os.killpg(gradle_process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                gradle_process.communicate()
        if gradle_log is not None:
            gradle_log.close()
        if recorder_pid is not None:
            try:
                pids = screenrecord_pids(adb, serial)
                if recorder_pid in pids:
                    adb_command(adb, serial, "shell", "kill", "-INT", recorder_pid)
                    for _ in range(30):
                        if recorder_pid not in screenrecord_pids(adb, serial):
                            break
                        time.sleep(0.1)
                    if recorder_pid in screenrecord_pids(adb, serial):
                        adb_command(adb, serial, "shell", "kill", "-TERM", recorder_pid,
                                    capture=True, timeout=10)
            except Exception:
                pass
        if recorder is not None and recorder.poll() is None:
            try:
                recorder.communicate(timeout=10)
            except Exception:
                recorder.kill()
                recorder.communicate()
        subprocess.run([adb, "-s", serial, "shell", "rm", "-f", remote_video],
                       capture_output=True, timeout=10, check=False)
        for checkpoint in ("before-main-list", "add-search-screen"):
            package_path = f"cache/scenario-evidence-{run_id}-{checkpoint}"
            subprocess.run([adb, "-s", serial, "shell", "run-as", APP_ID, "rm", "-f",
                            package_path + ".png", package_path + ".ms"],
                           capture_output=True, timeout=10, check=False)
        subprocess.run([adb, "-s", serial, "shell", "svc", "wifi",
                        "enable" if wifi_was_enabled else "disable"],
                       capture_output=True, timeout=10, check=False)


def parser() -> argparse.ArgumentParser:
    value = argparse.ArgumentParser(description="Capture or validate local Kriptofolio Android scenario evidence.")
    commands = value.add_subparsers(dest="command", required=True)
    capture_parser = commands.add_parser("capture")
    capture_parser.add_argument("--serial", required=True, help="connected local emulator serial, for example emulator-5554")
    capture_parser.add_argument("--probe-failure", action="store_true",
                                help="run the deliberate failing assertion after the real UI checks")
    validate_parser = commands.add_parser("validate")
    validate_parser.add_argument("--manifest", required=True)
    validate_parser.add_argument("--expected-head", required=True)
    cleanup_parser = commands.add_parser("cleanup")
    cleanup_parser.add_argument("--run-id")
    return value


def main(argv=None) -> int:
    args = parser().parse_args(argv)
    try:
        if args.command == "capture":
            run_id = datetime.now(timezone.utc).strftime("run-%Y%m%dT%H%M%SZ-") + os.urandom(4).hex()
            code, path = capture(run_id, args.serial, args.probe_failure)
            print(f"run: {run_id}")
            print(f"status: {'failed probe (expected)' if code == 2 else 'passed'}")
            print(f"package: {path}")
            print("upload: none (local-only)")
            return code
        if args.command == "validate":
            manifest_path = Path(args.manifest).absolute()
            scenarios, frames, total = validate(manifest_path, args.expected_head)
            print(f"valid package ({scenarios} scenario, {frames} frames, {total} bytes)")
            return 0
        if args.run_id:
            removed = cleanup_run(args.run_id)
            print("removed owned run" if removed else "owned run was already absent")
        else:
            removed = cleanup_expired()
            print("removed expired owned runs: " + (", ".join(removed) if removed else "none"))
        return 0
    except EvidenceError as error:
        print("android-scenario-evidence: " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
