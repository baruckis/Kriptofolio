# ADR-005 — Koin for dependency injection

- **Status:** accepted
- **Date:** 2026-10-21
- **Supersedes:** the note "Hilt is the migration target" in `UPGRADE-NOTES.md` section 8

## Context

The 1.x app uses Dagger 2.23 with `dagger-android`, processed by kapt. The modernization moves
logic into Kotlin Multiplatform modules and ViewModels into shared code (ADR-003, ADR-004), and
2.0.0 adds iOS. Hilt, the usual Android successor to Dagger, works only in Android code and does
not support Kotlin Multiplatform; choosing it would mean a second migration once ViewModels move
to shared code. Two multiplatform options were compared:

- **Koin**: a pure-Kotlin library with first-class Kotlin Multiplatform support, a Compose
  Multiplatform ViewModel integration and the largest community among KMP projects. The graph is
  resolved at run time.
- **Metro**: a Kotlin compiler plugin with a Dagger-like graph checked at compile time and a Dagger
  interop mode. Its plugin must be upgraded in step with the Kotlin compiler, and its community is
  smaller.

## Decision

1. Koin, declared with its Kotlin DSL (no annotation processing), is the dependency injection for
   `app/` and `shared/`. ViewModels are obtained with the Koin Compose ViewModel integration in
   shared code.
2. A unit test runs Koin's module verification for every module in CI, so a missing definition
   fails before merge instead of at run time.
3. The migration is incremental. Koin starts beside Dagger; objects still provided by Dagger are
   bridged into Koin with single definitions that delegate to the Dagger component, and screens
   move one at a time. Dagger, `dagger-android` and the last kapt processor are removed when the
   last screen has moved. A spike proves the bridge on one screen before the migration starts.

## What we do not do

- No Hilt, not even as an intermediate step.
- No service-locator calls from inside business logic; dependencies arrive through constructors,
  and only the platform entry points and screen wrappers ask Koin for objects.
- No Koin annotations or code generation unless a later decision records why.

## Consequences

- Build time no longer includes kapt for dependency injection.
- The safety that Dagger gave at compile time is replaced by the verification test, which must
  stay green and cover every module.
- The step is a worked example for the many legacy apps that use Dagger or Hilt today.

## Reopen when

The verification test cannot cover a real case, Koin's multiplatform support regresses, or Hilt
gains Kotlin Multiplatform support that would make it the simpler choice.
