import hashlib
import json
import shutil
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path
from unittest.mock import patch


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


def png_with_color_metadata():
    value = tiny_png()
    ihdr_end = 8 + 25
    return value[:ihdr_end] + png_chunk(b"sRGB", b"\x00") + \
        png_chunk(b"sBIT", b"\x08\x08\x08") + value[ihdr_end:]


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

    def test_reads_wifi_state_from_its_dump_line(self):
        self.assertTrue(evidence.wifi_enabled_from_dump("Verbose logging is off\nWi-Fi is enabled\n"))
        self.assertFalse(evidence.wifi_enabled_from_dump("Verbose logging is off\nWi-Fi is disabled\n"))
        with self.assertRaisesRegex(evidence.EvidenceError, "previous Wi-Fi state"):
            evidence.wifi_enabled_from_dump("Verbose logging is off\n")

    def test_reads_mobile_data_state_explicitly(self):
        self.assertTrue(evidence.mobile_data_enabled_from_settings("1"))
        self.assertFalse(evidence.mobile_data_enabled_from_settings("0"))
        with self.assertRaisesRegex(evidence.EvidenceError, "previous mobile-data state"):
            evidence.mobile_data_enabled_from_settings("null")

    def test_waits_for_network_state_transition(self):
        with patch.object(evidence, "emulator_network_state",
                          side_effect=[(True, True), (False, False)]), \
                patch.object(evidence.time, "sleep"):
            evidence.wait_for_network_state("adb", "emulator-5554", False, False)

    def test_restores_both_previous_network_states_and_verifies_them(self):
        with patch.object(evidence, "adb_command") as adb_command, \
                patch.object(evidence, "wait_for_network_state") as wait_for_state:
            errors = evidence.restore_emulator_network_state("adb", "emulator-5554", True, False)

        self.assertEqual(errors, [])
        self.assertEqual(adb_command.call_count, 2)
        self.assertEqual(adb_command.call_args_list[0].args,
                         ("adb", "emulator-5554", "shell", "svc", "wifi", "enable"))
        self.assertEqual(adb_command.call_args_list[1].args,
                         ("adb", "emulator-5554", "shell", "svc", "data", "disable"))
        wait_for_state.assert_called_once_with("adb", "emulator-5554", True, False)

    def test_reports_network_restore_command_and_verification_failures(self):
        with patch.object(evidence, "adb_command", side_effect=[
                evidence.EvidenceError("wifi restore failed"),
                evidence.EvidenceError("mobile data restore failed")]), \
                patch.object(evidence, "wait_for_network_state",
                             side_effect=evidence.EvidenceError("state mismatch")):
            errors = evidence.restore_emulator_network_state("adb", "emulator-5554", True, True)

        self.assertEqual(errors, ["wifi restore failed", "mobile data restore failed", "state mismatch"])

    def test_sigterm_handler_ignores_repeated_termination_before_cleanup(self):
        with patch.object(evidence.signal, "signal") as set_signal:
            with self.assertRaisesRegex(evidence.EvidenceError, "cleanup will run"):
                evidence.interrupt_capture_on_sigterm(evidence.signal.SIGTERM, None)

        set_signal.assert_called_once_with(evidence.signal.SIGTERM, evidence.signal.SIG_IGN)

    def test_reads_resumed_activity_package_from_api_34_dump(self):
        dump = """ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)
  topResumedActivity=ActivityRecord{f3a1 u0 com.baruckis.kriptofolio.demo/.ui.mainlist.MainActivity t42}
"""
        self.assertEqual(evidence.resumed_activity_package(dump), "com.baruckis.kriptofolio.demo")

    def test_ignores_activity_dump_without_a_resumed_component(self):
        self.assertIsNone(evidence.resumed_activity_package("mResumedActivity: none\n"))

    def test_detects_when_instrumentation_uninstalls_the_demo_app(self):
        with patch.object(evidence, "adb_text", return_value="") as adb_text:
            self.assertFalse(evidence.app_is_installed("adb", "emulator-5554"))
        adb_text.assert_called_once_with("adb", "emulator-5554", "shell", "pm", "list",
                                         "packages", evidence.APP_ID)

    def test_detects_an_installed_demo_app_for_private_file_cleanup(self):
        with patch.object(evidence, "adb_text",
                          return_value="package:" + evidence.APP_ID):
            self.assertTrue(evidence.app_is_installed("adb", "emulator-5554"))

    def test_matches_the_exact_demo_package_id(self):
        with patch.object(evidence, "adb_text",
                          return_value="package:" + evidence.APP_ID + ".other"):
            self.assertFalse(evidence.app_is_installed("adb", "emulator-5554"))

    def test_rejects_an_unexpected_package_list_response(self):
        with patch.object(evidence, "adb_text", return_value="not-a-package-name"):
            with self.assertRaisesRegex(evidence.EvidenceError,
                                        "could not determine whether the demo app is installed"):
                evidence.app_is_installed("adb", "emulator-5554")

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

    def test_rejects_unreadable_media_directory(self):
        def fail_on_unreadable(top, *, followlinks, onerror):
            if Path(top) == self.root:
                onerror(PermissionError(13, "permission denied", str(self.root / "frames")))
            return iter(())

        with patch.object(evidence.os, "walk", side_effect=fail_on_unreadable):
            with self.assertRaisesRegex(evidence.EvidenceError,
                                        "unable to read package directory.*frames"):
                self.validate()

    def test_cleanup_removes_an_owned_older_package(self):
        workspace = (Path(self.temporary.name) / "workspace").resolve()
        output_root = workspace / evidence.OUTPUT_RELATIVE_ROOT
        run_id = "run-123"
        package = output_root / run_id
        output_root.mkdir(parents=True)
        shutil.copytree(self.root, package)
        with patch.object(evidence, "ROOT", workspace), patch.object(evidence, "OUTPUT_ROOT", output_root):
            self.assertTrue(evidence.cleanup_run(run_id))
        self.assertFalse(package.exists())

    def test_cleanup_refuses_unlisted_file(self):
        workspace = (Path(self.temporary.name) / "workspace").resolve()
        output_root = workspace / evidence.OUTPUT_RELATIVE_ROOT
        run_id = "run-123"
        package = output_root / run_id
        output_root.mkdir(parents=True)
        shutil.copytree(self.root, package)
        extra = package / "frames/extra.png"
        extra.write_bytes(tiny_png())
        with patch.object(evidence, "ROOT", workspace), patch.object(evidence, "OUTPUT_ROOT", output_root):
            with self.assertRaisesRegex(evidence.EvidenceError, "missing or undeclared files"):
                evidence.cleanup_run(run_id)
        self.assertTrue(package.is_dir())
        self.assertTrue(extra.is_file())

    def test_cleanup_refuses_unreadable_directory(self):
        workspace = (Path(self.temporary.name) / "workspace").resolve()
        output_root = workspace / evidence.OUTPUT_RELATIVE_ROOT
        run_id = "run-123"
        package = output_root / run_id
        output_root.mkdir(parents=True)
        shutil.copytree(self.root, package)

        def fail_on_unreadable(top, *, followlinks, onerror):
            if Path(top) == package:
                onerror(PermissionError(13, "permission denied", str(package / "frames")))
            return iter(())

        with patch.object(evidence, "ROOT", workspace), \
                patch.object(evidence, "OUTPUT_ROOT", output_root), \
                patch.object(evidence.os, "walk", side_effect=fail_on_unreadable):
            with self.assertRaisesRegex(evidence.EvidenceError,
                                        "unable to read package directory.*frames"):
                evidence.cleanup_run(run_id)
        self.assertTrue(package.is_dir())

    def test_cleanup_refuses_a_foreign_package(self):
        workspace = (Path(self.temporary.name) / "workspace").resolve()
        output_root = workspace / evidence.OUTPUT_RELATIVE_ROOT
        run_id = "run-123"
        package = output_root / run_id
        package.mkdir(parents=True)
        (package / "scenario-evidence.json").write_text(json.dumps({
            "repository": "someone/else",
            "run": {"id": run_id, "kind": "local"},
        }), encoding="utf-8")
        with patch.object(evidence, "ROOT", workspace), patch.object(evidence, "OUTPUT_ROOT", output_root):
            with self.assertRaisesRegex(evidence.EvidenceError, "not owned by this tool"):
                evidence.cleanup_run(run_id)
        self.assertTrue(package.is_dir())

    def test_cleanup_refuses_symlinked_output_parent(self):
        workspace = (Path(self.temporary.name) / "workspace").resolve()
        external = Path(self.temporary.name) / "external"
        (workspace / "app").mkdir(parents=True)
        external.mkdir()
        (workspace / "app/build").symlink_to(external, target_is_directory=True)
        output_root = workspace / evidence.OUTPUT_RELATIVE_ROOT
        package = output_root / "run-123"
        package.mkdir(parents=True)
        shutil.copytree(self.root, package, dirs_exist_ok=True)

        with patch.object(evidence, "ROOT", workspace), patch.object(evidence, "OUTPUT_ROOT", output_root):
            with self.assertRaisesRegex(evidence.EvidenceError, "output path cannot contain symlinks"):
                evidence.cleanup_run("run-123")

        self.assertTrue((package / "scenario-evidence.json").is_file())
        self.assertTrue((external / "outputs/android-scenario-evidence/run-123").is_dir())

    def test_strips_emulator_color_metadata_for_factory_contract(self):
        sanitized = evidence.strip_png_metadata(png_with_color_metadata(), "test frame")
        self.assertNotIn(b"sRGB", sanitized)
        self.assertNotIn(b"sBIT", sanitized)
        self.assertEqual(evidence.validate_png(sanitized, "test frame"), (1, 1))

    def test_rejects_ancillary_png_metadata_in_package(self):
        self.write_bundle()
        path = self.root / "frames/before-main-list.png"
        path.write_bytes(png_with_color_metadata())
        (self.root / "scenario-evidence.json").write_text(
            json.dumps(self.manifest(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(evidence.EvidenceError, "PNG metadata is not allowed"):
            self.validate()


if __name__ == "__main__":
    unittest.main()
