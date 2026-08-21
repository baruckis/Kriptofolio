# Recorded CoinMarketCap responses

Real responses from `https://pro-api.coinmarketcap.com`, captured on **2026-08-21** with the
production API key, from the two endpoints this app actually calls:

```
GET v1/cryptocurrency/listings/latest?convert={fiat}&limit={n}
GET v1/cryptocurrency/quotes/latest?convert={fiat}&id={ids}
```

They are committed as test data, and they are the baseline the 2.0 rewrite is measured against:
the new code has to turn these exact bytes into the same numbers on screen as the current code
does.

## Why they exist

This app depends on a third-party API that has already changed underneath it once — the sandbox
host the demo flavor was built against stopped resolving, which is why that flavor has had no data
for years. The API key is a single shared key with a monthly quota, so the responses can also stop
arriving for reasons that have nothing to do with the code.

If the format changes, the quota runs out, or the key lapses, the ability to record *today's*
behaviour is gone, and with it the ability to prove that a rewrite behaves the same. So it was
recorded while it could be.

The same files serve three purposes: the behaviour baseline for characterization tests, the data
the demo flavor will ship with instead of talking to a dead host, and deterministic input for
screenshot tests.

## The files

| File | What it is | Why it is here |
|---|---|---|
| `listings-latest-usd-200.json` | A real `listings/latest` response, `convert=USD`, `limit=200` | The main call. The app asks for `limit=5000`; 200 has the identical shape at a size that is reasonable to commit. |
| `listings-latest-eur-10.json` | The same call with `convert=EUR` | The quote object is keyed by the fiat code, and the app maps all 93 codes onto one field with Gson's `alternate`. A second currency proves that mechanism. |
| `quotes-latest-usd-1-1027.json` | A real `quotes/latest` response for ids `1,1027` (Bitcoin, Ethereum) | The second endpoint. Its `data` is a **map keyed by id**, not a list — a different shape from `listings/latest`. |
| `listings-edge-cases.json` | Nine real records selected out of a full 5000-record capture | See below. This is the most useful file here. |
| `error-401-invalid-key.json` | HTTP 401, `error_code: 1001` | What a dead or rotated key looks like. |
| `error-400-invalid-id.json` | HTTP 400, `error_code: 400` | What an unknown coin id looks like. |
| `response-200-unknown-convert.json` | **HTTP 200**, `error_code: 0` | Named deliberately: an unsupported currency is *not* reported as an error. See below. |

A full 5000-record capture (5.8 MB, 1.2 MB gzipped) is kept outside this repository, in the
author's private workspace, since committing it would add several megabytes to a repository that
is also a historical exhibit.

## What the edge cases are, and why each was chosen

Every record in `listings-edge-cases.json` is real, taken from the 5000-record capture:

| Coin | Why |
|---|---|
| TOMI | Smallest non-zero price in the whole response: `2.27526737415e-19` |
| Maya Preferred PRA | Largest price: `3 741 731 042.49` |
| Black Phoenix | Largest 24-hour gain |
| Felis | Largest 24-hour loss |
| Ethereum | `max_supply` is `null` |
| Global X Robotics & Artificial Int… | Longest name |
| USD Coin (Wormhole) | Longest symbol |
| 币安人生 | Non-ASCII characters in both name and symbol |
| Bitcoin | `platform` and `quote.tvl` are null; also the ordinary rank-1 case |

**The price range across one response spans 28 orders of magnitude**, from `2.3e-19` to `3.7e9`.
Every formatting, rounding and totalling decision in the app has to survive that, in 93 currencies
and 4 locales. That is the single most useful fact in this directory.

## Two things the capture revealed about the current code

Both are recorded here rather than fixed, because this directory documents behaviour rather than
changing it.

1. **`max_supply` is `null` for 1735 of 5000 records — 34.7 % — and `CryptocurrencyLatest`
   declares it as a non-null `Double`.** Gson sets the field through reflection, so Kotlin's null
   check never runs and nothing throws. It is harmless today only because nothing reads it:
   `maxSupply`, `totalSupply`, `circulatingSupply`, `numMarketPairs`, `dateAdded` and `slug` are
   all parsed and never used anywhere in the app. In the rewrite they should not exist at all, and
   anything that does exist must be nullable where the API says it is.

2. **An unsupported `convert` code is not an error.** The API answers HTTP 200 with
   `error_code: 0`, and returns `quote: { "XXX": { "price": null, … } }`. The app maps the quote
   object with `@SerializedName(value = "USD", alternate = [ …93 codes… ])`, so an unknown key
   matches nothing and the quote silently becomes null. It cannot happen while the app's own
   currency list matches CoinMarketCap's, and it is exactly what would happen if the two ever
   drift apart.

## Regenerating

```bash
KEY=…                                  # never committed; see UPGRADE-NOTES.md section 7
B=https://pro-api.coinmarketcap.com
curl -H "X-CMC_PRO_API_KEY: $KEY" "$B/v1/cryptocurrency/listings/latest?convert=USD&limit=200"
curl -H "X-CMC_PRO_API_KEY: $KEY" "$B/v1/cryptocurrency/quotes/latest?convert=USD&id=1,1027"
```

The responses contain no credentials — the key travels in a request header, never in the body.
They do contain a `status.timestamp` and live prices, so they are a snapshot of one moment and
should be treated as fixtures, not as facts about the market.
