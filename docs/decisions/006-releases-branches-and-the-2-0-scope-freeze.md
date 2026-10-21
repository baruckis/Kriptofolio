# ADR-006 — Releases, branches and the 2.0.0 scope freeze

- **Status:** accepted
- **Date:** 2026-10-21

## Context

The modernization of the existing app and the new product features are different kinds of work
with different audiences. The modernization should reach users, and the write-up about it, as
soon as it is complete. The new features, iOS and Pro make most sense to users as one launch.
Plans for this project have grown before without anything shipping, so the second release needs
a boundary that does not move.

## Decision

1. **1.3.0 is the modernized Android app.** It contains the 1.x features plus adding to an
   existing holding ([#10](https://github.com/baruckis/Kriptofolio/issues/10)) and a plain export
   and import, rebuilt through roadmap stages 2–4. It takes the next `versionCode` after 1.2.3.
2. **2.0.0 is the product release**, on Android and iOS at the same time. Its scope is frozen to
   this list: your own key with CoinMarketCap and CoinGecko and the starter feed (ADR-001); iOS;
   Pro with encrypted backup, cost basis and profit or loss, app lock and Insights (ADR-002,
   ADR-007); the Supporter tiers; date of insertion ([#6](https://github.com/baruckis/Kriptofolio/issues/6));
   the `play` and `foss` flavors and the F-Droid request. The first iOS version is 2.0.0.
3. **The freeze is enforced.** A feature not on that list goes to the "later" backlog
   automatically. The list changes only through a new decision record that states the reason.
4. **Branches.**
   - At the 1.3.0 release commit the owner creates the exhibit branch `modernization-2026`. It is
     kept forever beside `legacy` and `Part-1`–`Part-5`; `legacy...modernization-2026` shows the
     whole modernization. A later modernization gets its own year.
   - The owner also creates `release/1.3` from the tag `v1.3.0`. Hotfixes for 1.3.x are pull
     requests into `release/1.3` and are cherry-picked to `master`. The branch is kept until
     2.0.0 is in production; deleting it afterwards is the owner's decision.
   - `master` carries unreleased 2.0.0 work between the two releases. It always builds and passes
     its tests; it is not released from until 2.0.0. No feature flags are used.
5. Releases use staged rollout. A rollout is halted rather than rolled back, because the database
   cannot be downgraded.

## What we do not do

- No release of 2.0.0 work through 1.3.x.
- No dates promised in this repository.

## Consequences

- The article about the modernization is published after 1.3.0 reaches all users; 2.0.0 is its
  own launch.
- `AGENTS.md` lists `modernization-2026` as protected once it exists, and `release/1.3` as a
  maintenance branch.
- Issues carry the `1.3.0` or `2.0.0` milestone; everything else carries the `later` label.

## Reopen when

A 1.3.x problem cannot be fixed without 2.0.0 work, or a frozen 2.0.0 item proves impossible on one
of the platforms.
