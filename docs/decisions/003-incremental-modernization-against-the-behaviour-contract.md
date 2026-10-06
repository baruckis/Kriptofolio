# ADR-003 — Incremental modernization against the behaviour contract

- **Status:** accepted
- **Date:** 2026-10-06 (replaces an earlier draft that proposed a rewrite beside the old code)

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
2. Target stack, reached step by step: Kotlin 2.x with KSP, Hilt, Jetpack Compose, Room with an
   exported schema, Ktor client with kotlinx.serialization, a version catalog and dependency
   verification metadata, JDK 21 toolchain. Where the structure matches, it follows the public
   `baruckis/ai-news` app.
3. Shared logic is extracted into Kotlin Multiplatform modules under `shared/` (model, data with
   Room KMP, network and market-data sources), consumed by `app/` as soon as each module exists.
   Screens move from XML to Compose one at a time; when all are Compose they move into a Compose
   Multiplatform module and an iOS shell (`iosApp/`) is added. Android and iOS ship together at
   the 2.0.0 release.
4. `applicationId com.baruckis.kriptofolio` and the signing key never change. The Android product
   flavors become `play` and `foss`; the legacy `demo` flavor is retired by this decision, and a
   fixtures mode in debug builds replaces it. A `foss` build signed by F-Droid cannot upgrade a
   Play install in place, which the F-Droid page states.
5. The v1 database must open after every step. Any schema change ships with a migration and a test
   against the real files in `app/src/test/resources/db/`. There is no destructive fallback.
6. **Merged is not released.** A step is merged to `master` when it is reviewed and green.
   Releases are separate, owner-only actions (version bump, tag, signing, store upload) planned in
   `docs/roadmap.md`; `master` must always be releasable, but most merges are not released.
7. Parity is measured against `BEHAVIOUR.md` and `ui-inventory.md`; deliberate deviations are
   recorded in `docs/decisions/` and mapped in `docs/behaviour-parity.md`.

## What we do not do

- No second code base beside `app/` and no big-bang switch.
- No desktop or web target before the stage 7 review.
- No changes to `Part-1`–`Part-5`, `legacy` or the tag `v1.2.1-legacy`.
- No rules copied into tool-specific files; `AGENTS.md` remains the single entry point.

## Consequences

- The minimal-diff rule of `AGENTS.md` continues to govern keep-alive pull requests; steps listed
  in the roadmap follow the "Modernization track" section instead.
- `UPGRADE-NOTES.md` section 8 is resolved by the steps, not by unrelated patches.
- CI keeps the current gate green on every step and adds jobs as modules appear: shared tests
  first, then an iOS build on a macOS runner (free for public repositories).
- iOS appears later than Android's first modernized releases, because the iOS shell is built once
  the logic and screens are shared. The owner accepts the Apple Developer Program fee as the one
  recurring cost of the product.
- Each step becomes a worked example for the AI-First Android Factory method, reproduced from a
  clean checkout at the reviewed revision.

## Reopen when

A step proves impossible without breaking the contract (for example a Room KMP limitation on
opening the v1 file), in which case the affected step is redesigned in a new record.
