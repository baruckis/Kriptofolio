import hashlib
import json
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parent))
import android_scenario_evidence as evidence  # noqa: E402


HEAD = "1" * 40
BASE = "2" * 40


def png_chunk(kind, value):
    data = kind + value
    return struct.pack(">I", len(value)) + data + struct.pack(">I", zlib.crc32(data) & 0xFFFFFFFF)


def tiny_png():
    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)
    pixels = zlib.compress(b"\x00\x20\x80\xc0")
    return signature + png_chunk(b"IHDR", ihdr) + png_chunk(b"IDAT", pixels) + png_chunk(b"IEND", b"")


def tiny_mp4(duration_ms=2000):
    ftyp = struct.pack(">I4s4s4s", 16, b"ftyp", b"isom", b"isom")
    mvhd = struct.pack(">I4sIIIIII", 32, b"mvhd", 0, 0, 0, 1000, duration_ms, 0)
    mdat = struct.pack(">I4s", 9, b"mdat") + b"x"
    return ftyp + mvhd + mdat


class AndroidScenarioEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="android-scenario-evidence-")
        self.root = Path(self.temporary.name) / "run-123"
        self.root.mkdir()
        (self.root / "videos").mkdir()
        (self.root / "frames").mkdir()
        self.write_bundle()

    def tearDown(self):
        self.temporary.cleanup()

    def manifest(self):
        video = self.root / "videos/mainlist-add-search.mp4"
        frames = [self.root / "frames/before-main-list.png",
                  self.root / "frames/add-search-screen.png"]
        value = {
            "schema_version": 1,
            "repository": "baruckis/Kriptofolio",
            "pull_request": None,
            "tested_commit_sha": HEAD,
            "base_commit_sha": BASE,
            "dirty": False,
            "run": {"id": "run-123", "kind": "local",
                    "started_at": "2026-09-30T13:00:00Z", "ended_at": "2026-09-30T13:00:03Z"},
            "environment": {
                "platform": "android", "profile": "demo-debug-espresso-offline-apk-" + "0" * 64,
                "tool_versions": {"adb": "Android Debug Bridge 1.0.41", "gradle": "8.8; AGP 8.6.0",
                                  "android_api": "34"},
                "viewport": None,
                "device": "AVD test; SDK/API 34; 720x1600 px; density 280 dpi; locale en-US; animations 1/1/1",
                "synthetic_data": True,
            },
            "capture_status": "complete",
            "scenarios": [{"id": "mainlist-add-search", "title": "Open crypto search from an empty demo portfolio",
                           "acceptance_criteria_ids": ["AC-ANDROID-ADD-SEARCH"], "status": "passed",
                           "limitations": ["Local demo flavor only."]}],
            "steps": [
                {"id": "step-empty-main-list", "scenario_id": "mainlist-add-search",
                 "action": "Open the empty demo portfolio screen", "expected": "The add action is visible",
                 "at_ms": 300, "checkpoint_frame_id": "before-main-list"},
                {"id": "step-add-search", "scenario_id": "mainlist-add-search",
                 "action": "Tap the add action", "expected": "The add-search screen opens",
                 "at_ms": 1500, "checkpoint_frame_id": "add-search-screen"},
            ],
            "assertions": [{"id": "assert-add-search-visible", "scenario_id": "mainlist-add-search",
                            "criterion_id": "AC-ANDROID-ADD-SEARCH",
                            "check": "Espresso verifies the add-search container is visible",
                            "result": "passed", "expected": "The add-search container is visible",
                            "actual": "The add-search container is visible", "source": "android_espresso_test"}],
            "videos": [{"scenario_id": "mainlist-add-search", "path": "videos/mainlist-add-search.mp4",
                        "mime": "video/mp4", "bytes": video.stat().st_size,
                        "sha256": hashlib.sha256(video.read_bytes()).hexdigest(), "duration_ms": 2000}],
            "frames": [],
            "limitations": ["Local emulator only; physical-device coverage is not established."],
        }
        for identifier, path, step_id, at_ms in (
                ("before-main-list", frames[0], "step-empty-main-list", 300),
                ("add-search-screen", frames[1], "step-add-search", 1500)):
            data = path.read_bytes()
            value["frames"].append({"id": identifier, "scenario_id": "mainlist-add-search",
                                    "path": f"frames/{identifier}.png", "mime": "image/png",
                                    "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
                                    "at_ms": at_ms, "step_id": step_id})
        return value

    def write_bundle(self, manifest=None):
        (self.root / "videos/mainlist-add-search.mp4").write_bytes(tiny_mp4())
        for path in (self.root / "frames/before-main-list.png",
                     self.root / "frames/add-search-screen.png"):
            path.write_bytes(tiny_png())
        value = manifest if manifest is not None else self.manifest()
        (self.root / "scenario-evidence.json").write_text(
            json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    def validate(self):
        return evidence.validate(self.root / "scenario-evidence.json", HEAD)

    def test_accepts_complete_passed_android_package(self):
        self.assertEqual(self.validate()[:2], (1, 2))

    def test_accepts_complete_failed_harness_probe(self):
        value = self.manifest()
        value["scenarios"][0]["status"] = "failed"
        value["scenarios"][0]["acceptance_criteria_ids"].append("AC-ANDROID-FAILURE-PROBE")
        value["assertions"].append({
            "id": "assert-intentional-failure-probe", "scenario_id": "mainlist-add-search",
            "criterion_id": "AC-ANDROID-FAILURE-PROBE",
            "check": "The harness records an intentional failing assertion",
            "result": "failed", "expected": "The deliberate failure is present in the test report",
            "actual": "The deliberate failure is present in the test report",
            "source": "android_espresso_test",
        })
        self.write_bundle(value)
        self.assertEqual(self.validate()[:2], (1, 2))

    def test_rejects_a_different_expected_head(self):
        with self.assertRaisesRegex(evidence.EvidenceError, "does not match"):
            evidence.validate(self.root / "scenario-evidence.json", "3" * 40)

    def test_rejects_a_symlinked_manifest(self):
        link = self.root / "linked-manifest.json"
        link.symlink_to(self.root / "scenario-evidence.json")
        with self.assertRaisesRegex(evidence.EvidenceError, "not a regular file"):
            evidence.validate(link, HEAD)

    def test_rejects_changed_video_bytes(self):
        value = self.manifest()
        value["videos"][0]["sha256"] = "0" * 64
        self.write_bundle(value)
        with self.assertRaisesRegex(evidence.EvidenceError, "SHA-256"):
            self.validate()

    def test_rejects_boolean_video_duration(self):
        value = self.manifest()
        value["videos"][0]["duration_ms"] = True
        self.write_bundle(value)
        with self.assertRaisesRegex(evidence.EvidenceError, "video.duration_ms: integer"):
            self.validate()

    def test_rejects_nested_directories(self):
        (self.root / "videos/frames").mkdir()
        with self.assertRaisesRegex(evidence.EvidenceError, "unexpected directory"):
            self.validate()


if __name__ == "__main__":
    unittest.main()
