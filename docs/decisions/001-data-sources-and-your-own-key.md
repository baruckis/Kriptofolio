# ADR-001 — Data sources and your own key

- **Status:** accepted
- **Date:** 2026-10-06, revised 2026-10-21 (the change ships in 2.0.0, not in 1.3.0)

## Context

The 1.x app calls CoinMarketCap directly with the author's API key compiled into the APK. Every
user's refresh spends the author's credits, so cost grows with users; the key can be extracted from
the APK and abused; and the provider sees which coin ids each device holds, because the portfolio
screen asks for quotes by id. The author does not want to buy a paid data plan, and a cache that
the author runs would make the data minutes old for everyone while adding a server that must be
paid for and kept alive.

## Decision

1. Market data comes from a provider the user chooses and authenticates with a free key the user
   creates: CoinMarketCap first, CoinGecko second, behind one `MarketDataSource` interface.
   The key is stored only on the device in encrypted storage. The app ships no shared key.
2. Prices are fetched in USD and converted on the device with daily ECB exchange rates, so the
   providers' convert-currency limits do not constrain the app.
3. A privacy mode fetches the top-coin table instead of quotes by id, so the provider does not
   learn which coins the user holds. It uses more of the user's quota, and the app says so.
4. The app shows how fresh the data is and how much of the user's quota has been used, and sizes
   background refresh to the quota of the user's plan.
5. A starter feed without a key is served by a small cache the author runs on the provider's
   free plan, so a new user sees the app work before creating a key. It serves the same delayed
   top-coin table to everyone, keeps no identifiers, and the key lives only on that server. The
   app works fully without it: the provider may object or withdraw the key at any time, and the
   fallback is always the user's own key, which the app offers from the first screen.
6. Entering a key must take under two minutes: the provider is recognised from the key format, a
   key copied to the clipboard is offered for one-tap paste when the user returns to the app, the
   key is tested immediately with a visible quota meter, and the encrypted Pro backup carries the
   key so a second device needs no retyping.
7. **Timing.** All of the above ships in 2.0.0 (roadmap stage 5). The iOS app, which also ships
   in 2.0.0, never contains the author's key. Until then 1.3.0 keeps the 1.x data path, moved
   behind the same `MarketDataSource` interface in stage 3, with stricter limits: a manual
   refresh at most every 60 seconds, an automatic refresh on opening only when the data is older
   than five minutes, a visible "updated" time and an error message that names the cause. The
   key stays out of the repository and is supplied only to the owner's release build, as today.

## What we do not do

- We do not buy CoinMarketCap, CoinGecko or any other paid plan.
- From 2.0.0 we do not compile any provider key into the app, not even an "anonymous" one.
- We do not log request headers, and release builds contain no debug bridges.
- We do not write to the provider about the starter feed; we design for the feed disappearing.

## Consequences

- Onboarding in 2.0.0 must get a user from install to a working key in about two minutes, with a
  guided wizard and a key test. Existing users receive the same wizard after updating to 2.0.0;
  the app may never silently stop refreshing.
- Until 2.0.0 the "Unable to refresh" reports ([#5](https://github.com/baruckis/Kriptofolio/issues/5),
  [#7](https://github.com/baruckis/Kriptofolio/issues/7)) are mitigated, not removed; they are
  closed by 2.0.0.
- Coin identity across providers becomes a maintained data file with a generator script and a
  release check, because every provider uses different ids.
- Each provider's attribution requirements are shown in the app.
- Building the app yourself requires your own key as well; the test suites use recorded
  responses and never call a live provider.

## Reopen when

A provider changes its free plan terms or withdraws the starter feed key, or a keyless source
with acceptable terms appears.
