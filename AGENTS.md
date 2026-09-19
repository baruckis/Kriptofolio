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
- `README.md` and all documentation files — do not modify unless the task explicitly says so.
- Git history — NEVER use `push --force`, `rebase` on pushed branches, or history rewrites.
- Never push directly to `master`. All work goes through a feature branch and a pull request
  that the human reviews and merges.

## Branches

The branch list in this repository is documentation. Someone arriving from the 2018 article
series sees `Part-1`…`Part-5` and immediately understands where they have landed. That signal
only survives while the list stays short, so every branch here is one of two kinds.

**Exhibits — kept forever.** `Part-1`…`Part-5` contain commits that are not on `master` and
never will be; deleting one destroys history that the published articles link to. `legacy`
marks the state of the app before the modernization. These are the protected branches listed
above.

**Working branches — deleted automatically once their pull request is merged.** Everything else: a feature,
a fix, a documentation change, a CI change. Nothing is lost by deleting one. Its commits stay
reachable from `master`, and the pull request keeps the full diff, the review and the
discussion — GitHub can restore the branch from the pull request page at any time. The
durable markers for a release are tags (`v1.2.1`, `v1.2.2`), never branches.

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
- Versions live in `versions.gradle` at the project root.

## Working style

- Minimal-diff policy: change only what the current task requires. No reformatting, no renames,
  no "while I'm here" improvements, no code style modernization in existing files.
- Match the existing 2019-era code style of whatever file you touch.
- When a dependency upgrade is needed, upgrade to the OLDEST version that solves the problem.
  Every tempting-but-not-required upgrade goes into `UPGRADE-NOTES.md` as a note for the
  planned 2.0 rewrite instead of being done now.
- One logical step per commit. Commit messages follow this repository's established
  convention, based on the Udacity Git Commit Message Style Guide
  (https://udacity.github.io/git-styleguide/): `type: Capitalized summary.` with type one of
  feat / fix / docs / style / refactor / test / chore. A commit touching several aspects may
  chain sentences: `feat: Added X. fix: Corrected Y.` — see `git log` for live examples.
  This project teaches the Android community good practices; the standard is not optional.
- Run the relevant build/tests after every step; never stack changes on a red build.
- If something is ambiguous or destructive, stop and ask the human.

## Build commands

```bash
./gradlew clean
./gradlew assembleFullDebug assembleDemoDebug     # both flavors must compile
./gradlew testFullDebugUnitTest                   # unit tests
./gradlew lintFullDebug                           # lint
./gradlew bundleFullRelease                       # AAB (signing is done by the human)
```

Signing, Play Console uploads, and merging PRs are always done by the human.

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
