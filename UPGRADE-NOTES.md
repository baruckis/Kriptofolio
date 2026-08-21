# UPGRADE-NOTES.md — the keep-alive upgrades

Working notes for the smallest change sets that keep Kriptofolio releasable on Google Play.

* **Stage 0** (sections 1-8) — compileSdk / targetSdk **35**, released as **1.2.2**, versionCode 5.
* **Stage 0.5** (section 9) — compileSdk / targetSdk **36**, prepared as **1.2.3**, versionCode 6.

Sections 6 and 7 - how to build, and how to cut a release - apply to both and are kept current.

---

## Stage 0 scope

The smallest change set that made the app releasable again before Google Play stopped showing
apps that target API 34 or lower to new users (2026-08-31).

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

## 7. Cutting a release

Everything above describes how to *build* this branch. This section is what has to happen to
turn that build into a release, in order. It exists because two of these steps are invisible in
the repository — nothing in the source tree reminds you about them, and the gap between releases
here is measured in years.

### 1. The API key is not in this repository, and must go in by hand

`app/src/full/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt` ships with

```kotlin
const val API_SERVICE_AUTHENTICATION_KEY = "" // TODO: put your CoinMarketCap API key here
```

That empty string is deliberate and correct — the key is a secret and this is a public
repository. It also means **a release built straight from a clean checkout is broken**: it
installs, it launches, it passes review, and every screen that needs data shows "Unable to get
data". There is no build error and no warning; the app is simply useless.

So, before building a release:

1. paste the real CoinMarketCap key into that constant;
2. build and sign;
3. **put the empty string back** and confirm with `git status` that nothing is staged.

The failure this guards against is silent in both directions — forget step 1 and you ship a dead
app, forget step 3 and you publish your key permanently into a public git history. A local
`pre-commit` hook that refuses to commit a non-empty key is a cheap safety net; `.git/hooks/` is
never pushed, so it costs nothing and protects only the person who installs it.

### 2. Add the changelog for the new versionCode

`fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`, for **every** locale directory
that exists (currently `en-US` and `lt-LT`). F-Droid and its downstream repositories read these,
and a missing file means no release notes for those users.

This is not hypothetical: issue #4 was open from 2020 to 2026 for exactly this reason — the file
for versionCode 3 was never added, and nobody noticed until someone outside the project pointed
it out.

### 3. Sign with the same key as the previous release

Verify before uploading, rather than discovering it afterwards:

```bash
apksigner verify --print-certs <new>.apk        | grep "SHA-256 digest"
apksigner verify --print-certs <previous>.apk   | grep "SHA-256 digest"
```

The two digests must be identical. A mismatch cannot be fixed later — Google Play will not accept
an update signed with a different key, and existing installs cannot be upgraded.

### 4. Verify the upgrade path on a device before uploading

Install the **previously published** artifact, create real data with it, then install the new
build over it. Turn networking off first: with no network, whatever is on screen afterwards can
only have come from the existing database, so a wiped database cannot hide behind a fast refetch.

This matters more here than in most apps: portfolios exist only on the device, there is no cloud
backup and no export, and `fallbackToDestructiveMigration()` is enabled — so a schema mismatch
does not fail loudly, it deletes.

### 5. Upload, then confirm rather than assume

Use a staged rollout. The only proof that a compliance requirement has been met is the warning
disappearing from the Play Console — not the value in the manifest, and not a successful upload.

### 6. Tag and publish the source release

Tag the merge commit and create the matching GitHub release, keeping the naming used by the
existing tags.

### A note on the demo flavor

`sandbox-api.coinmarketcap.com` no longer resolves — CoinMarketCap retired the sandbox that the
demo flavor was built against. The demo build still compiles, launches and navigates, but it can
never fetch anything.

Publishing a demo build that cannot work is worse than publishing none: someone downloads it,
sees an error state, and concludes the app is broken. **The demo flavor is therefore not being
published for 1.2.2.** It must still build — that is a review rule, and it is how the app is kept
honest about supporting both flavors — but it is not shipped until it has a data source again.
Giving it bundled offline fixture data is on the 2.0 list, and would make it a better demo than
it ever was.

## 8. 2.0 backlog

Things that were noticed while doing this stage and deliberately not done.

### Must be done before the app can target 36

> **Done in Stage 0.5 (section 9).** Items 1 and 2 below are the work that stage did. They are
> left here unedited, as they were written, because what they got right and what they missed is
> part of the record - the list said two things had to happen and there turned out to be three.

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
4. **Promote section 7 to a real `RELEASING.md`.** The release procedure currently lives inside
   a document about one specific upgrade, which is the wrong home for it — it will keep being
   true long after this stage is history. Moving it out, and having CI check the parts a machine
   can check (is the API key empty on `master`? does a changelog exist for the current
   versionCode?), turns three of those steps from discipline into automation.
5. **Expect the IDE to keep pushing the Gradle floor up.** The wrapper had to move from 8.7 to
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

---

## 9. Stage 0.5 — targetSdk 36 (Android 16)

Prepared on **2026-08-21**, branched from `master` at `2ec3b24`, branch `stage0.5/target-sdk-36`,
released as **1.2.3**, versionCode 6.

### 9.1 Why this stage exists, and why the deadline is not what it looks like

Google Play's policy page carries three target-API entries, not one, and they are different
numbers with different consequences:

| | Requirement | Consequence | From |
|---|---|---|---|
| 🔴 enforced | targetSdk ≥ **35** | app updates are rejected | 2025-08-31 |
| 🟡 warning | targetSdk ≥ **35** | app is not available to new users on newer Android | 2026-08-31 |
| 🟡 warning | targetSdk ≥ **36** | app updates are rejected | 2026-08-31 |

1.2.2 satisfies both API-35 rows. The only thing at stake is the third: **from 2026-08-31 no
update of any kind can be submitted until the binary targets 36.** Nothing is removed, unlisted
or suspended, existing users are unaffected, and new users keep installing it on every Android
version. A targetSdk 36 build submitted after the date is accepted normally, with no penalty and
no reinstatement step.

So this stage is not a race. The cost of being late is precisely that there is no path to ship a
hotfix while the gate is closed.

Two other dated requirements were found on the same page and are recorded here so the next
person does not rediscover them: **Play Console app registration by 2026-09-30**, whose stated
penalty is global removal (verified done for this app on 2026-08-21), and **16 KB page size
support by 2027-02-01** for apps targeting 35 and above, which this app satisfies with nothing to
do because it ships no native libraries. Android 17 (API 37) was released on 2026-06-16 and the
Android Developers Blog has already announced API 37 as required for Play distribution in August
2027; the canonical requirements page does not mention it yet.

### 9.2 What actually breaks at targetSdk 36, measured before any code was written

Android ships a compatibility framework that gates behaviour changes on the declared target, and
it exposes exactly the switch this stage needed:

```
ChangeId(309578419; name=ENFORCE_EDGE_TO_EDGE;         enableSinceTargetSdk=35)
ChangeId(377864165; name=DISABLE_OPT_OUT_EDGE_TO_EDGE; enableSinceTargetSdk=36)

adb shell am compat enable DISABLE_OPT_OUT_EDGE_TO_EDGE com.baruckis.kriptofolio.demo
```

Enabling the second one on a **demo** debug build - installed beside the production app, so the
real install and its database were never touched - reproduces target 36 behaviour with no change
to the repository at all. On an Android 16 emulator:

| Screen | What happened |
|---|---|
| main (`AppTheme.NoActionBar`) | the toolbar drew under the status bar; the title overlapped the clock, the overflow button sat under the battery icon |
| add / search (`AppTheme.NoActionBar`) | the same |
| settings (decor ActionBar) | the action bar was positioned correctly by AppCompat's `ActionBarOverlayLayout`, but the strip above it was painted `#FAFAFA` while the status bar icons stayed white - an effectively invisible status bar |
| bottom, gesture navigation | content drew behind the transparent bar; the FAB sat in the gesture area |
| bottom, 3-button navigation | the FAB was cut in half by the system's translucent scrim |

That last distinction mattered: a static analysis of AppCompat's own bytecode had predicted a
colour problem rather than a hidden-content problem, and it was right for exactly one screen out
of three. Ten minutes on an emulator settled what no amount of reading would have.

### 9.3 The three problems, not one

Section 8 listed two things that had to be done before the app could target 36. There were three.

1. **Real window insets handling.** `android:windowOptOutEdgeToEdgeEnforcement` is disabled at
   target 36, and its removal is silent - no error, no lint warning, no log line.
2. **`Window.setStatusBarColor` is a no-op.** `PrimaryActionModeController` used it to turn the
   status bar black while coins are selected. Play's own pre-release analysis of the 1.2.2 bundle
   named the two methods and the file.
3. **Predictive back.** Section 5 recorded this as "does not apply", reasoning that the app has no
   `onBackPressed()` override anywhere. True, and incomplete: the app does not use back, but
   **AppCompat does** - it is how the contextual action mode is cancelled and how an expanded
   `SearchView` is collapsed. `strings classes*.dex | grep -i onbackinvoked` on the shipped APK
   returns nothing, because appcompat 1.1.0 and activity 1.1.0-rc01 both predate the 1.6.0
   releases that added `OnBackInvokedCallback`. Measured at targetSdk 36: selecting a coin and
   pressing back left `topResumedActivity=NexusLauncherActivity`. Back closed the app instead of
   cancelling the selection.

The lesson worth keeping: *"we never call that API"* is not the same as *"nothing we depend on
calls that API"*.

### 9.4 The approach

Two strategies were possible: keep the app's current appearance, or go properly edge-to-edge.
This is a compliance release, so the appearance is kept.

The design decision that makes it cheap is what is **not** done:
`WindowCompat.setDecorFitsSystemWindows(window, false)` is never called. On Android 14 and below
the decor view still consumes the system window insets itself, so the listener installed on each
activity's content view is handed zeros, applies zero padding and leaves the bar views at zero
height - those versions are laid out exactly as before. On Android 15 and above the platform
forces edge-to-edge and the same listener receives real insets and puts them back as padding.
**One code path from API 21 to 36, with no `Build.VERSION` branch anywhere.**

Because the padding goes on the content root, every child keeps its existing arithmetic: the
RecyclerView's 72dp bottom padding still clears the FAB and nothing else, the FAB's 16dp margin is
still measured from where content ends, and both `SwipeRefreshLayout`s still start below the app
bar. **No layout below an activity root needed a change.**

Four views, included from `layout/system_bar_backgrounds.xml`, paint the areas the platform used
to paint from the theme. The left and right ones are declared last so they draw over the ends of
the top and bottom ones, which is what reproduces the black cutout column with the coloured status
strip starting beside it. Their gravity is `left`/`right` rather than `start`/`end` on purpose:
window insets are physical, and must not mirror in Hebrew.

`WindowInsetsCompat.Type.systemBars()` is asked for together with `displayCutout()`, because the
pre-Android-15 window avoided the cutout too.

### 9.5 What changed, commit by commit

| Commit | What | Build after it |
|---|---|---|
| `chore: Raised androidx.core to 1.16.0.` | the only dependency change | green |
| `chore: Raised compileSdk to 36, keeping the Android Gradle plugin at 8.6.0.` | compileSdk + one property | green |
| `feat: Handled window insets so the app keeps its layout when edge to edge is enforced.` | `BaseActivity`, 2 activity roots, the bar views | green, and visually identical at targetSdk 35 |
| `fix: Gave the settings screen its own toolbar so it can paint its status bar area.` | `activity_settings.xml`, `SettingsActivity`, manifest theme | green |
| `refactor: Recoloured the action mode status bar area without Window.setStatusBarColor.` | `PrimaryActionModeController` | green |
| `fix: Opted out of predictive back until the back handling is migrated.` | one manifest attribute | green |
| `chore: Raised targetSdk to 36 and removed the Android 15 edge to edge opt out.` | the point of the stage; `values-v35/` deleted | green |
| `chore: Annotated the predictive back opt-out with the API level it needs.` | `tools:targetApi` | green |
| `fix: Painted the display cutout area, which landscape on a notched device exposed.` | 2 more bar views | green |
| `chore: Bumped versionCode to 6 and versionName to 1.2.3.` | release version | green |
| `docs: Added the fastlane release changelogs for versionCode 6.` | `en-US`, `lt-LT` | green |

The order is deliberate. Every fix landed and was verified **before** `targetSdk` moved, so a
visual regression could only have come from the change being tested at that moment.

### 9.6 Dependency and toolchain changes

| Item | Old | New | Why |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.2.0-rc01 | **1.16.0** | 1.2.0 has no `WindowInsetsCompat.Type` and no `getInsets(int)`. This is not a convenience: on API 30+ the platform maps the deprecated `getSystemWindowInsetBottom()` to `systemBars()` **plus** `ime()`, so a root padding listener written with it would grow by the keyboard height whenever the keyboard opened. `Type.systemBars()` first exists in core 1.5.0; 1.16.0 is the newest core whose AAR metadata still fits this toolchain exactly (`minCompileSdk 35`, `minAndroidGradlePluginVersion 8.6.0`). 1.17.0 demands AGP 8.9.1 and drags `kotlin-stdlib 2.0.21` into a Kotlin 1.9.25 project. |
| `compileSdk` | 35 | **36** | required by `targetSdk 36` |
| `targetSdk` | 35 | **36** | the point of the stage |
| Android Gradle plugin | 8.6.0 | **8.6.0** | unchanged — see below |
| Gradle wrapper | 8.8 | **8.8** | unchanged |
| Kotlin | 1.9.25 | **1.9.25** | unchanged |
| JDK | 21 | **21** | unchanged |
| `minSdk` | 21 | **21** | unchanged |

Resolved after the core bump, confirmed from the dependency graph: `appcompat 1.1.0`,
`material 1.0.0`, `fragment 1.2.0-rc01`, `activity 1.1.0-rc01` — **all unchanged**.

#### Why the Android Gradle plugin did not move, which is the surprise of this stage

AGP 8.10.0 is the oldest plugin whose release notes state a maximum API level of 36, so it was the
obvious choice. It does not work here.

**Every AGP from 8.9 onward ships `androidx.databinding:databinding-ktx` built with Kotlin 2.1.0,
and places a `strictly 2.1.0` constraint on `kotlin-stdlib` on the application's own compile
classpath.** Kotlin 1.9.25's compiler reads metadata up to 2.0.0. The result is hundreds of
errors, starting with the standard library:

```
e: jetified-kotlin-stdlib-2.1.0.jar!/META-INF/kotlin-stdlib.kotlin_module
   Module was compiled with an incompatible version of Kotlin.
   The binary version of its metadata is 2.1.0, expected version is 1.9.0.
e: Converters.kt:31:23 Unresolved reference: let
```

Observed with 8.10.0 and with 8.9.0, so it is the plugin generation, not one bad release. Data
binding is what pulls `databinding-ktx` in, and this project cannot drop data binding.

Three doors were built and measured rather than argued about. All three build green and pass both
unit-test suites:

| Door | Toolchain | Lint result |
|---|---|---|
| **1 — chosen** | AGP 8.6.0, Gradle 8.8, Kotlin 1.9.25, `android.suppressUnsupportedCompileSdk=36` | 2 errors, 52 warnings, 1 info — **byte identical to the recorded baseline** |
| 2 | AGP 8.10.0, Gradle 8.11.1, Kotlin 1.9.25, `-Xskip-metadata-version-check` | 2 errors, 66 warnings, 1 hint |
| 3 | AGP 8.10.0, Gradle 8.11.1, Kotlin 2.2.10 | 2 errors, 65 warnings, 1 hint |

Door 1 was chosen because it is the only one that changes nothing this project can measure:
running lint under AGP 8.6.0 at compileSdk 35 and again at compileSdk 36 produces an identical
issue set, id by id and count by count. Door 2 asks the Kotlin compiler to stop checking metadata
versions across the entire dependency graph — the check that caught this problem in the first
place. Door 3 changes the compiler to K2 for the whole application inside a compliance release; it
needed no source change, which is encouraging and is recorded in the 2.0 backlog, but a behaviour
difference would surface at runtime rather than at build time.

What door 1 costs is exactly what its property says: **AGP 8.6.0 was not tested by Google against
compileSdk 36.** The exposure is small and checkable — the app calls no API 36 method, ships no
native code, and every screen was exercised on an Android 16 emulator — but it is real, and the
property is written into `gradle.properties` with a comment rather than hidden.

### 9.7 Verification

All commands run with `JAVA_HOME` pointing at Temurin 21 (section 6).

| Check | Result |
|---|---|
| `assembleFullDebug assembleDemoDebug` | pass |
| `assembleFullRelease assembleDemoRelease` | pass (unsigned) |
| `bundleFullRelease bundleDemoRelease` | pass |
| `testFullDebugUnitTest` | pass — 14 tests, 0 failures |
| `testDemoDebugUnitTest` | pass — 14 tests, 0 failures |
| `lintFullDebug` | 2 errors, 52 warnings, 1 informational — **the same counts as the Stage 0 baseline** |
| Native `.so` libraries in either release bundle | **none** |
| Shipped manifest | `minSdkVersion 21`, `targetSdkVersion 36`, `compileSdkVersion 36`, `versionCode 6`, `versionName 1.2.3` |
| Room identity hash in the generated code | `ad1c80913f23361aa985d56ecf84d645` — **unchanged** from v1.2.1 and v1.2.2 |
| `ConstantsFlavor.kt` API key on the branch | empty string, as it must be |

#### Lint, compared exactly

Two warnings changed identity and the totals did not move:

```
MergeRootFrame   1 -> 2   the FrameLayout activity_main.xml now uses to hold the bar views
Overdraw         1 -> 0   that same opaque wrapper removes the overdraw lint used to see
```

`UnusedAttribute` appeared once for `android:enableOnBackInvokedCallback`, which exists from API
33 while `minSdk` is 21, and was answered with `tools:targetApi` rather than silenced.

#### Measured on an Android 16 emulator (API 36, arm64), not eyeballed

The published v1.2.2 APK and this branch were compared by sampling the same pixels in the same
screens. Status bar area, portrait, with a real portfolio in the database:

| | published v1.2.2 | this branch |
|---|---|---|
| main screen | `#00796b` | `#00796b` |
| contextual action mode | `#000000` | `#000000` |
| after leaving the action mode | `#00796b` | `#00796b` |
| target 36 before this branch | — | `#009688` (the toolbar showing through) |

Hebrew, landscape, with the corner display-cutout overlay enabled:

| | published v1.2.2 | this branch |
|---|---|---|
| cutout column | `#000000` | `#000000` |
| cutout column, top corner | `#000000` | `#000000` |
| status bar strip | `#00796b` | `#00796b` |
| navigation bar | `#ebebeb` | `#ebebeb` |
| right edge | `#009688` | `#009688` |

Also exercised at real targetSdk 36, with no crash and no logcat exception: the main list with
data, the add/search screen, settings, the third-party software list, a full licence page, the
donate dialog, the contextual action mode including its dismissal, Hebrew with full RTL mirroring,
rotation to landscape and back, and both gesture and 3-button navigation.

Back navigation was checked directly, because it is the one thing that cannot be simulated with a
compatibility flag: selecting a coin and pressing back at targetSdk 36 without the opt-out left
the launcher on screen; with the opt-out it cancels the action mode and restores `#00796b`.

Not reachable without an API key and left for manual testing on a signed build: fetching live
data, pull-to-refresh, and adding a coin. To make the visual checks above possible at all, a
**Room v1 database was constructed by hand** from the schema and identity hash in the generated
`AppDatabase_Impl.java` and pushed into the *demo* app with `run-as`. That the app opened it
without a destructive fallback is itself evidence that the schema has not moved.

#### The one difference from v1.2.2 that could not be removed

In **3-button navigation** the navigation bar is drawn by the system as a light bar with dark
icons, where v1.2.2 had a black bar with white icons. This is not a choice this app can make any
more: setting `WindowInsetsControllerCompat.isAppearanceLightNavigationBars` to `true` and to
`false` produces byte-identical screenshots on Android 16, so the platform ignores it. Since the
icon colour cannot be controlled, the background is left to the system too, which at least pairs
them correctly. In **gesture navigation**, which is the default, the bar is black exactly as
before.

### 9.8 Still deferred after this stage

* **Migrate back handling to `OnBackPressedDispatcher`** and drop
  `android:enableOnBackInvokedCallback="false"`. This needs appcompat 1.6.0+ and activity 1.6.0+,
  which is an AndroidX upgrade, not a compliance change. The opt-out is a stay of execution.
* **Move the Android Gradle plugin forward**, which now requires Kotlin 2.x first because of the
  `databinding-ktx` metadata problem in 9.6. Kotlin 2.2.10 was verified to build this project
  green with AGP 8.10.0 and Gradle 8.11.1 with **no source change**, so the migration is smaller
  than it looks — it just needs its own stage and its own verification.
* **API 37 / Android 17**, required for Play distribution around August 2027. Its headline change
  is enforced resizability and adaptive layouts on large screens, and the temporary opt-out
  property for that is documented as not working at target 37. This app declares no orientation,
  resizability or aspect-ratio restriction, so it has nothing to opt out of — but it also has no
  size or orientation resource qualifiers at all, and stretches one phone layout across a tablet.
* Everything else already listed in section 8.
