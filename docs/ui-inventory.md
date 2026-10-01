# UI inventory — reproducible public subset

This inventory publishes five real screenshots for representative portfolio states: empty,
synthetic data, refresh error with cached data, delete with Undo, and Hebrew right-to-left (RTL).
The images are emulator captures; they were not generated or altered by AI.

## Evidence identity and limits

| Item | Value |
|---|---|
| App source | Kriptofolio candidate commit `17c66998b135dad828317e57eb7ea6ae0d4b9042` |
| Artifact | `demoDebug` built from that source commit; application ID `com.baruckis.kriptofolio.demo` |
| Toolchain | Eclipse Temurin JDK 21.0.11, Gradle 8.8 wrapper, Android SDK platform 34 |
| Device | A new, disposable Android Emulator `default` arm64 image, Pixel 6 profile, API 34 / Android 14 |
| Display | 1080 × 2400 pixels, 420 dpi, three-button navigation |
| Device locale / time zone | `en-US` / `Europe/Vilnius`; Hebrew was selected in the app's own language setting |
| Network | Wi-Fi and mobile data disabled before launch and during all captures |
| Data | The fictional four-coin portfolio from the `v1.2.3` database fixture proposed in [PR #24](https://github.com/baruckis/Kriptofolio/pull/24); no real user's data |

These are screenshots of a locally built debug APK at the exact source commit above. They are not
evidence from the signed 1.2.3 release APK or from a Play installation. The APK's `versionName`
remains 1.2.3, but its build identity is the source commit and variant listed here. The demo
flavor's sandbox service is retired; network access was disabled, so no API request or credential
was used. The full flavor still has no production API key in this public repository.

The database file used for the populated states was added by [PR #24](https://github.com/baruckis/Kriptofolio/pull/24)
and is now available from `master` after merge commit
[a123008](https://github.com/baruckis/Kriptofolio/commit/a12300803badb300babbc3d37c1d56e6aa720f5c).
Its SHA-256 is `50860db1cf9a2035d63828963b335f39159e0a21f3acd9dddcff4adff40b2da2`. The
screenshots still identify their app source commit and demo build separately; adding the fixture
to `master` did not change application code or the captures.

## Public captures

| State | Evidence | Preparation and visible result |
|---|---|---|
| Empty portfolio, English | [empty screenshot](ui-evidence/empty-portfolio-demo-api34-en.png) | Fresh app data before the fixture is copied; empty-list message and demo subtitle |
| Portfolio data, English | [data screenshot](ui-evidence/portfolio-data-demo-api34-en.png) | Fixture copied; four fictional holdings and totals are displayed |
| Refresh error with cached data, English | [error screenshot](ui-evidence/portfolio-refresh-error-demo-api34-en.png) | Fixture copied, network disabled, pull to refresh; rows remain and `Unable to refresh.` / `RETRY` appears |
| Delete and Undo, English | [Undo screenshot](ui-evidence/portfolio-undo-demo-api34-en.png) | Long-press one holding, tap delete, capture the `Deleted: 1` / `UNDO` snackbar within its 2.75-second window |
| Portfolio data, Hebrew RTL | [RTL screenshot](ui-evidence/portfolio-data-demo-api34-iw.png) | Fixture copied, then choose Hebrew (`עִברִית`) in Settings → Language; the toolbar, totals, columns, cards and FAB mirror |

The populated screenshots show the fixture's stored fetch time using the device's `Europe/Vilnius`
time zone. The app appends the literal label `UTC` even though it formats that value locally, so
the displayed time must not be read as UTC; this existing behaviour is documented as K1 in the
behaviour specification.

The states listed here are the complete public screenshot set in this PR. Other states from the
historical inventory are deliberately not represented by these images.

## Reproduce the public subset

Use a new disposable API 34 `default` arm64 emulator. Do not use a personal device or an emulator
containing user data. Run the commands from a clean Kriptofolio checkout based on `master`; it now contains the PR #24 fixture.
The `default` image supports `adb root`, which is needed to install the synthetic database file.

Build and install the demo variant, then capture the empty portfolio before seeding it:

```sh
./gradlew assembleDemoDebug
APP=com.baruckis.kriptofolio.demo
adb install -r app/build/outputs/apk/demo/debug/app-demo-debug.apk
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell svc wifi disable
adb shell svc data disable
adb shell am start -n "$APP/com.baruckis.kriptofolio.ui.mainlist.MainActivity"
adb exec-out screencap -p > empty-portfolio.png
```

The first launch creates the app's empty database and preferences. Stop the app, copy the public
synthetic fixture from PR #24, and set its owner to the disposable emulator's app UID:

```sh
adb root
adb shell am force-stop "$APP"
adb shell rm -f "/data/data/$APP/databases/kriptofolio-db" \
  "/data/data/$APP/databases/kriptofolio-db-wal" \
  "/data/data/$APP/databases/kriptofolio-db-shm"
APP_UID=$(adb shell stat -c '%u' "/data/data/$APP" | tr -d '\r')
adb push app/src/test/resources/db/kriptofolio-v1.2.3.db \
  "/data/data/$APP/databases/kriptofolio-db"
adb shell chown "$APP_UID:$APP_UID" "/data/data/$APP/databases/kriptofolio-db"
adb shell chmod 660 "/data/data/$APP/databases/kriptofolio-db"
adb push app/src/test/resources/db/kriptofolio-v1.2.3-preferences.xml \
  "/data/data/$APP/shared_prefs/com.baruckis.kriptofolio.demo_preferences.xml"
adb shell chown "$APP_UID:$APP_UID" \
  "/data/data/$APP/shared_prefs/com.baruckis.kriptofolio.demo_preferences.xml"
adb shell chmod 660 \
  "/data/data/$APP/shared_prefs/com.baruckis.kriptofolio.demo_preferences.xml"
adb shell am start -n "$APP/com.baruckis.kriptofolio.ui.mainlist.MainActivity"
adb exec-out screencap -p > portfolio-data.png
```

For the cached refresh error, pull down on the top portfolio row and capture after the snackbar
appears:

```sh
adb shell input swipe 540 900 540 1550 700
sleep 2
adb exec-out screencap -p > portfolio-refresh-error.png
```

For the Undo state, long-press the first holding, tap the trash icon in the selection toolbar,
then capture immediately; the Undo action is transient:

```sh
adb shell input swipe 520 950 520 950 1200
adb shell input tap 910 200
adb exec-out screencap -p > portfolio-undo.png
```

For RTL, open the overflow menu, choose Settings → Language → Hebrew (`עִברִית`), return to the
portfolio, and capture it:

```sh
adb exec-out screencap -p > portfolio-data-iw.png
```

The coordinates above were exercised on the display size in the evidence table. With another
emulator profile, use the same visible controls and confirm the state in the screenshot rather
than assuming the coordinates identify it.

## Historical or not repeated

The earlier inventory described a much broader 29-state matrix and 105 screenshots held with
private article material. Those images and the private capture script are not included here and
are not public evidence. This PR repeats only the five states above, using accessible synthetic
data and ordinary emulator actions.

The following historical states remain un-repeated in this public subset:

- Add/search first-open loading, empty-cache error, list refresh, search and amount-dialog states;
- settings, licence screens, external app hand-offs, landscape, and the system dark-mode comparison;
- the remaining combinations of screen state × locale (including RTL versions of screens other
  than the portfolio);
- transient loading states that last too briefly for a stable capture.

The app's normal demo network flow cannot provide a populated list because its sandbox host is
retired, and the public full flavor has no production key. This PR does not add a local demo data
mode or change the app's data layer. A later, separate task may provide deterministic demo data if
future UI work needs those additional states.
