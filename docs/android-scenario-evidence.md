# Local Android scenario evidence

This pilot records one existing demo-flavor Espresso test on a disposable Android emulator. It
produces a short MP4, two timestamped PNG checkpoints, and a manifest tying them to the exact
commit, emulator profile, and demo APK hash. Everything stays under the ignored
`app/build/outputs/android-scenario-evidence/` directory.

This example verifies one navigation behavior: from an empty demo portfolio, tapping the add
button opens the add/search screen. The offline capture cannot load CoinMarketCap data, prove search
results, exercise the full app, or cover a physical device. It must never run on a device that
contains a personal portfolio. The test checks that the demo portfolio is empty before saving the
first frame, and the capture script disables emulator Wi-Fi and mobile data while the test runs,
verifies both are disabled, then restores their previous states afterward.

## Prepare a disposable emulator

Use an API 34 emulator that contains no personal or production app data. Set its screen profile,
locale, and animations explicitly. These commands affect only the selected emulator:

```sh
export ANDROID_HOME="/path/to/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ADB="$ANDROID_HOME/platform-tools/adb"

"$ADB" -s emulator-5554 shell wm size 720x1600
"$ADB" -s emulator-5554 shell wm density 280
"$ADB" -s emulator-5554 shell settings put global window_animation_scale 1.0
"$ADB" -s emulator-5554 shell settings put global transition_animation_scale 1.0
"$ADB" -s emulator-5554 shell settings put global animator_duration_scale 1.0
```

The script refuses capture above 2,000,000 pixels, without an explicit animation value, from a
physical device, or from a dirty Git worktree. Use the project's documented demo application ID
and keep the demo portfolio empty.

## Capture and validate

Run from the repository root and select the local emulator serial:

```sh
python3 scripts/android_scenario_evidence.py capture --serial emulator-5554
```

The command builds the demo APK and instrumentation APK, waits until the demo activity is in the
foreground, starts screen recording, then releases the Espresso test through a temporary
app-private readiness marker. It runs only `MainListFragmentTest.clickAddFab_opensAddCryptoUi` and
saves a package after that test passes. The video therefore starts with the tested app in view,
without recording Gradle startup or an idle emulator launcher.
The package records the exact checked-out HEAD and merge base, emulator/API/screen/density/locale/
animation values, Android tools, demo APK SHA-256, video duration and SHA-256, and each checkpoint's
SHA-256 and timestamp. It does not include the APK, source files, portfolio data, or a PR URL.

Validate a package against the current HEAD:

```sh
python3 scripts/android_scenario_evidence.py validate \
  --manifest app/build/outputs/android-scenario-evidence/<run-id>/scenario-evidence.json \
  --expected-head "$(git rev-parse HEAD)"
```

To verify that the harness distinguishes a real UI test from an intentional failure, run:

```sh
python3 scripts/android_scenario_evidence.py capture \
  --serial emulator-5554 --probe-failure
```

That probe first performs the same Espresso UI assertions and captures both checkpoints, then
raises a deliberate assertion. Exit status `2` and a valid manifest marked `failed` are expected
for this command; the failed package is not passing feature evidence. Other test failures discard
the recording. It refuses to start if it cannot read the emulator's Wi-Fi or mobile-data state. On
normal success or failure exits, the script removes its temporary emulator video/checkpoint files
and restores those states.

## Storage, cleanup, and limits

- Evidence is local, ignored by Git, capped by the tool, and not sent to GitHub or another service.
- A run can be removed by its exact ID; automatic cleanup considers only valid packages created by
  this repository's local capture tool, identified by its repository and run ID, that are more than
  24 hours old. Cleanup can remove an older package even when a newer validator no longer accepts
  its format:

  ```sh
  python3 scripts/android_scenario_evidence.py cleanup --run-id <run-id>
  python3 scripts/android_scenario_evidence.py cleanup
  ```

- The validator checks the manifest schema, package paths, PNG structure and decoded pixel data,
  MP4 container markers and declared duration, byte limits, checksums, exact HEAD, and checkpoint
  links. Capture strips ancillary PNG chunks before packaging so emulator color metadata does not
  enter the evidence. It does not decode or play the MP4. A successful Espresso result is the
  behavioral check; a reviewer still needs to view the video to assess what is visible.
- Because this local-only pilot does not upload or attach artifacts, a remote pull request reviewer
  cannot access its video. Public PR video delivery remains blocked until a zero-cost, permitted
  artifact route is verified; do not commit the binary or enable a paid workflow to work around it.

This procedure is project-specific and self-contained. It does not require a private coordination
repository, an AI account, a hosted reviewer, or a paid service.
