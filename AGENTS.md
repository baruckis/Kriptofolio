# AGENTS.md — Kriptofolio

Rules for every AI coding agent working in this repository, independent of provider or model. Read this file fully before doing anything.

## What this project is

Kriptofolio is a free, open-source, privacy-first cryptocurrency portfolio app for Android
(Google Play: `com.baruckis.kriptofolio`). It was built in 2018–2019 and is publicly documented,
step by step, in a well-known blog series (baruckis.com + freeCodeCamp). This repository is
therefore BOTH a living app AND a historical artifact. Treat it like a museum you are allowed
to renovate: carefully, reversibly, and without touching the exhibits.

## Protected — never touch

- Branches `Part-1`, `Part-2`, `Part-3`, `Part-4`, `Part-5` — frozen references for the blog
  series. Never commit to, rebase, rename, or delete them.
- Branch `legacy` and tag `v1.2.1-legacy` — frozen forever.
- Branch `modernization-2026`, once the owner creates it at the 1.3.0 release — frozen forever
  (`docs/decisions/006`).
- `README.md` and all documentation files — do not modify unless the task explicitly says so.
- Git history — NEVER use `push --force`, `rebase` on pushed branches, or history rewrites.
- Never push directly to `master`. All work goes through a feature branch and a pull request
  that the human reviews and merges.

## Branches

The branch list in this repository is documentation. Someone arriving from the 2018 article
series sees `Part-1`…`Part-5` and immediately understands where they have landed. That signal
only survives while the list stays short, so every branch here is one of three kinds.

**Exhibits — kept forever.** `Part-1`…`Part-5` contain commits that are not on `master` and
never will be; deleting one destroys history that the published articles link to. `legacy`
marks the state of the app before the modernization; `modernization-2026` will mark the state
after it, at the 1.3.0 release. These are the protected branches listed above.

**Maintenance branch — kept until 2.0.0 is in production.** `release/1.3`, created by the owner
from the tag `v1.3.0`, receives 1.3.x hotfix pull requests; each fix is cherry-picked to `master`.
Deleting it after 2.0.0 is the owner's decision.

**Working branches — deleted automatically once their pull request is merged.** Everything else: a feature,
a fix, a documentation change, a CI change. Nothing is lost by deleting one. Its commits stay
reachable from `master`, and the pull request keeps the full diff, the review and the
discussion — GitHub can restore the branch from the pull request page at any time. The
durable markers for a release are tags (`v1.2.1`, `v1.2.2`); the only branches tied to a release
are the exhibit `modernization-2026` and the maintenance branch `release/1.3` described above.

GitHub `delete_branch_on_merge` is enabled. It removes only the merged pull request's head
branch; it does not delete `master`, protected exhibit branches, tags, or commits in history. So:

- never manually delete a remote branch unless the owner explicitly asks;
- if automatic deletion did not occur, verify that the branch is an ancestor of `master`
  (`git merge-base --is-ancestor origin/<branch> origin/master`) before asking the owner whether
  to remove it;
- never delete, rename or rewrite `Part-1`…`Part-5`, `legacy`, or `master`.

## Project facts

- `applicationId com.baruckis.kriptofolio` (never change; Play listing and signing depend on it).
- Product flavors: `full` (real CoinMarketCap API) and `demo` (sandbox API, applicationIdSuffix `.demo`).
  Both must always build.
- Room database `version = 1`, entities `MyCryptocurrency` and `Cryptocurrency`,
  `exportSchema = true`. User portfolios live ONLY on-device (no cloud backup). Any change
  that could break opening an existing v1 database is forbidden without an explicit migration
  plus a test against a real old database file.
- Localization: default (en), `values-lt`, `values-iw` (Hebrew — RTL!), `values-sw-rKE`.
  All four must keep working; if a new user-visible string lacks any of the four translations,
  flag it to the human.
- Versions live in the version catalog `gradle/libs.versions.toml`.

## Working style

- Minimal-diff policy: change only what the current task requires. No reformatting, no renames,
  no "while I'm here" improvements, no code style modernization in existing files.
- Match the existing 2019-era code style of whatever file you touch.
- When a dependency upgrade is needed, upgrade to the OLDEST version that solves the problem.
  Every tempting-but-not-required upgrade goes into `UPGRADE-NOTES.md` as a note for the
  modernization steps in `docs/roadmap.md` instead of being done now.
- One logical step per commit. Commit messages follow this repository's established
  convention, based on the Udacity Git Commit Message Style Guide
  (https://udacity.github.io/git-styleguide/): `type: Capitalized summary.` with type one of
  feat / fix / docs / style / refactor / test / chore. A commit touching several aspects may
  chain sentences: `feat: Added X. fix: Corrected Y.` — see `git log` for live examples.
  This project teaches the Android community good practices; the standard is not optional.
- Run the relevant build/tests after every step; never stack changes on a red build.
- If something is ambiguous or destructive, stop and ask the human.

## Modernization track

`docs/roadmap.md` modernizes this app in place, one bounded, reviewed step at a time; the reasons are
in `docs/decisions/`. Stages 2–4 lead to release 1.3.0, stages 5–7 to release 2.0.0, whose scope is
frozen (`docs/decisions/006`). The keep-alive rules in "Working style" above govern any pull request
that is not a roadmap step; a roadmap step follows these rules instead.

- One step is one pull request with acceptance criteria, behaviour preserved as in
  `docs/BEHAVIOUR.md` unless a decision record says otherwise, and evidence from a clean checkout.
- Touch only what the step needs. New and rewritten code uses current conventions (Kotlin 2 with
  KSP, Koin, Compose Multiplatform with the MVVM rules of `docs/decisions/004`, Room KMP with an
  exported schema, Ktor client with kotlinx.serialization, DataStore, a version catalog); do not
  restyle files the step does not change.
- Shared logic and ViewModels live in `shared/` Kotlin Multiplatform modules, consumed by `app/`.
  Compose screens are written in a Compose Multiplatform module under `shared/` that targets only
  Android until stage 6, when the iOS targets and the iOS shell `iosApp/` arrive.
- Dependency injection is Koin (`docs/decisions/005`); every Koin module is covered by the module
  verification test. No Hilt.
- A feature that is not on the frozen 2.0.0 list goes to the backlog with the `later` label; do not
  implement it without a new decision record.
- `applicationId com.baruckis.kriptofolio` and the signing key never change. The database rule
  applies to every step: the real v1 files in `app/src/test/resources/db/` must open through a
  tested migration, with no destructive fallback.
- **Merged is not released.** A merge never publishes anything. Version bumps, tags, signing and
  store uploads are done by the human in the release PR and procedure (`UPGRADE-NOTES.md` section 7,
  later `RELEASING.md`). `master` must stay releasable: both flavors build, tests and migration
  tests pass.
- Flavors: `full` and `demo` through 1.3.0; stage 7 replaces them with `play` (Google Play Billing)
  and `foss` (no Google libraries, everything unlocked), as `docs/decisions/003` records. From then
  on, the rules that name `full` and `demo` ("Project facts" above and hard rules 3 and 4 of
  `config/review-rules.md`) apply to `play` and `foss`, and the `.demo` suffix retires with the
  `demo` flavor.
- No new provider key in the repository; the documented CoinMarketCap sandbox key of the `demo`
  flavor stays until that flavor retires (hard rule 6 of `config/review-rules.md`). Until 2.0.0 the
  owner supplies the 1.x key only to the release build; from stage 5 users supply their own key at
  runtime. Tests use recorded responses from `app/src/test/resources/api/` and new fixtures.
- Parity is measured against `docs/BEHAVIOUR.md` and `docs/ui-inventory.md`; deliberate deviations
  go in `docs/decisions/` and `docs/behaviour-parity.md`.
- Gate for a step: the build command below, plus once the modules exist
  `./gradlew :shared:allTests`, and for iOS (stage 6) the `xcodebuild` result of the iOS shell.

## Build commands

Use JDK 21 for this project; the reason and the tested boundary are recorded in
[`UPGRADE-NOTES.md`, section 6](UPGRADE-NOTES.md). Configure `JAVA_HOME` and the Android SDK
through your local Android development setup before running Gradle. Keep machine-specific SDK
paths out of project documentation and commits.

```bash
./gradlew clean
./gradlew assembleFullDebug assembleDemoDebug testFullDebugUnitTest testDemoDebugUnitTest lintFullDebug --stacktrace
./gradlew bundleFullRelease                       # AAB (signing is done by the human)
```

The lint gate has two documented, narrow exceptions in `app/lint.xml`: `NotificationPermission`
from the unused Glide notification class, and `SuspiciousIndentation` only at the existing
feedback-subject expression in `SettingsFragment.kt`. They preserve current behavior and do not
declare or request a notification permission. Keep new findings visible; do not replace these
entries with a lint baseline.

Signing, Play Console uploads, and merging PRs are always done by the human.

## Review

Apply the additional [review criteria](config/review-rules.md) when reviewing a change.
The reviewer may be a person or a separately chosen tool; no private service is required.
Maintainers can run the repository's own automated review by commenting `/simple-review`
([`CONTRIBUTING.md`](CONTRIBUTING.md), "Automated review").

Whether a pull request needs an independent review depends on its tier, decided by the paths it
changes and shown as a `review: docs`, `review: rules` or `review: code` label
([`CONTRIBUTING.md`](CONTRIBUTING.md), step 5). A `docs` pull request needs no independent
review; `rules` and `code` pull requests do. A change to this file, to the review criteria or to a
decision record is at least `rules`. Never change a tier label by hand to avoid a review.

## Reproducible modernization

`AGENTS.md` is the agent entry point; linked project documents hold the details.
`CLAUDE.md` is only an optional import adapter, with no separate rules. No AI provider,
subscription, owner-hosted reviewer or tool-specific command is required by the method.
Choose tools by capability and record the setup actually used for each published step.
Automated review is an optional implementation: any required review must still identify
its revision, findings and unresolved limitations. Disabling an integration is not a
passing review; reproducible tests and the human merge decision remain required.

This repository is a worked example of Android modernization. Keep the project-specific
instructions, build/test commands, decisions and acceptance evidence needed to reproduce
each published step in this repository. Apply the same documented project requirements
to owner and contributor work.

- Record each modernization step's starting revision, intended behavior, implementation
  and actual verification results in its PR or relevant project document. Report checks
  that were not run explicitly.
- Do not require access to the owner's private coordination repository, workflow plugin,
  accounts or undocumented instructions to follow a published step.
- An owner-hosted review service may support the work. Before teaching a step that depends
  on it, document how to reproduce that review with the reader's own setup or a documented
  alternative. Owner service output alone does not prove that alternative works.
- Verify reproducibility from a clean checkout at the documented revision and record
  required tools and prerequisites. This is an acceptance requirement, not a claim that
  every existing modernization step has already passed it.

These rules preserve the existing protections for history, user data, signing and human
merge decisions.

## Renovate dashboard (information only)

Renovate runs daily at 05:17 UTC and can also be started from **Actions → Renovate dependency updates → Run workflow** on `master`. Its [issue dashboard](https://github.com/baruckis/Kriptofolio/issues) is informational: every update waits for an explicit dashboard selection. Vulnerability auto-fix PRs, automerge, and platform automerge are disabled. An unselected dashboard run should not create dependency branches or pull requests. To enable it, set the repository Actions variables `RENOVATE_ENABLED=true` and `RENOVATE_APP_CLIENT_ID`, plus the secret `RENOVATE_APP_PRIVATE_KEY`; install the writer App only in this repository. Set `RENOVATE_ENABLED=false` or remove it to pause runs. The maintainer owns App settings and must never commit its private key.
