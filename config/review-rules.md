# Kriptofolio review rules

Read [AGENTS.md](../AGENTS.md) first. This file adds review-specific criteria for any
reviewer: a person, a separate agent session, or an automation chosen by the contributor.
There is no required hosted service or AI provider. The old automated workflow is removed;
its absence is not a passing review. Record the reviewed revision, findings and limitations
in the pull request, alongside actual build/test evidence.

Rules are written as *the rule, and why it exists*. The reason matters: a reviewer that
understands why a rule is there applies it correctly to cases this list does not name.

## What this project is

A free, open-source, privacy-first cryptocurrency portfolio app for Android, built in
2018–2019 and documented step by step in a public blog series that is still online and
still links here. The repository is both a living app and a historical artifact. Review it
like a museum somebody is allowed to renovate: carefully, reversibly, and without touching
the exhibits.

## Hard rules — treat violations as blocking issues

1. **History is untouchable.** Branches `Part-1`, `Part-2`, `Part-3`, `Part-4`, `Part-5`,
   branch `legacy` and tag `v1.2.1-legacy` are frozen: the 2018 blog series
   links directly to them, so rewriting them breaks published articles that this project
   does not control. No force push, no rebase of pushed branches, no direct pushes to
   `master`. Everything goes through a pull request. `README.md` and other documentation
   may change only when the task explicitly authorizes documentation changes, as in `AGENTS.md`.

2. **User data must survive an update.** The Room database is `version = 1` with
   `exportSchema = true`, and portfolios exist **only on the device** — there is no cloud
   backup and no export, so a wiped database is a permanently lost portfolio with no way to
   recover it. Any change that could break opening an existing v1 database is blocking
   unless it ships with a migration *and* a test against a real old database file. Treat
   entity fields, table and column names, type converters, the database version and the
   `fallbackToDestructiveMigration()` setting as load-bearing.

3. **Release identity is fixed.** `applicationId com.baruckis.kriptofolio` and the demo
   flavor's `.demo` suffix must never change. The Google Play listing and the signing key
   are bound to them; changing either orphans every existing install.

4. **Both flavors must always build.** `full` (real CoinMarketCap API) and `demo`. A change
   that compiles in one and not the other is blocking.

5. **Four locales, and one of them is right-to-left.** Default (`en`), `values-lt`,
   `values-iw` (Hebrew — RTL) and `values-sw-rKE`. A new user-visible string that is missing
   any of the four translations is blocking. Layout changes must consider RTL: hardcoded
   `left`/`right`, absolute margins, or directional drawables in a changed layout are
   findings, not opinions.

6. **No secrets in the repository.** The real CoinMarketCap API key never enters source
   control. Two things here look like violations of that and are not:

   - the **empty string with a TODO** in the **full** flavor's `ConstantsFlavor.kt`. It is
     deliberate: the key is a secret, this repository is public, and the key is pasted in by
     hand at release time. Do not flag it, do not suggest a default value, and never propose
     putting a real key there.
   - the **key in the demo flavor's `ConstantsFlavor.kt`**. It is CoinMarketCap's publicly
     documented shared *sandbox* key, not a private credential — it appears in dozens of public
     repositories, and the host it points at (`sandbox-api.coinmarketcap.com`) no longer exists.
     Do not flag it, and do not propose rotating or removing it.

   Anything else that resembles a live credential — an API key, token, password, private key or
   keystore — appearing in a diff is blocking.

7. **Privacy is the product promise.** No analytics, no crash reporting, no advertising and
   no tracking SDKs, and no portfolio data leaving the device. This is what the app is
   advertised as and why people trust it with financial holdings. Adding any of them, or any
   new network destination that carries user data, is blocking.

8. **Stage discipline.** A pull request that declares itself a keep-alive, compliance or
   maintenance change must not contain refactoring, reformatting, renames or opportunistic
   modernization. Those belong to the modernization steps in `docs/roadmap.md` and are recorded in
   `UPGRADE-NOTES.md`. Scope creep in a minimal release is a real defect: it makes the diff
   unreviewable exactly when reviewability is the point.

9. **Commit convention.** Udacity Git Commit Message Style Guide —
   `type: Capitalized summary.` with type one of `feat` / `fix` / `docs` / `style` /
   `refactor` / `test` / `chore`. A commit touching several aspects may chain sentences.
   This project teaches the Android community good practices, so the standard is not
   optional.

## Explicitly NOT blocking

Everything in this section is noise. Do not raise it as an Issue; at most mention it once
in Notes.

- **Code style and formatting.** There is no ktlint or detekt in this repository, and the
  code is deliberately 2019-era so it keeps matching the published articles that walk
  through it line by line. Never comment on formatting, naming style, indentation, or on a
  file "not following modern Kotlin conventions".
- **Suggestions to modernize existing code.** View binding, Compose, Koin, KSP, coroutines
  idioms, newer AndroidX APIs — all of it belongs to the modernization steps in `docs/roadmap.md`
  (backlog in `UPGRADE-NOTES.md` section 8), not to a review finding. If a diff *touches* such
  code, review the change, not the age of the surrounding code.
- **Markdown wording.** Documentation phrasing, tone and grammar are the author's.
- **Anything that cannot be verified from this repository alone.** Claims about the Play
  Console, a live API, a remote artifact, a device or a network are Notes by definition,
  however plausible.

## Issue vs Note

An **Issue** is a defect demonstrable from this repository alone, stated with a concrete
failure scenario — what input or situation produces what wrong outcome. Issues block.

Everything else is a **Note**: hardening preferences, hypotheticals, style opinions, and
anything needing verification outside this repository. Notes never count as unresolved and
do not become blocking issues merely because a reviewer prefers a different approach.

## Modernization step additions

The rules above protect the app as it is. For a pull request that is a step of `docs/roadmap.md`
(see `docs/decisions/003-incremental-modernization-against-the-behaviour-contract.md`), also check:

1. **Behaviour is preserved or traced.** A change from `docs/BEHAVIOUR.md` or `docs/ui-inventory.md`
   is a finding unless the pull request cites the decision record that allows it and updates
   `docs/behaviour-parity.md`.
2. **The migration is the exhibit.** Any change to the Room schema must come with the exported
   schema and a green migration test against the real v1 files in `app/src/test/resources/db/`.
3. **Merged is not released.** A step must not change `versionName` or `versionCode`, create a tag
   or touch signing and store configuration; only the owner's release pull request does.
4. **`master` stays releasable.** Both flavors build, tests pass, no feature half-enabled for
   users. From stage 7 the flavors are `play` and `foss` (`docs/decisions/003`); hard rules 3 and 4
   then apply to `play` and `foss`, and the `.demo` suffix retires with the `demo` flavor. The `foss`
   release build must contain no Google Play or Billing classes. From stage 6 a change to shared code must show that the iOS
   shell still builds.
5. **No key, no header in a log.** A provider key in source, fixtures, logs or screenshots is
   blocking, except the documented demo sandbox key of hard rule 6 until the `demo` flavor retires, as is logging of the authentication header outside debug builds.
6. **The 2.0.0 scope is frozen.** A pull request that adds a user-visible feature outside the 1.3.0
   scope or the frozen 2.0.0 list of `docs/decisions/006` is blocking unless it cites a newer
   decision record that changes that list.
7. **Presentation and wiring follow the records.** New screens follow the eight rules of
   `docs/decisions/004` (one immutable state, plain ViewModel methods, no navigation in the
   ViewModel, Room as the single source of truth). New dependencies are Koin definitions covered by
   the module verification test (`docs/decisions/005`); a Hilt dependency is blocking.
