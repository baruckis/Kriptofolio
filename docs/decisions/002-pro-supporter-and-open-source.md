# ADR-002 — Pro, Supporter and open source

- **Status:** accepted
- **Date:** 2026-10-06, revised 2026-10-21 (Insights joins Pro; plain export stays free)

## Context

Kriptofolio has been free since 2018 and earns nothing. It is Apache-2.0 and will stay so: the
tutorial series links to this code, and the trust of a privacy-first audience depends on it.
Once the data costs are the user's own (ADR-001), the author's recurring costs are close to zero,
so a subscription would charge rent for nothing. Google Play does not allow a free listing to
become a paid one, and changing the `applicationId` would orphan every install and the rating.

## Decision

1. **Pro is a one-time lifetime purchase** through Google Play Billing on Android and StoreKit on
   iOS, for features that run on the device: encrypted backup and restore, cost basis with profit
   or loss, app lock, and Insights (ADR-007); after 2.0.0, alerts, widget, several portfolios and
   CSV export. Everything the 1.x app does, plus the bug fixes, stays free.
2. **The privacy basics are free and stay free:** no account, no tracking, data only on the
   device, and a plain export and import of the portfolio to a file the user owns. Pro adds
   convenience and protection on top of them, never instead of them. The plain export ships in
   1.3.0, before 2.0.0 changes the database schema, so every user can keep a copy first.
3. **Supporter** is two optional one-time tiers that include everything in Pro and add a
   thank-you line in Settings, for people who want to give more. Purchases belong to the store
   they were made in and do not transfer between Android and iOS, because there is no account;
   the app states this before purchase. There is no subscription and no "live data" tier.
4. **The code stays Apache-2.0, including the Pro checks.** Billing sits behind one
   `Entitlements` interface with platform implementations: Google Play Billing in the Android
   `play` flavor, StoreKit 2 on iOS, and an always-Pro implementation in the Android `foss`
   flavor, which contains no Google libraries. If you build the app yourself, Pro is yours: you pay
   for convenience, updates and a maintained store listing, not for secrets.
5. **Trademark.** The Kriptofolio name and logo are not licensed. A fork must ship under a
   different name and icon. This is stated in `README.md`.

## What we do not do

- No open-core split with a private premium module.
- No gating of the privacy basics listed above, themes or languages.
- No ads and no data sale, ever.
- No shared-key "live" tier funded by subscriptions.

## Consequences

- The `foss` build is what F-Droid publishes, with all Pro features unlocked.
- Pricing, product ids and store configuration are the owner's operational decisions and are not
  recorded in this repository.
- Anyone can patch the entitlement check in their own build; at a one-time price for a privacy
  app this is accepted and not worth an arms race.

## Reopen when

Recurring costs appear on the author's side, or the review after 2.0.0 shows that the one-time
model cannot sustain maintenance.
