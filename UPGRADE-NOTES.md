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
