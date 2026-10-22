# Kriptofolio execution plan

*Last updated 2026-10-22. Owner: Andrius Baruckis. This plan orders the issues of the
[`1.3.0`](https://github.com/baruckis/Kriptofolio/milestone/2) and
[`2.0.0`](https://github.com/baruckis/Kriptofolio/milestone/1) milestones so that an AI coding agent
can carry out each one in its own session. Acceptance criteria stay in the issues; this file adds
the order, the agent setup the owner uses, implementation notes and stop conditions. The stages
and releases are in [`roadmap.md`](roadmap.md). Status is on the
[Kriptofolio Modernization](https://github.com/users/baruckis/projects/3) board.*

## How one task runs

1. Take the next task in the order below and move it to "In progress" on the board. One task is in
   progress at a time.
2. Open a new agent session with the model and effort in the table, and give it the brief below. If
   the issue and this plan are not enough, fix them, not the conversation.
3. The agent works in its own worktree, opens one pull request and stops before merge.
4. The pull request gets the review its tier requires ([`CONTRIBUTING.md`](../CONTRIBUTING.md),
   step 5): `/simple-review` until 5/5, or a separate agent session.
5. After merge: move the task to "Done" and start the next one.

```text
Implement {{ISSUE_URL}} in baruckis/Kriptofolio.
Read first: AGENTS.md, CONTRIBUTING.md, config/review-rules.md, the issue, and task {{TASK_ID}}
in docs/execution-plan.md. Work in a new worktree from origin/master on branch
mod/{{ISSUE_NUMBER}}-{{SLUG}}. Copy the acceptance criteria to the top of the pull request.
Stop before merge. Do not sign, release, tag, publish or post public replies.
If a stop condition is hit, stop and report instead of working around it.
Report: what changed, commands and results, what was not run, open questions.
```

## The agent setup the owner uses

The method needs no particular provider: any capable coding agent works, and each pull request
records the setup actually used. The owner's own setup is Claude Code with these models:

| Model | Used for |
|---|---|
| Claude Haiku 5.5 | Mechanical work: text, links, labels, creating issues from prepared text |
| Claude Sonnet 5.5 | Most steps with clear criteria: modules, screens, CI, documentation |
| Claude Opus 5.5 | Spikes, risky build steps, user data, iOS, payments |
| Claude Fable 5.1 | Only the reviews of the four steps where a missed defect would lose user data or money |

Effort: `medium` for a well-described step, `high` for several modules, build risk or UI with
screenshot tests, `xhigh` only for data migrations and cryptography, `max` only when escalating. If
a task fails its gate twice, the next attempt goes one level up, effort first, then model, and the
pull request says so.

## Order: release 1.3.0

| ID | Issue | Task | Depends on | Model / effort |
|---|---|---|---|---|
| T1 | [#35](https://github.com/baruckis/Kriptofolio/issues/35) | Version catalog | – | Sonnet / `medium` |
| T2 | [#38](https://github.com/baruckis/Kriptofolio/issues/38) | Supply chain, flipview, Stetho, [#29](https://github.com/baruckis/Kriptofolio/issues/29) | T1 | Sonnet / `high` |
| T3 | [#34](https://github.com/baruckis/Kriptofolio/issues/34) | Kotlin 2, KSP, Gradle and AGP, JDK range | T2 | Opus / `high` |
| T4 | [#39](https://github.com/baruckis/Kriptofolio/issues/39) | `RELEASING.md` and release gates | T1; alongside T3 | Sonnet / `medium` |
| T5 | [#36](https://github.com/baruckis/Kriptofolio/issues/36) | Koin spike | T3 | Opus / `high` |
| T6 | [#37](https://github.com/baruckis/Kriptofolio/issues/37) | Dagger to Koin | T5 go | Sonnet / `high` |
| T7 | [#40](https://github.com/baruckis/Kriptofolio/issues/40) | Room KMP spike | T6 | Opus / `xhigh` |
| T8 | [#41](https://github.com/baruckis/Kriptofolio/issues/41) | `shared/model`, `shared/data`, v1 migration | T7 go | Opus / `xhigh`; review Fable / `high` |
| T9 | [#42](https://github.com/baruckis/Kriptofolio/issues/42) | `shared/network`, `MarketDataSource`, refresh limits | T8 | Sonnet / `high` |
| T10 | [#43](https://github.com/baruckis/Kriptofolio/issues/43) | DataStore with the 1.x keys | T8; alongside T9 | Sonnet / `medium` |
| T11 | [#44](https://github.com/baruckis/Kriptofolio/issues/44) | Compose Multiplatform spike | T9, T10 | Opus / `high` |
| T12 | [#45](https://github.com/baruckis/Kriptofolio/issues/45) | Design system and licenses screen | T11 go | Sonnet / `high` |
| T13 | [#46](https://github.com/baruckis/Kriptofolio/issues/46) | Portfolio screen | T12 | Sonnet / `high` |
| T14 | [#47](https://github.com/baruckis/Kriptofolio/issues/47) | Add coin screen and [#10](https://github.com/baruckis/Kriptofolio/issues/10) | T13 | Sonnet / `high` |
| T15 | [#48](https://github.com/baruckis/Kriptofolio/issues/48) | Settings, export and import | T14 | Sonnet / `high` |
| T16 | [#49](https://github.com/baruckis/Kriptofolio/issues/49) | XML removal, parity table, TalkBack | T15 | Sonnet / `medium` |
| T17 | [#50](https://github.com/baruckis/Kriptofolio/issues/50) | 1.3.0 release preparation and update test | T16 | Sonnet / `high`; the owner releases |

The order differs from the issue numbers on purpose: flipview's Java 21 bytecode is what pins the
build to JDK 21 (`UPGRADE-NOTES.md` section 6), so vendoring it (T2) comes before the Kotlin 2 step
that widens the JDK range (T3).

## Order: release 2.0.0

Before each stage, a session turns these rows into issues with the `2.0.0` milestone, acceptance
criteria and a link to the decision record. The scope is frozen
([ADR-006](decisions/006-releases-branches-and-the-2-0-scope-freeze.md)).

| ID | Stage | Task | Model / effort |
|---|---|---|---|
| T34 | 5 | Encrypted key storage (`expect`/`actual`), CoinMarketCap with the user's key, the author's key removed | Sonnet / `high` |
| T35 | 5 | CoinGecko source with recorded responses | Sonnet / `high` |
| T36 | 5 | `coin-map.json`, its generator and manual matches | Sonnet / `medium` |
| T37 | 5 | USD prices and ECB rates on the device | Sonnet / `medium` |
| T38 | 5 | Privacy mode and quota meter | Sonnet / `high` |
| T39 | 5 | Key wizard (under two minutes) and the migration of existing users | Opus / `high` |
| T40 | 5 | Starter feed `feed/` (Ktor, Docker, no IP logs) and `docs/privacy-and-network.md`; the owner deploys | Sonnet / `high` |
| T41 | 7 | Schema v2: date of insertion ([#6](https://github.com/baruckis/Kriptofolio/issues/6)) and cost basis ([#28](https://github.com/baruckis/Kriptofolio/issues/28)), migration tested on the real v1 files | Opus / `xhigh`; review Fable / `high` |
| T42 | 7 | `Entitlements`, Play Billing, `play` and `foss`, `demo` replaced by debug fixtures | Opus / `high`; review Fable / `high` |
| T43 | 7 | Date of insertion UI and profit or loss | Sonnet / `high` |
| T44 | 7 | Encrypted backup and restore (AES-GCM, passphrase, versioned JSON) | Opus / `xhigh`; review Fable / `high` |
| T45 | 7 | App lock (PIN, biometrics) | Sonnet / `high` |
| T46 | 7 | Insights: ADR-007 confirmed, `InsightsEngine`, anonymizer, Android on-device model, own-key fallback | Opus / `high` |
| T47 | 6 | iOS targets for `shared/` and the UI module, Room KMP on iOS, Ktor Darwin | Opus / `high` |
| T48 | 6 | `iosApp/` shell, Keychain, background refresh, file picker | Opus / `high` |
| T49 | 6 | macOS CI (`xcodebuild`, shared tests), VoiceOver, iOS locales and RTL | Sonnet / `medium` |
| T50 | 7 | StoreKit 2 `Entitlements` and Insights through Apple Foundation Models | Opus / `high` |
| T51 | 7 | Supporter and Patron tiers, "Free and Pro" screen | Sonnet / `medium` |
| T52 | 7 | F-Droid metadata, reproducibility comparison, request text | Sonnet / `high` |
| T53 | R2 | 2.0.0 release preparation and update tests on both platforms | Sonnet / `high`; the owner releases |

Order: stage 5 (T34–T40); stage 7 from T41 to T46, which can start after T39; stage 6 (T47–T49)
after T34, whose key storage the iOS shell implements with Keychain; T50 after T42 and T48, because
StoreKit builds on `Entitlements` and needs the iOS shell; then T51, T52 and T53. Store accounts,
products, data-safety forms and the F-Droid request are the owner's actions. Task numbers T18–T33
belong to work outside this repository and are tracked elsewhere.

## Implementation notes for 1.3.0

Each agent picks the newest stable versions at the time, checks the compatibility tables and
records the versions in its pull request.

**T1 #35.** Save the dependency reports before and after; they must be identical. A pure move, no
version change. **Stop** if any resolved version differs.

**T2 #38.** Vendor flipview first: its Java 21 bytecode sets the JDK floor. Keep the licence header
and source link of the two Java files; `flipview_front_custom.xml` and the Data Binding imports keep
working. Stetho becomes `debugImplementation` with initialisation only in the debug source set.
`dependenciesInfo { includeInApk = false }`. Generate the verification metadata last and document
how to update it. Record the tool and command that show the dependency-info block is gone.

**T3 #34.** Upgrade the Gradle wrapper, then AGP, then Kotlin, then KSP. Room on KSP needs a newer
Room: after the upgrade the identity hash in `schemas/1.json` stays
`ad1c80913f23361aa985d56ecf84d645` and the characterization tests on the real database files stay
green. Glide moves to KSP; Dagger stays on kapt. If the pre-AndroidX `android.arch.navigation` 1.0
or Data Binding blocks K2, move minimally to `androidx.navigation` in the same pull request. CI
matrix: JDK 17 and 21, 25 if it works. **Stop** if the identity hash changes or any
characterization test changes its result.

**T4 #39.** `RELEASING.md` numbers the owner's steps, including `modernization-2026` and
`release/1.3`. The `release-gates` job checks that the key constant is empty, that a changelog
exists for the current `versionCode` in every fastlane locale, and that both flavors assemble in
release mode without signing.

**T5 #36.** Start with settings or licenses, the screens with the fewest dependencies. `startKoin`
in `App.onCreate` after Dagger; bridge with `single { appComponent.x() }`. The module verification
test is a JVM unit test. The pull request describes how the remaining screens will move and says go
or no-go.

**T6 #37.** One screen or service per commit, the gate after each. Order: licenses, settings,
search and add, main list, then `ViewModelFactory`, `AppInjector` and the `*BuildersModule` classes,
then the Dagger dependencies and kapt. `LocalizationModule` may stay, through Koin, until T16
removes `LocalizationManager`.

**T7 #40.** A test or a scratch module; choose between `AndroidSQLiteDriver` and
`BundledSQLiteDriver` by which one opens the real v1 file unchanged. Check that the `Double` columns
and the converters return the same values. **Stop** and return the decision to the owner if the
file would have to change before it opens.

**T8 #41.** `:shared:model` (plain types; `BigDecimal` through `expect`/`actual` or a multiplatform
library, choice recorded) and `:shared:data` (Room KMP, DAO with `Flow`, repository with
`refresh()`). The old repository and `NetworkBoundResource` stay until T9 and T16 no longer need
them; never two sources of truth for one table. Before the first open, the new version keeps a copy
of the v1 file for one version.

**T9 #42.** Ktor with the OkHttp engine on Android; rethrow `CancellationException`. The
CoinMarketCap envelope (`error_code` other than 0 with HTTP 200, `null` quote, `null` `max_supply`)
as in `BEHAVIOUR.md` section 4. The refresh limits are a pure function with an injected clock and
tests. Retrofit, Gson and `LiveDataCallAdapter` go when unused.

**T10 #43.** `SharedPreferencesMigration` with the exact set of 1.x keys; a test with
`app/src/test/resources/db/kriptofolio-v1.2.3-preferences.xml`.

**T11 #44.** A sample screen in a Compose Multiplatform module, Android target only. Check `iw`
(alias of `he`), `sw-rKE`, right-to-left layout, `AppCompatDelegate.setApplicationLocales` without
a restart, and screenshot tests in `en` and `iw`. Draft a navigation decision record with two
options and a recommendation. **Stop** with no-go if localization breaks.

**T12–T16.** Every screen: one `UiState`, a ViewModel in shared code, a stateless composable,
screenshot tests for every `ui-inventory.md` state in at least `en` and `iw`, its part of the parity
table, and scenario evidence on the free emulator. The old fragment and layout go in the same pull
request. T15's export format: JSON with `schemaVersion: 1`, coin id and amount; import merges by id
after a confirmation, and a damaged file leaves the portfolio untouched.

**T17 #50.** The agent prepares the release pull request (version, changelogs in every fastlane
locale) and runs the update test on the emulator: 1.2.3 with a synthetic portfolio and changed
settings, updated to 1.3.0, everything in place. Both versions must be signed with the same key:
both debug (1.2.3 from the `v1.2.3` tag and 1.3.0) or both release signed by the owner. The owner
signs, tags and creates the branches by `RELEASING.md`.

## Stop conditions for every task

The agent stops and reports if:

- the Room identity hash of an existing schema version would change, or a real v1 file would not
  open (a new schema version is allowed only in T41, with an exported schema and a migration
  tested on the real v1 files);
- the `applicationId`, the signing key or `versionCode` would change outside the release pull
  request;
- a key, password or paid account is needed;
- a spike says no-go;
- the task leaves the 1.3.0 scope or the frozen 2.0.0 list;
- the same gate fails twice.
