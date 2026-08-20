# UPGRADE-NOTES.md — Stage 0 "keep-alive" release (compileSdk / targetSdk 35)

Working notes for the smallest change set that makes Kriptofolio releasable again before
Google Play stops showing apps that target API 34 or lower to new users (2026-08-31).

Scope of this stage, deliberately narrow:

* `compileSdk` and `targetSdk` → **35** (not 36 — see "2.0 backlog" at the bottom),
* `minSdk` stays **21**,
* toolchain raised only as far as required to make 35 build at all,
* every dependency raised to the **oldest** version that fixes a real, observed error,
* **no** feature work, **no** refactoring, **no** code-style modernization.

---

## 1. Baseline — the state of `master` before this branch

Recorded on **2026-08-20**, commit `e0f0a08` (`master`), branch `stage0/target-sdk-35`.

### Declared toolchain (`versions.gradle`, `gradle/wrapper/gradle-wrapper.properties`)

| Item | Version |
|---|---|
| Android Gradle plugin | 7.4.2 |
| Gradle wrapper | 7.5 |
| Kotlin | 1.6.21 |
| `compileSdk` | 34 |
| `minSdk` | 21 |
| `targetSdk` | 34 |
| `versionCode` / `versionName` | 4 / 1.2.1 |
| Java source/target compatibility | 1.8 (`jvmTarget = "1.8"`) |

### Local environment used for the baseline attempt

| Item | Value |
|---|---|
| Machine | macOS 15 (Darwin 25.6.0), Apple Silicon |
| JDK | Temurin 17.0.19 (`JAVA_HOME` pointed at it explicitly) |
| Android SDK | `~/Library/Android/sdk` |
| Installed on demand for the baseline | `platforms;android-34`, `build-tools;33.0.1`, `build-tools;34.0.0`, `cmdline-tools;latest` |
| Android SDK licenses | accepted via `sdkmanager --licenses` |

No source file, build file or Gradle property was modified for the baseline attempt.

### Baseline build result: **RED — and not because of anything in this repository**

```
./gradlew assembleFullDebug assembleDemoDebug testFullDebugUnitTest
```

fails during dependency resolution:

```
> Could not resolve all files for configuration ':app:demoDebugRuntimeClasspath'.
   > Failed to transform flipview-1.2.0.aar (eu.davidea:flipview:1.2.0) ...
      > Failed to transform ... using Jetifier.
        Reason: IllegalArgumentException, message: Unsupported class file major version 65.
```

The cause is external and worth writing down carefully, because it changes the shape of this
whole stage:

* The project resolves dependencies from `jcenter()`. JCenter is decommissioned; the host now
  answers every request with `301 Moved Permanently` to Maven Central. So `jcenter()` still
  "works", but it silently serves **whatever Maven Central has under that coordinate today**.
* `eu.davidea:flipview:1.2.0` — the small view library used for the flipping coin icons — was
  **re-published to Maven Central on 2026-03-07 under the same version number**. Same
  coordinate, different artifact:
  * built with Gradle 9.4.0, compiled to **Java 21 bytecode** (class file major version 65),
  * carries `minCompileSdk=35` in its AAR metadata,
  * declares a (spurious — the library is two plain `.java` files) dependency on
    `org.jetbrains.kotlin:kotlin-stdlib:2.2.10`.

Three consequences, each confirmed by a separate probe build:

| Probe | Command | Result |
|---|---|---|
| A — untouched project | `assembleFullDebug` | Jetifier cannot read class file version 65 |
| B — Jetifier off | `-Pandroid.enableJetifier=false` | `checkFullDebugAarMetadata`: *"Dependency 'eu.davidea:flipview:1.2.0' requires ... compile against version 35 or later. :app is currently compiled against android-34."* |
| C — compileSdk 35, Jetifier skipping flipview | `-Pandroid.jetifier.ignorelist=flipview -Pandroid.suppressUnsupportedCompileSdk=35` | Kotlin compiles, then `mergeExtDexFullDebug` fails: *"D8: java.lang.IllegalArgumentException: Unsupported class file major version 65"* for both `flipview-1.2.0.aar` and `kotlin-stdlib-2.2.10.jar` |

**Conclusion: there is no green baseline to preserve.** `master` as published does not build on
any machine today, regardless of the Android 15 deadline. Probe C also settles a question this
stage would otherwise have had to guess at: moving to AGP 8 is **required**, not merely
preferred — the D8 shipped with AGP 7.4.2 cannot dex Java 21 class files, so no amount of
flag-twiddling keeps the old plugin alive.

Because the baseline is red, **unit-test and Lint baselines could not be captured**. Section 4
records what is used instead for the "no new Lint errors" comparison.

### Warnings already present in the baseline (for later comparison)

* `Warning: The 'kotlin-android-extensions' Gradle plugin is deprecated.` — the plugin was
  *removed* in Kotlin 1.8, so this is a hard blocker for any Kotlin upgrade (see section 3).
* `WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 34. This
  Android Gradle plugin (7.4.2) was tested up to compileSdk = 33.`
* `Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.`
* `Configuration 'demoDebugRuntimeClasspath' was resolved during configuration time.`
* Manifest merger warnings about duplicate namespaces coming from `com.android.support`
  artifacts that Jetifier rewrites (`dagger-android-support`, `stetho`).

---

## 2. What changed, commit by commit

| Commit | What | Build after it |
|---|---|---|
| `docs: Added UPGRADE-NOTES.md...` | this file | red (was already red) |
| `refactor: Replaced the Kotlin Android Extensions plugin...` | synthetics → `findViewById`, `@Parcelize` → `kotlin-parcelize` | red |
| `chore: Replaced the shut down JCenter repository...` | `jcenter()` → `mavenCentral()` | red |
| `test: Replaced mockito-all 2.0.2-beta with mockito-core 5.3.1.` | test dependency only | red |
| `chore: Upgraded to Gradle 8.7, Android Gradle plugin 8.6 and Kotlin 1.9.25.` | the toolchain, plus `compileSdk 35` (the wrapper moved to 8.8 later, see below) | **green — first buildable commit** |
| `fix: Opted out of the Android 15 edge to edge enforcement...` | `values-v35/styles.xml` | green |
| `chore: Raised targetSdk to 35.` | the actual point of the stage | green |
| `chore: Bumped versionCode to 5 and versionName to 1.2.2.` | release version | green |
| `docs: Added the missing fastlane release changelogs.` | closes issue #4 | green |
| `docs: Corrected the required JDK...` | section 6 rewritten | green |
| `chore: Raised the Gradle wrapper to 8.8 so Android Studio can sync.` | wrapper only | green |

The first four commits cannot be green because `master` itself is not green (section 1).
Everything from the toolchain commit onward builds and tests clean.

## 3. Dependency and toolchain changes

Every line here was forced by an error that was actually observed. Nothing was raised
"because a newer version exists".

| Item | Old | New | Why |
|---|---|---|---|
| Android Gradle plugin | 7.4.2 | 8.6.0 | AGP 7.4.2's D8 cannot dex the Java 21 class files in `flipview`. 8.6.0 is the lowest 8.x that supports `compileSdk 35` silently — 8.4.2 and 8.5.2 both print "tested up to compileSdk 34". |
| Gradle wrapper | 7.5 | 8.8 | 8.7 is the minimum AGP 8.6 accepts, and was the original choice. It had to move again: Android Studio injects a diagnostic init script calling `gradle.lifecycle.afterProject`, and `Gradle.getLifecycle()` only exists from **Gradle 8.8**, so on 8.7 every IDE sync fails with `Could not get unknown property 'lifecycle'`. 8.8 is the oldest release that has it. |
| Kotlin | 1.6.21 | 1.9.25 | AGP 8 needs Kotlin ≥ 1.8, and 1.8 is where `kotlin-android-extensions` was removed anyway. 1.9.25 is the last 1.9; 2.x is deliberately out of scope. |
| `compileSdk` | 34 | 35 | `flipview` declares `minCompileSdk 35`, and 35 is needed for `targetSdk 35`. |
| `targetSdk` | 34 | 35 | The whole point of the stage. |
| `minSdk` | 21 | 21 | Unchanged. |
| Mockito | `mockito-all` 2.0.2-beta | `mockito-core` 5.3.1 | cglib cannot generate mocks on a modern JDK. 5.3.1 is the oldest release whose Byte Buddy (1.14.4) knows the JDK this project now needs. No test source change. |
| Repositories | `jcenter()` | `mavenCentral()` | JCenter is shut down and only redirects; Gradle 8 deprecates the shortcut. |
| `flipview` | — | same version, `kotlin-stdlib` excluded | The republished artifact drags in `kotlin-stdlib 2.2.10`. The library is two plain Java files and does not use it. |
| Jetifier | on | on | Still required: `dagger-android-support`, `stetho` and `android.arch.navigation` all reference legacy support artifacts (`./gradlew checkJetifier` confirms). Left alone on purpose. |

### Transitive change worth knowing about

`androidx.lifecycle` resolves to **2.6.1** instead of 2.2.0-rc03. This is not a choice: AGP
8.6's own `androidx.databinding:databinding-runtime:8.6.0` carries dependency constraints that
pin the whole `androidx.lifecycle` group to 2.6.1. The declared version in `versions.gradle` is
left at 2.2.0-rc03 so the diff stays honest about what the project asks for versus what the
plugin imposes. The only source consequence was `Transformations` (section 2).

### Dependencies deliberately NOT touched

`dagger 2.23.2`, `room 2.2.3`, `glide 4.11.0`, `navigation 1.0.0`, `constraint_layout
2.0.0-beta4`, `recyclerview 1.2.0-alpha01`, `recyclerview_selection 1.1.0-beta01`, `okhttp
4.3.1`, `retrofit 2.7.1`, `appcompat 1.1.0`, `material 1.0.0`, `preference 1.1.0`, `browser
1.2.0`, `coroutines 1.0.1`, `stetho 1.5.1`, `oss_licenses 17.0.1`, `ktx 1.2.0-rc01`, `junit
4.13`, `espresso/runner/rules 3.x-alpha`.

Notably, **kapt with Dagger 2.23.2, Room 2.2.3 and Glide 4.11.0 all run fine** on the new
toolchain, which was the upgrade most expected to be needed. They were left alone.

## 4. Verification

All commands run with `JAVA_HOME` pointing at Temurin 21 (see section 6).

| Check | Result |
|---|---|
| `./gradlew assembleFullDebug assembleDemoDebug` | pass |
| `./gradlew testFullDebugUnitTest` | pass — 14 tests, 0 failures |
| `./gradlew testDemoDebugUnitTest` | pass — 14 tests, 0 failures |
| `./gradlew assembleFullRelease assembleDemoRelease` | pass (unsigned) |
| `./gradlew bundleFullRelease bundleDemoRelease` | pass |
| `./gradlew lintFullDebug` | 2 errors, 52 warnings — **exactly the same set as before `targetSdk` was raised** |
| Native `.so` libraries in the release APKs and AABs | **none** — pure Kotlin/Java, so the Play 16 KB page size requirement for apps targeting 15+ is satisfied with nothing to do |
| Shipped manifest | `minSdkVersion 21`, `targetSdkVersion 35`, `versionCode 5`, `versionName 1.2.2`, `package com.baruckis.kriptofolio` (`.demo` for the demo flavor) |

### Lint comparison

There is no `master` Lint run to compare against (section 1). The next best comparison was run
instead: Lint on the toolchain commit (`compileSdk 35`, `targetSdk 34`) versus Lint on the
final branch (`targetSdk 35`), same plugin, same everything else.

```
targetSdk 34:  2 errors, 50 warnings   {NotificationPermission: 1, SuspiciousIndentation: 1}
targetSdk 35:  2 errors, 50 warnings   {NotificationPermission: 1, SuspiciousIndentation: 1}
new errors: none      new warnings: none
```

Both errors pre-date this branch:

* `NotificationPermission` — raised against `com.bumptech.glide.request.target.NotificationTarget`,
  a Glide class the app never uses. It fires for any app targeting 33+, so it was already firing
  at `targetSdk 34`.
* `SuspiciousIndentation` at `SettingsFragment.kt:226` — a **real, pre-existing bug**, see the
  backlog below. Not fixed here: it changes user visible behaviour and is out of scope for a
  keep-alive release.

### Database safety, verified rather than assumed

`fallbackToDestructiveMigration()` is enabled on the Room builder, so a schema identity mismatch
would be destructive. The generated Room code was compared against the **published v1.2.1 APK**
(downloaded from the GitHub release):

```
identity hash   ad1c80913f23361aa985d56ecf84d645   identical
legacy hash     e2771731f66eeb1e66880bbf9343332c   identical
CREATE TABLE all_cryptocurrencies (...)            identical
CREATE TABLE my_cryptocurrencies  (...)            identical
```

An existing v1 database therefore opens with no migration and no destructive fallback. This
still deserves a real device check before upload — the manual checklist in the pull request
starts with it.

### Smoke test on an Android 15 emulator (API 35, arm64)

The published v1.2.1 demo APK and this branch's demo build were each installed on a clean
Android 15 emulator and photographed on the main screen. **The two screenshots are
byte-identical**, which is the strongest available evidence that the edge-to-edge opt-out
works: the window still stops below the status bar, and the status bar is still painted with
the app's own `colorPrimaryDark` — which is exactly the mechanism
`PrimaryActionModeController` uses when it recolours the status bar for the contextual
action mode.

Also exercised on Android 15, with no crash and no logcat exception:

* main screen, add-coin screen (including its error state),
* settings, third-party software list, a full license page,
* the donate dialog and its click-to-copy (the file that changed most),
* switching the app language to Hebrew — full RTL mirroring,
* rotation to portrait and back while in Hebrew.

Not reachable without an API key, left for manual testing: adding a coin, portfolio totals,
the contextual action mode, and pull-to-refresh.

## 5. Android 15 behaviour changes for apps targeting SDK 35

Every item on the official list was checked against what this app actually does.

| Change | Applies? | Why |
|---|---|---|
| Edge-to-edge enforced | **yes** | Opted out in `values-v35`. This is the only item that needed work. |
| `Window.setStatusBarColor` / `setNavigationBarColor` become no-ops | **yes, and handled** | `PrimaryActionModeController` recolours the status bar. The opt-out restores the old behaviour; verified by the identical screenshots above. |
| `elegantTextHeight` defaults to true | no | Only affects Arabic, Lao, Myanmar, Tamil, Gujarati, Kannada, Malayalam, Odia, Telugu and Thai. The app ships en, lt, iw (Hebrew) and sw-rKE. Hebrew is worth a glance anyway during manual testing. |
| `Configuration` screen size now includes the system bars | no | The app never reads `Configuration.screenWidthDp` / `screenHeightDp` or display metrics. |
| Predictive back / `enableOnBackInvokedCallback` | no | No `onBackPressed()` override anywhere; only `onSupportNavigateUp()`, which is unaffected. |
| Foreground service types, timeouts, boot-completed restrictions | no | The app declares no services. |
| Stricter background activity launches | no | The app never starts an activity from the background. |
| Ordered broadcast priority, `registerReceiver` rules | no | No broadcast receivers. |
| Restricted TLS versions | no | Only `INTERNET` and HTTPS through OkHttp; a real TLS request to `pro-api.coinmarketcap.com` succeeded from the Android 15 emulator during the smoke test. |
| 16 KB page size (a Play requirement for apps targeting 15+, not a behaviour change) | no work needed | No `.so` files in the artifacts at all. |
| Privacy, camera, media, photo picker, `MediaStore` changes | no | The app requests no runtime permissions and touches none of those APIs. |
| Minimum installable target SDK 24 | no | Irrelevant — this app targets 35. |

## 6. How to build this branch

**This branch needs exactly Java 21 — not 17, and not anything newer.** The window is one
version wide, and both edges were hit for real. Set it explicitly; do not assume the default
is right.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)     # exactly 21 - see the floor and ceiling below
./gradlew assembleFullDebug assembleDemoDebug
./gradlew testFullDebugUnitTest testDemoDebugUnitTest
./gradlew lintFullDebug
./gradlew bundleFullRelease
```

### The floor: Java 21, because of a dependency

`eu.davidea:flipview:1.2.0` is now published as Java 21 bytecode (class file major version 65),
data binding generates Java that imports `FlipView`, and javac refuses to read a class file
newer than itself. On JDK 17:

```
error: cannot access FlipView
  bad class file: .../flipview-1.2.0-api.jar(/eu/davidea/flipview/FlipView.class)
    class file has wrong version 65.0, should be 61.0
```

### The ceiling: pin 21, because nothing above it has been verified

Java 21 is the only version this project has actually been built and tested on:

| JVM | Result |
|---|---|
| Temurin 17.0.19 | fails in kapt — see the floor above |
| **Temurin 21.0.11** | **green: both flavors, all tests, both bundles** |
| JetBrains Runtime 25.0.2 (Android Studio's bundled runtime) | fails |

Gradle 8.8 itself documents support up to Java 22, so 22 may well work — but it has not been
tried here, and the component that breaks first is not Gradle. On JBR 25 the wrapper starts
happily (`./gradlew --version` and `./gradlew help` both succeed) and the build dies much later,
inside kapt, with the JVM version as the entire error text:

```
Execution failed for task ':app:kaptFullDebugKotlin'.
> Error while evaluating property 'javacOptions' of task ':app:kaptFullDebugKotlin'.
   > Failed to calculate the value of task ':app:kaptFullDebugKotlin' property 'javacOptions'.
      > 25.0.2
```

Kotlin 1.9.25 predates that JVM and cannot parse its version. So "the wrapper started, therefore
the JDK is fine" is not a safe inference here — verify with `./gradlew --version`, which prints
the JVM actually in use, and pin 21 until somebody tests otherwise.

### Android Studio setup — required, and it is not the default

Android Studio runs Gradle on its **bundled JBR**, which is newer than 21, so a fresh clone
fails to sync out of the box:

```
The project's Gradle version Gradle 8.8 is incompatible with the Gradle JVM version 25.
```

Point the IDE at a real JDK 21 in
**Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**, or through the
**"Change Gradle JDK configuration"** link the sync error offers.

> **Do not trust an entry in that dropdown just because it is named 21.** Android Studio keeps
> its registered JDKs in `jdk.table.xml`, and the entry named **`jbr-21`** points at
> `$APPLICATION_HOME_DIR$/jbr` — the bundled runtime, *whatever version it happens to be after
> the last IDE update*. On Android Studio 2026.1.3 that entry is still named `jbr-21`, and even
> records `JetBrains Runtime 21.0.10` as its version, while the runtime actually behind it is
> 25.0.2. The one-click **"Use JVM 21"** button in the sync dialog picks that entry, so the sync
> fails again with exactly the same error. Register a real JDK 21 via *Add JDK…*, or check what
> is behind an entry before trusting its name.

The most reliable option is the one this repository already uses: the Gradle JDK is set to
`#GRADLE_LOCAL_JAVA_HOME`, which reads `java.home` from `.gradle/config.properties` — a file
that is git-ignored. Setting the path there keeps the choice out of version control entirely:

```properties
# .gradle/config.properties  (git-ignored)
java.home=/path/to/a/real/jdk-21
```

> **Do not commit that change.** `.idea/gradle.xml` is tracked in this repository. Depending
> on how the JDK is selected, Android Studio writes the choice either into the ignored
> `.gradle/config.properties` (when the entry resolves to `#GRADLE_LOCAL_JAVA_HOME`) or
> directly into the tracked `.idea/gradle.xml` — and a committed absolute JDK path is a path
> that is wrong on everybody else's machine. Check `git status` after changing it, and leave
> any `.idea/` modifications out of your commits. The same applies to the other `.idea/` files
> Studio rewrites on first open.

Nothing about this is specific to macOS or to Android Studio; a CI job needs the same single
version pinned.

## 7. 2.0 backlog

Things that were noticed while doing this stage and deliberately not done.

### Must be done before the app can target 36

1. **Real window insets handling.** `android:windowOptOutEdgeToEdgeEnforcement` is deprecated
   and Google has said it will stop working. Delete `app/src/main/res/values-v35/` and lay out
   all 15 XML screens against `WindowInsets` instead. This is the single reason this stage
   stopped at 35.
2. **Stop relying on `Window.setStatusBarColor`** in `PrimaryActionModeController` — it is
   deprecated and only still works because of the opt-out above.
3. **Widen the supported JDK range.** The build is verified on exactly one Java version
   (section 6), which is brittle: it breaks the moment a contributor, a CI image or Android
   Studio's bundled runtime moves on. Raising the ceiling means a newer Gradle, and eventually a
   newer Android Gradle plugin — they move together, not separately. A newer Kotlin is part of
   it too: kapt is what actually failed on the newer JVM here, so moving to KSP removes that
   particular constraint entirely.
4. **Expect the IDE to keep pushing the Gradle floor up.** The wrapper had to move from 8.7 to
   8.8 purely because Android Studio injects an init script using a Gradle API that 8.7 lacks
   (section 3). That pressure is continuous — each IDE release may assume a newer Gradle than
   the one pinned here — and there is no CI to catch it. Whoever owns 2.0 should expect to track
   Gradle more actively than a keep-alive release does, and should add CI that builds on a
   pinned JDK so this class of breakage is caught by a machine rather than by opening the IDE.

### Dependencies that were tempting and skipped

| Dependency | Now | Note |
|---|---|---|
| `eu.davidea:flipview` | 1.2.0 | **Highest priority.** In March 2026 the same version number was republished on Maven Central as a completely different artifact — different bytecode level, new `minCompileSdk`, new transitive dependency. A version coordinate that can change under you is a supply chain hazard. It is two Java files; vendor them, or replace the flipping icon with something maintained. |
| `androidx.lifecycle` | declared 2.2.0-rc03, resolves to 2.6.1 | Declare what actually resolves, and drop `lifecycle-extensions` — it is deprecated, and nothing in this app imports anything from it. |
| `android.arch.navigation` | 1.0.0 | Still the pre-AndroidX group name, only working because Jetifier rewrites it to `androidx.navigation:2.0.0`. Moving to a current `androidx.navigation` is what would finally allow `android.enableJetifier=false`. |
| `com.google.dagger` | 2.23.2 | Works on the new toolchain, but `dagger-android` is deprecated upstream. Hilt is the migration target. |
| `androidx.room` | 2.2.3 | Also set `exportSchema = true`, commit the schema JSON, and add a real migration test against an old database file — today the schema is unversioned and `fallbackToDestructiveMigration()` is a loaded gun. |
| `com.github.bumptech.glide` | 4.11.0 | Fine today; the move off kapt to KSP is the reason to upgrade. |
| `androidx.constraintlayout` | 2.0.0-beta4 | A beta from 2019 is shipping in production. |
| `androidx.recyclerview` | 1.2.0-alpha01 | Same, an alpha. |
| `androidx.test:runner` / `rules` / `espresso` | `*-alpha03` | Same, alphas. |
| `kotlinx-coroutines` | 1.0.1 | Very old; the API has moved on considerably. |
| `com.facebook.stetho` | 1.5.1 | Archived upstream. It is also a debug-only tool shipped in the release build, which it should not be. |
| Kotlin | 1.9.25 | 2.x, together with the K2 compiler and KSP instead of kapt. |
| `minSdk` | 21 | Android 5 is below Play's own minimum for new installs on most devices now. |
| kapt | in use | `KaptUsageInsteadOfKsp` is one of the Lint warnings. |
| `android.nonTransitiveRClass` / `nonFinalResIds` | both `false` | Pinned to the old defaults on purpose here, so the R class behaves exactly as it did. Flipping them is a 2.0 cleanup. |
| View binding | none | The synthetics removal in this branch used `findViewById` because it is the smallest, most obviously-correct change. Proper view binding (or Compose) is the 2.0 answer. |

### Bugs and facts found while working, not fixed here

1. **`SettingsFragment.kt:226` — real bug.** The feedback email subject is built as

   ```kotlin
   val subject = ...getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME
           " " + ...getString(R.string.app_subtitle)
   ```

   The second line is a dangling expression that is computed and thrown away, so the subject
   silently loses the app subtitle. It has been in the code since at least 1.2.0. Fixing it
   changes user visible behaviour, which is out of scope for a keep-alive release, but it is a
   one line fix whenever it is wanted.

2. **The demo flavor can no longer fetch anything.** `sandbox-api.coinmarketcap.com` does not
   resolve any more — CoinMarketCap retired the sandbox. The demo build launches, navigates and
   renders its error state correctly, but every screen that needs data shows "Unable to get
   data". This has nothing to do with this branch; it is simply true of the demo flavor today,
   and it should be decided in 2.0 whether the demo flavor is retired, pointed at a bundled
   fixture, or pointed somewhere else.

3. **`app/src/full/.../ConstantsFlavor.kt` ships an empty API key**, so a clean checkout of the
   full flavor also shows "Unable to get data" until a key is supplied. That is intentional for
   an open-source repository; it just means the full flavor cannot be smoke tested from a clean
   clone either.

4. **JCenter's redirect is what changed `flipview` underneath this project.** Any other
   dependency resolved through that redirect could change the same way. Pinning with a
   dependency lock file, or at minimum verification metadata, belongs on the 2.0 list.
