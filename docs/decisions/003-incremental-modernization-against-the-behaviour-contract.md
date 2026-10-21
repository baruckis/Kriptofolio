# ADR-003 — Incremental modernization against the behaviour contract

- **Status:** accepted
- **Date:** 2026-10-06 (replaces an earlier draft that proposed a rewrite beside the old code),
  revised 2026-10-21 (Koin instead of Hilt, Compose Multiplatform from stage 4, two releases)

## Context

Stage 1 produced a behaviour contract (`BEHAVIOUR.md`), a public UI inventory and characterization
tests that run against real v1 database files. The 2019 code base carries years-old dependencies,
a build that works on exactly one JDK and patterns the project no longer wants to teach. This
repository is also the public worked example of a method: a legacy Android app modernized in
bounded, evidenced, reviewed steps, with the app working after every step. A rewrite beside the old
code would contradict that, and would discard the safety net stage 1 built.

## Decision

1. The app is modernized in place, in this repository, one bounded step at a time. Every step is
   a pull request that keeps the app working, preserves the behaviour in `BEHAVIOUR.md` unless a
   decision record says otherwise, and carries evidence from a clean checkout.
2. Target stack, reached step by step: Kotlin 2.x with KSP; Koin for dependency injection
   (ADR-005); Room KMP with an exported schema; Ktor client with kotlinx.serialization; DataStore
   for settings; Coil 3 for images; Compose Multiplatform with MVVM (ADR-004); a version catalog
   and dependency verification metadata; a JDK 21 toolchain. Its module layout (`app`, `core`,
   `feature`) and version catalog follow the public `baruckis/ai-news` app; its MVI presentation
   layer does not (ADR-004). The navigation library is chosen in the stage 4
   spike and recorded in a new decision.
3. Shared logic is extracted into Kotlin Multiplatform modules under `shared/` (model, data with
   Room KMP, network and market-data sources), consumed by `app/` as soon as each module exists.
   Screens move from XML to Compose one at a time, written from the start in a Compose
   Multiplatform module under `shared/` that targets only Android until stage 6, with their ViewModels in shared
   code. Stage 6 adds the iOS targets and the iOS shell (`iosApp/`).
4. The modernization of the existing app ships as **1.3.0** (Android). The new product features,
   iOS, Pro and the F-Droid build ship together as **2.0.0** (ADR-006).
5. `applicationId com.baruckis.kriptofolio` and the signing key never change. The Android product
   flavors stay `full` and `demo` through 1.3.0 and become `play` and `foss` in 2.0.0; the legacy
   `demo` flavor is then retired, and a fixtures mode in debug builds replaces it. A `foss` build
   signed by F-Droid cannot upgrade a Play install in place, which the F-Droid page states.
6. The v1 database must open after every step. Any schema change ships with a migration and a test
   against the real files in `app/src/test/resources/db/`. There is no destructive fallback.
7. **Merged is not released.** A step is merged to `master` when it is reviewed and green.
   Releases are separate, owner-only actions (version bump, tag, signing, store upload) planned in
   `docs/roadmap.md`; `master` must always build and pass its tests, but most merges are not
   released.
8. Parity is measured against `BEHAVIOUR.md` and `ui-inventory.md`; deliberate deviations are
   recorded in `docs/decisions/` and mapped in `docs/behaviour-parity.md`.
9. **One deliberate deviation in 1.3.0: the licenses screens.** Google's `OssLicensesMenuActivity`
   and the hand-written list of 28 libraries (`BEHAVIOUR.md` section 8) are replaced by one
   screen that AboutLibraries generates at build time, so the app no longer needs a Google Play
   Services library for it. The app's own Apache 2.0 notice stays. The parity table records it.

## What we do not do

- No second code base beside `app/`, `shared/` (which also holds the Compose Multiplatform UI)
  and, from stage 6, the `iosApp/` shell; no big-bang switch.
- No desktop or web target before the review after 2.0.0.
- No changes to `Part-1`–`Part-5`, `legacy` or the tag `v1.2.1-legacy`.
- No rules copied into tool-specific files; `AGENTS.md` remains the single entry point.

## Consequences

- The minimal-diff rule of `AGENTS.md` continues to govern keep-alive pull requests; steps listed
  in the roadmap follow the "Modernization track" section instead.
- `UPGRADE-NOTES.md` section 8 is resolved by the steps, not by unrelated patches.
- CI keeps the current gate green on every step and adds jobs as modules appear: shared tests
  first, then an iOS build on a macOS runner (free for public repositories).
- Because the screens are Compose Multiplatform from the first one, stage 6 adds targets instead
  of moving screens. The stage 4 spike must first prove that Compose Multiplatform resources keep
  the four locales, `values-iw`, right-to-left layout and the Android per-app language setting
  working; if it does not, stage 4 uses Android Compose and the screens move in stage 6.
- The owner accepts the Apple Developer Program fee as a new recurring cost of the product; the
  starter feed of ADR-001 runs on a server the author already pays for.
- Each step becomes a worked example for the AI-First Android Factory method, reproduced from a
  clean checkout at the reviewed revision.

## Reopen when

A step proves impossible without breaking the contract (for example a Room KMP limitation on
opening the v1 file, or the stage 4 spike failing), in which case the affected step is redesigned
in a new record.
