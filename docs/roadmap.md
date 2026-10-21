# Kriptofolio roadmap

*Last updated 2026-10-21. Owner: Andrius Baruckis. Decisions are recorded in
[`docs/decisions/`](decisions/README.md). The behaviour contract is [`BEHAVIOUR.md`](BEHAVIOUR.md);
the public screen states are in [`ui-inventory.md`](ui-inventory.md). Progress is tracked on the
public [Kriptofolio Modernization](https://github.com/users/baruckis/projects/3) board.*

## One app, modernized step by step

Kriptofolio is modernized in place, in this repository. Every step is a reviewed pull request that
keeps the app working and preserves the behaviour in `BEHAVIOUR.md`, with evidence reproduced from
a clean checkout ([ADR-003](decisions/003-incremental-modernization-against-the-behaviour-contract.md)).
The app is also the public worked example of the AI-First Android Factory method.

**Merged is not released.** A step is merged when it is reviewed and green. A release (version bump,
tag, signing, store upload) is a separate decision of the owner. Stage 1, for example, is merged and
the last release is still 1.2.3.

## Two releases

| Release | What users get | What it proves |
|---|---|---|
| **1.3.0** (Android) | The same app, rebuilt: Kotlin 2, Koin, a shared Kotlin Multiplatform core, Compose UI with MVVM. Adding to an existing holding ([#10](https://github.com/baruckis/Kriptofolio/issues/10)), export and import to a file, refresh limits with honest error messages, no Google library in the build's licenses screen | A live 2018 app can be modernized step by step without losing anyone's portfolio |
| **2.0.0** (Android and iOS) | Your own key with CoinMarketCap or CoinGecko and a starter feed, iOS, Pro (encrypted backup, profit or loss, app lock, Insights), Supporter tiers, date of insertion ([#6](https://github.com/baruckis/Kriptofolio/issues/6)), the `foss` build on F-Droid | Kriptofolio as a product on two platforms |

Release mechanics, branches and the frozen 2.0.0 scope are in
[ADR-006](decisions/006-releases-branches-and-the-2-0-scope-freeze.md).

## Principles

1. **Your data stays on your device.** No account, no sync service, no analytics, no server that
   knows who you are. Portfolios live in the on-device database; backups are files you own.
2. **Your own key, from 2.0.0.** Market data will come from a provider you choose, with a free key
   you create yourself, and the app will ship no shared key. Until then 1.3.0 keeps the 1.x data
   path with stricter refresh limits. Details in
   [ADR-001](decisions/001-data-sources-and-your-own-key.md).
3. **Open source with a fair Pro.** The code stays Apache-2.0. Pro is a one-time purchase for
   features that run on your device; the `foss` build has everything unlocked. If you build it
   yourself, Pro is yours. Details in [ADR-002](decisions/002-pro-supporter-and-open-source.md).
4. **Evidence before merge.** Every change is a reviewed pull request with the commands that were
   run, their results and the exact revision that was reviewed. See [`CONTRIBUTING.md`](../CONTRIBUTING.md).

## Stages

| Stage | Goal | Exit criteria | Release | Status |
|---|---|---|---|---|
| 0 | Keep-alive: build again, target SDK 35 | Clean build, both flavors, 14 unit tests | 1.2.2 | Done ([#11](https://github.com/baruckis/Kriptofolio/pull/11)) |
| 0.5 | Target SDK 36 | Insets handled, deprecated opt-outs removed | 1.2.3 | Done (`UPGRADE-NOTES.md` §9) |
| 1 | Understand the legacy app | `BEHAVIOUR.md`, `ui-inventory.md`, characterization tests with real v1 database files, CI gate | – | Done ([#18](https://github.com/baruckis/Kriptofolio/pull/18), [#21](https://github.com/baruckis/Kriptofolio/pull/21)–[#24](https://github.com/baruckis/Kriptofolio/pull/24)) |
| 2 | Foundation | Kotlin 2 with KSP, wider JDK range, version catalog, Dagger replaced by Koin with a module `verify()` test ([ADR-005](decisions/005-koin-for-dependency-injection.md)), dependency verification, vendored flipview, Stetho debug-only, no dependency-info block in APKs ([#29](https://github.com/baruckis/Kriptofolio/issues/29)), `RELEASING.md` with release gates in CI | 1.3.0 | Planned |
| 3 | Shared core | `shared/` KMP modules for model, data (Room KMP, tested v1 migration) and network (Ktor behind `MarketDataSource`, still the 1.x CoinMarketCap path); DataStore with the 1.x preference keys; refresh limits and a freshness indicator ([#5](https://github.com/baruckis/Kriptofolio/issues/5), [#7](https://github.com/baruckis/Kriptofolio/issues/7)) | 1.3.0 | Planned |
| 4 | Compose and MVVM | A spike for Compose Multiplatform resources, four locales, RTL, per-app language and navigation; every `ui-inventory.md` state in Compose one screen at a time, in a Compose Multiplatform module with the Android target only; ViewModels in shared code ([ADR-004](decisions/004-mvvm-presentation-layer.md)); [#10](https://github.com/baruckis/Kriptofolio/issues/10); export and import; licenses screen without Google libraries; XML and Data Binding removed; parity table `docs/behaviour-parity.md`, created with the first screen and completed at the end; TalkBack | 1.3.0 | Planned |
| R1 | Release 1.3.0 | Staged rollout; branch `modernization-2026`; `release/1.3` for hotfixes | 1.3.0 | Planned |
| 5 | Your own key | CoinMarketCap and CoinGecko with a user-owned key in encrypted storage; key wizard; coin identity map; prices in USD converted on the device with ECB rates; starter feed; privacy and network page | 2.0.0 | Planned |
| 6 | iOS | iOS targets for the shared modules and the Compose Multiplatform UI, `iosApp/` shell, Keychain, VoiceOver | 2.0.0 | Planned |
| 7 | Pro and F-Droid | Database schema v2 with a tested migration (date of insertion [#6](https://github.com/baruckis/Kriptofolio/issues/6), cost basis [#28](https://github.com/baruckis/Kriptofolio/issues/28)); `Entitlements` with Google Play Billing, StoreKit 2 and an always-Pro `foss` build; encrypted backup, profit or loss, app lock, Insights ([ADR-007](decisions/007-insights-on-the-device.md)); Supporter tiers; `play` and `foss` flavors; F-Droid metadata and reproducibility check | 2.0.0 | Planned |
| R2 | Release 2.0.0 | Android and iOS together, staged rollout, F-Droid request filed | 2.0.0 | Planned |
| Later | After 2.0.0 | Price alerts ([#3](https://github.com/baruckis/Kriptofolio/issues/3)), widget, several portfolios, CSV export, QR key transfer between your own devices, desktop builds | 2.x | Backlog |

Dates are not promised. Issues carry the `1.3.0` or `2.0.0` milestone; anything later carries the
`later` label. Technical debt that stage 0 deliberately left alone is listed in `UPGRADE-NOTES.md`
section 8 and is resolved by these steps.

## Free and Pro

| Free | Pro, one-time purchase (2.0.0) |
|---|---|
| Everything the 1.x app does, with the refresh bugs fixed ([#5](https://github.com/baruckis/Kriptofolio/issues/5), [#7](https://github.com/baruckis/Kriptofolio/issues/7)) | Encrypted backup and restore, including your key and settings, without any cloud account |
| Add to an existing holding ([#10](https://github.com/baruckis/Kriptofolio/issues/10)), date of insertion ([#6](https://github.com/baruckis/Kriptofolio/issues/6)) | Cost basis and profit or loss ([#28](https://github.com/baruckis/Kriptofolio/issues/28)) |
| Export and import of your portfolio to a plain file | App lock with PIN or biometrics ([#28](https://github.com/baruckis/Kriptofolio/issues/28)) |
| Your own key, both providers, privacy mode, freshness indicator | Insights: an educational comment on your portfolio, generated on your device where supported |
| All themes, all languages, all layouts | After 2.0.0: alerts, widget, several portfolios, CSV export |

Pro is a lifetime purchase. The two Supporter tiers include everything in Pro and exist for people
who want to give more; they add a thank-you line in Settings. Purchases are made in the store of
your platform and do not transfer between Android and iOS, because there is no account; the app
says so before you buy. There is no subscription and no "live data" tier: freshness comes from
your own key.

## Data sources

| Source | Free tier as published by the provider | Role in Kriptofolio |
|---|---|---|
| CoinMarketCap Basic | Monthly credit quota, one convert currency, 60-second data | The 1.x integration; from 2.0.0 the default provider with your own key |
| CoinGecko Demo | Monthly call quota, attribution required | Second provider from 2.0.0 |
| Starter feed without a key | Delayed top-coin table served by a small cache the author runs on the provider's free plan | From 2.0.0: lets you see the app work before you create a key. It never receives your holdings and keeps no identifiers. The app works fully without it; if the provider withdraws it, your own key is the path |
| Exchange public endpoints | Per-IP limits, no key | Evaluated for live prices of held coins after 2.0.0 |

From 2.0.0 prices are fetched in USD and converted on the device with daily ECB rates, so a
provider's convert-currency limit does not matter. The privacy mode downloads the top-coin table
instead of asking the provider for your coin ids, so nobody learns what you hold.

## Platforms

Android first: 1.3.0 is Android only, with the logic already in Kotlin Multiplatform modules and
the screens in a Compose Multiplatform module. iOS joins at 2.0.0 from the same code. Android is
distributed on Google Play and, as the `foss` build, on F-Droid and GitHub releases; iOS on the
App Store. IzzyOnDroid already lists 1.2.1; 1.3.0 removes the dependency-info block and the Google
licenses library it reports. Desktop builds for macOS, Windows and Linux are a later item. No web
version is planned.

## F-Droid

The `foss` flavor contains no Google libraries and has every Pro feature unlocked. The request to
fdroiddata is filed when 2.0.0 is in production. Metadata lives in `fastlane/metadata/android/`.

## What we will not do

- No analytics or telemetry, no account, no server that knows who you are. The starter feed
  serves the same table to everyone and keeps no identifiers.
- No paid data plan on the author's side, no "live" subscription, no ads.
- No second code base beside `app/`, `shared/` (including the Compose Multiplatform UI) and, from
  stage 6, the `iosApp/` shell; no big-bang rewrite.
- No feature added to 2.0.0 outside its frozen list without a new decision record.
- No web version before the review after 2.0.0.
- No rewriting of history: the `Part-1`–`Part-5` and `legacy` branches stay frozen, and so will
  `modernization-2026` once it exists.

## How to follow or contribute

Watch the [Kriptofolio Modernization](https://github.com/users/baruckis/projects/3) board, the
`1.3.0` and `2.0.0` milestones and the pull requests. Start with [`CONTRIBUTING.md`](../CONTRIBUTING.md)
and [`config/review-rules.md`](../config/review-rules.md). After 1.3.0 the modernization is written
up in one standalone article on [baruckis.com](https://baruckis.com/), next to the original
2018–2019 [series](https://baruckis.com/android/kriptofolio-app-series/). The repeatable method
behind the steps is a separate product, AI-First Android Factory.
