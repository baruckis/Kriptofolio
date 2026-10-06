# Kriptofolio roadmap

*Last updated 2026-10-06. Owner: Andrius Baruckis. Decisions are recorded in
[`docs/decisions/`](decisions/README.md). The behaviour contract is [`BEHAVIOUR.md`](BEHAVIOUR.md);
the public screen states are in [`ui-inventory.md`](ui-inventory.md).*

## One app, modernized step by step

Kriptofolio is modernized in place, in this repository. Every step is a reviewed pull request that
keeps the app working and preserves the behaviour in `BEHAVIOUR.md`, with evidence reproduced from
a clean checkout ([ADR-003](decisions/003-incremental-modernization-against-the-behaviour-contract.md)).
The app is also the public worked example of the AI-First Android Factory method.

**Merged is not released.** A step is merged when it is reviewed and green. A release (version bump,
tag, signing, store upload) is a separate decision of the owner. Stage 1, for example, is merged and
the last release is still 1.2.3.

## Principles

1. **Your data stays on your device.** No account, no sync service, no analytics, no server that
   knows who you are. Portfolios live in the on-device database; backups are files you own.
2. **Your own key.** Market data comes from a provider you choose, with a free key you create
   yourself. The app ships no shared key. Details in
   [ADR-001](decisions/001-data-sources-and-your-own-key.md).
3. **Open source with a fair Pro.** The code stays Apache-2.0. Pro is a one-time purchase for
   features that run on your device; the `foss` build has everything unlocked. If you build it
   yourself, Pro is yours. Details in [ADR-002](decisions/002-pro-supporter-and-open-source.md).
4. **Evidence before merge.** Every change is a reviewed pull request with the commands that were
   run, their results and the exact revision that was reviewed. See [`CONTRIBUTING.md`](../CONTRIBUTING.md).

## Stages

| Stage | Goal | Exit criteria | Status |
|---|---|---|---|
| 0 | Keep-alive: build again, target SDK 35 | Clean build, both flavors, 14 unit tests | Done ([#11](https://github.com/baruckis/Kriptofolio/pull/11)) |
| 0.5 | Target SDK 36 | Insets handled, deprecated opt-outs removed | Done (`UPGRADE-NOTES.md` §9) |
| 1 | Understand the legacy app | `BEHAVIOUR.md`, `ui-inventory.md`, characterization tests with real v1 database files, CI gate | Done, merged ([#18](https://github.com/baruckis/Kriptofolio/pull/18), [#23](https://github.com/baruckis/Kriptofolio/pull/23), [#24](https://github.com/baruckis/Kriptofolio/pull/24)) |
| 2 | Foundation | Kotlin 2 with KSP, Hilt, wider JDK range, dependency verification, vendored flipview, Stetho debug-only, `RELEASING.md` with release gates in CI | Planned |
| 3 | Shared core and your own key | `shared/` KMP modules for model, data (Room KMP, tested v1 migration) and market-data sources; CoinMarketCap and CoinGecko with a user-owned key; coin identity map; on-device FX; key wizard; starter feed; privacy and network page | Planned |
| 4 | Compose | Every `ui-inventory.md` state in Compose one screen at a time, XML and Data Binding removed, four locales including RTL, TalkBack, parity table | Planned |
| 5 | Pro and F-Droid | `Entitlements` interface, Google Play Billing, `play` and `foss` flavors, Pro launch set, F-Droid metadata and reproducibility check | Planned |
| 6 | iOS | Compose screens moved into a Compose Multiplatform module, iOS shell, Keychain, StoreKit 2, VoiceOver | Planned |
| 7 | After launch (v2.1) | Price alerts ([#3](https://github.com/baruckis/Kriptofolio/issues/3)), widget, several portfolios, CSV export, QR key transfer between your own devices, desktop builds | Backlog |

## Planned releases

| Release | Contents | Needs |
|---|---|---|
| 1.3 (Android) | Modernized foundation, your own key with the wizard, starter feed, migration of existing installs | Stages 2–3, privacy page live |
| 2.0.0 (Android and iOS) | Compose UI, Pro with Supporter tiers, `foss` build, iOS launch | Stages 4–6 |

Dates are not promised. Each release uses staged rollout; a rollout is halted rather than rolled
back, because the database cannot be downgraded. Issues for the first release carry the `v2.0`
milestone; the `v2.1` label marks stage 7. Technical debt that stage 0 deliberately left alone is
listed in `UPGRADE-NOTES.md` section 8 and is resolved by these steps.

## Free and Pro

| Free | Pro, one-time purchase |
|---|---|
| Everything the 1.x app does, with the refresh bugs fixed ([#5](https://github.com/baruckis/Kriptofolio/issues/5), [#7](https://github.com/baruckis/Kriptofolio/issues/7)) | Encrypted local backup and restore without any cloud account |
| Add to an existing holding ([#10](https://github.com/baruckis/Kriptofolio/issues/10)), date of insertion ([#6](https://github.com/baruckis/Kriptofolio/issues/6)) | Cost basis and profit or loss ([#28](https://github.com/baruckis/Kriptofolio/issues/28)) |
| Your own key, both providers, privacy mode, freshness indicator | App lock with PIN or biometrics ([#28](https://github.com/baruckis/Kriptofolio/issues/28)) |
| All themes, all languages, all layouts | Stage 7: alerts, widget, several portfolios, CSV export |

Pro is a lifetime purchase. The two Supporter tiers include everything in Pro and exist for people
who want to give more; they add a thank-you line in Settings. Purchases are made in the store of
your platform and do not transfer between Android and iOS, because there is no account; the app
says so before you buy. There is no subscription and no "live data" tier: freshness comes from
your own key.

## Data sources

| Source | Free tier as published by the provider | Role in Kriptofolio |
|---|---|---|
| CoinMarketCap Basic | Monthly credit quota, one convert currency, 60-second data | Default provider; the 1.x integration |
| CoinGecko Demo | Monthly call quota, attribution required | Second provider |
| Starter feed without a key | Delayed top-coin table served by a small cache the author runs on the provider's free plan | Lets you see the app work before you create a key. It never receives your holdings and keeps no identifiers. The app works fully without it; if the provider withdraws it, your own key is the path |
| Exchange public endpoints | Per-IP limits, no key | Evaluated for live prices of held coins after stage 7 |

Prices are fetched in USD and converted on the device with daily ECB rates, so a provider's
convert-currency limit does not matter. The privacy mode downloads the top-coin table instead of
asking the provider for your coin ids, so nobody learns what you hold.

## Platforms

Android first, then iOS: the logic is shared through Kotlin Multiplatform, the screens through
Compose Multiplatform once they are all Compose, and both platforms launch together at 2.0.0.
Android is distributed on Google Play and, as the `foss` build, on F-Droid and GitHub releases; iOS
on the App Store. Desktop builds for macOS, Windows and Linux are a stage 7 item. No web version is
planned.

## F-Droid

The `foss` flavor contains no Google libraries and has every Pro feature unlocked. The request to
fdroiddata is filed when 2.0.0 is in production. Metadata lives in `fastlane/metadata/android/`.

## What we will not do

- No analytics or telemetry, no account, no server that knows who you are. The starter feed
  serves the same table to everyone and keeps no identifiers.
- No paid data plan on the author's side, no "live" subscription, no ads.
- No second code base beside `app/`, no big-bang rewrite.
- No web version before the stage 7 review.
- No rewriting of history: the `Part-1`–`Part-5` and `legacy` branches stay frozen.

## How to follow or contribute

Watch the `v2.0` milestone and the pull requests. Start with [`CONTRIBUTING.md`](../CONTRIBUTING.md)
and [`config/review-rules.md`](../config/review-rules.md). The modernization is written up on
[baruckis.com](https://baruckis.com/android/kriptofolio-app-series/) as the 2026 continuation
of the original series; short progress notes appear on X. The repeatable method behind the steps
is a separate product, AI-First Android Factory, available as a preview on request.
