# Kriptofolio 1.2.3 — behaviour specification

*The contract the 2.0 rewrite is measured against. Extracted from the code on `master` at
`0b77c42cfeedd5de61ddbd1ee6d9624caac19da9` (the code that ships as 1.2.3, versionCode 6).
Since the `v1.2.3` tag exactly one file under `app/src/main` has changed,
`app/src/main/java/com/baruckis/kriptofolio/db/AppDatabase.kt`: `exportSchema` went from `false`
to `true` with an explanatory comment (pull request #18), which writes the schema file at build
time and changes nothing at runtime. Every source citation uses a repository-root-relative path
and line in the form `app/...:line[-line]`, including resource and locale-specific files.*

This document describes **what the app does**, not what it should do. Where the code does
something surprising, the surprise is recorded under *Known behaviour (2019)* and left alone:
this stage adds documents and tests, it does not fix. The rewrite decides each item there with a
note in the same pull request that changes it. Where the code leaves something undefined, it is
listed under *Undefined* rather than guessed.

The behavior tests in [PR #24 at candidate HEAD
`3d17901`](https://github.com/baruckis/Kriptofolio/tree/3d17901b2831e15ddec707393d7795002d4374ed)
pin only the behavior named in their assertions and comments. The public UI inventory in [PR #23
at candidate HEAD `24ece1b`](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md)
repeats five portfolio states; it does not verify every screen and state described here. At this
specification's base revision (`0b77c42`), the PR #23 inventory and PR #24 test code are separate,
unmerged candidates. A **pinned by** mark names the candidate evidence that reaches that claim.
Other behavior is supported by source citations here, not by an unlisted screenshot or test.

---

## 1. Portfolio maths

**pinned by** `CalculateUtilsTest` (the two pure functions) and `LegacyDatabaseTest` (stored values in the SQLite fixtures). ViewModel portfolio sums and the mixed-currency `NaN` state are described from source; the public UI subset does not capture the latter.

Two pure functions are the whole of the arithmetic, and both work on `Double`
(`app/src/main/java/com/baruckis/kriptofolio/utilities/CalculateUtils.kt:24-29`):

| Quantity | Formula | Where |
|---|---|---|
| holding value in fiat | `amount × priceFiat`; `null` amount gives `null` | `app/src/main/java/com/baruckis/kriptofolio/utilities/CalculateUtils.kt:24-25` |
| holding 24 h change in fiat | `amountFiat × (percentChange24h / 100)`; `null` amountFiat gives `null` | `app/src/main/java/com/baruckis/kriptofolio/utilities/CalculateUtils.kt:28-29` |

Both values are **stored**, not derived on display: they are written into the
`my_cryptocurrencies` row whenever the coin is added (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:170-174`),
updated from the network (`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:107-112`) or re-priced from a fresh listing
(`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:142-146`). The list on screen shows the stored numbers.

**Portfolio total in fiat** is the sum over the user's coins of the stored `amountFiat`, with a
`null` counted as `0.0` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:200-216`). **Portfolio 24 h change** is the
same sum over `amountFiatChange24h` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:160-176`). Summation is plain `Double`
addition in list order (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:142-154`).

**The NaN rule.** If any coin in the list is priced in a fiat currency other than the currently
selected one, both totals are `NaN` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:146-150`), and the screen shows
`― ― ―` instead of a number (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:171-173` and `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:225-227`, text from
`app/src/main/res/values/strings.xml:76`). This is the state between choosing a new currency and the refresh
that re-prices the coins in it.

**Portfolio total in Bitcoin** is `totalFiat / priceOfBitcoin`, where the Bitcoin price is the row
of the `all_cryptocurrencies` table whose `symbol` is `BTC` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:179-193`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:233-236`;
query `app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:65`, code from `app/src/main/res/values/strings.xml:78`). It is shown with the
`₿` sign (`app/src/main/res/values/strings.xml:79`). The Bitcoin row's own fiat currency is **not** checked against the
selected one (see *Known behaviour* K7).

**The "last updated" date** of the portfolio is the `lastFetchedDate` of the first coin in the
list, but only if every coin carries the same date; otherwise it is `null` and the header shows no
date at all (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:252-262`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:97-102`).

## 2. Number formatting

**pinned by** `FormatUtilsTest`

Three `DecimalFormat` patterns (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:24-26`, wrapped by
`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:37-41`):

| `ValueType` | pattern | used for |
|---|---|---|
| `Crypto` | `#,##0.00000000` | coin amounts, the Bitcoin total |
| `Fiat` | `#,##0.00` | prices, holding values, fiat totals, the 24 h fiat change in the header |
| `Percent` | `##0.00` | percentage changes |

`roundValue` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:50-54`) builds a `DecimalFormat` with the pattern **and the JVM
default locale** (no locale is passed), sets `RoundingMode.DOWN` and formats. So:

- values are **truncated**, never rounded: `0.999999999` BTC shows as `0.99999999`, `-0.005 %`
  shows as `-0.00`, `1.999` USD shows as `1.99`;
- a value smaller than the last digit shows as zero: a price of `2.27e-19` shows as `0.00`, which
  is what the smallest coin in `app/src/test/resources/api/listings-edge-cases.json` looks like on
  screen; the 28 orders of magnitude in that file collapse to "0.00 … 3,741,731,042.48";
- `null` is formatted as `DecimalFormat.format(null)` would — see *Undefined* U1; in practice the
  callers pass a non-null value or substitute `0.0` first (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:72-74`);
- the grouping and decimal separators come from the **default locale**, which the app sets to its
  own UI language on every start (§9). English, Hebrew and Swahili format `1,234.56`; Lithuanian
  formats `1 234,56` with a non-breaking space as the grouping separator. The pattern's grouping
  size (3) and digit counts do not vary by locale.

**Sign and colour** (`getSpannableValueStyled`, `app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:57-92`):

- `null` is treated as `0.0` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:72-74`);
- a positive value gets a `+` **appended to the left text** and the green colour
  `colorForValueChangePositive` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:77-80`, `app/src/main/res/values/colors.xml:29,74` = `#2E7D32`);
- a negative value keeps the `-` that `DecimalFormat` produces and gets the red colour
  `colorForValueChangeNegative` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:81-83`, `app/src/main/res/values/colors.xml:30,73` = `#C62828`);
- zero gets no sign and the neutral list text colour (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:60`, `app/src/main/res/values/colors.xml:39`);
- `NaN` is rendered as the caller's `textIfNaN` between `left` and `right`, and is coloured as
  zero because `NaN > 0` and `NaN < 0` are both false (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:76-87`);
- the colour is applied as a foreground span or as a background span depending on the caller
  (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:63-68`); the header's 24 h change uses the background style (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:185-187`),
  every list cell uses the foreground style (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:227-230`).

**What each text on screen is built from** (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:198-230`,
`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:182-246`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:97-123`):

| Text | Construction |
|---|---|
| rank | `rank` as an integer string (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:198`) |
| icon fallback | first 3 characters of the symbol (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:200`, `app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:29`, `app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:96-99`) |
| amount | `Crypto(amount) + " " + symbol` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:224`) |
| price | `Fiat(priceFiat) + " " + currencyCode` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:225`) |
| holding value | `Fiat(amountFiat) + " " + currencyCode` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:226`) |
| 1 h / 7 d change | `Percent(change1h) + "%"` + `" / "` + **`Fiat`**`(change7d) + "%"` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:227-228`, separator `app/src/main/res/values/strings.xml:88`) — see K2 |
| 24 h price change | `Percent(change24h) + "%"` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:229`) |
| 24 h holding change | **`Percent`**`(amountFiatChange24h) + " " + currencyCode` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:230`) — see K3 |
| header total | `sign + " " + Fiat(total)` or `sign + " ― ― ―"` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:225-227`) |
| header Bitcoin total | `"₿ " + Crypto(total / btcPrice)` or `"₿ ― ― ―"` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:242-244`) |
| header 24 h change | `" " + sign + " " [+ "+"] + Fiat(change) + " "` on a coloured background (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:185-187`) |
| header date | `"Total holdings value"` + `" (<date> UTC)"` when a date exists (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:98-100`, `app/src/main/res/values/strings.xml:66-67`) — see K1 |
| column headers | the sign is substituted into `"Price %1$s\nAmount %2$s"` and `"± amount %1$s 24 h"` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:121-122`, `app/src/main/res/values/strings.xml:86-87`) |

**The fiat sign** is looked up by code in two parallel string arrays,
`fiat_currency_code_array` (`app/src/main/res/values/strings.xml:109-203`) and `fiat_currency_sign_array`
(`app/src/main/res/values/strings.xml:205-299`), zipped by index (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:233-245`). Both
arrays have 93 entries, in the same order as the settings entries and values
(`app/src/main/res/values/strings.xml:363-457`, `app/src/main/res/values/strings.xml:459-553`) and as the 93 codes accepted from the API (§4). A code that is
not in the array throws `NoSuchElementException` (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:244`, see U2). Four
signs are wrong in the array — see K4.

## 3. Date and time formatting

**pinned by** `FormatUtilsTest`

`formatDate` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:107-137`) formats a `Date` with the user's date pattern (§7)
followed by a space and a time pattern — `HH:mm:ss` for 24-hour, `hh:mm:ss` for 12-hour
(`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:34-35`, `app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:102-105,128`). In 12-hour mode the localized AM/PM word
(`app/src/main/res/values/strings.xml:56-57` and its translations) is appended after another space (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:114-122,133`); the
pattern itself never contains `a`. A `null` date or a `null` pattern gives `""` (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:109`).

`SimpleDateFormat` is created with `Locale.getDefault()` and **no explicit time zone**
(`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:130`), so the instant is rendered in the **device's** time zone — while the surrounding label
says `UTC` (`app/src/main/res/values/strings.xml:67,312`). See K1.

The date being formatted is the server's `status.timestamp` from the response that last updated
the row (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:262-265,281-284`), parsed by Gson's default `Date` adapter
from the ISO 8601 string CoinMarketCap sends, and stored as epoch milliseconds
(`app/src/main/java/com/baruckis/kriptofolio/db/Converters.kt:30-37`, schema column `last_fetched_date INTEGER`). It is not the device time
at which the refresh happened.

The settings screen shows a preview of each date pattern applied to today's date, without a time
part (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:348-352`).

## 4. The CoinMarketCap envelope

**pinned by** `ApiEnvelopeTest`

**Requests.** Two endpoints (`app/src/main/java/com/baruckis/kriptofolio/api/ApiService.kt:32-46`), both `GET`, both with the API key in the
`X-CMC_PRO_API_KEY` header added by an interceptor (`app/src/main/java/com/baruckis/kriptofolio/api/AuthenticationInterceptor.kt:32-34`,
`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:31`):

| Call | Query | Used by |
|---|---|---|
| `v1/cryptocurrency/listings/latest` | `convert=<fiat>&limit=5000` (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:33`) | the add/search screen, §6 |
| `v1/cryptocurrency/quotes/latest` | `convert=<fiat>&id=<comma separated ids>` | the portfolio screen, §5 |

The base URL is `https://pro-api.coinmarketcap.com/` in the full flavor and
`https://sandbox-api.coinmarketcap.com/` in the demo flavor
(`app/src/full/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt:25`, `app/src/demo/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt:25`).
OkHttp is configured **not** to retry on connection failure (`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/AppModule.kt:63`).
Body logging is on in debug builds only (`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/AppModule.kt:71`). Timeouts are OkHttp's defaults (U3).

**Response shape.** The body is deserialized with Gson into `CoinMarketCap<T>` — `status`, `data`,
plus three fields (`statusCode`, `error`, `message`) that CoinMarketCap does not send
(`app/src/main/java/com/baruckis/kriptofolio/api/CoinMarketCap.kt:25-31`). `status` carries `timestamp` (a `Date`), `error_code`,
`error_message`, `elapsed`, `credit_count` (`app/src/main/java/com/baruckis/kriptofolio/api/CoinMarketCap.kt:33-42`). For `listings/latest`,
`data` is a list; for `quotes/latest`, `data` is a map keyed by the id as a string
(`app/src/main/java/com/baruckis/kriptofolio/api/ApiService.kt:38,46`; the map is flattened to a list before saving,
`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:65-72`).

**Which fields are read.** Of everything in `CryptocurrencyLatest` (`app/src/main/java/com/baruckis/kriptofolio/api/CryptocurrencyLatest.kt:24-46`)
the app uses `id`, `name`, `symbol`, `cmc_rank` and, from the quote, `price`,
`percent_change_1h`, `percent_change_24h`, `percent_change_7d`
(`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:262-265`). `slug`, `circulating_supply`, `total_supply`,
`max_supply`, `date_added`, `num_market_pairs`, `last_updated`, `volume_24h`, `market_cap` and the
quote's `last_updated` are parsed and never read.

**The quote key.** The quote object is keyed by the fiat code. The app maps it onto one field with
`@SerializedName(value = "USD", alternate = [ … ])` listing the other 92 codes
(`app/src/main/java/com/baruckis/kriptofolio/api/CryptocurrencyLatest.kt:53-63`). The 93 codes are exactly, and in the same order as, the codes
in `app/src/main/res/values/strings.xml:109-203`.

**How the envelope is classified** (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:35-75`, `app/src/main/java/com/baruckis/kriptofolio/utilities/LiveDataCallAdapter.kt:43-51`):

| What arrives | Classified as | Message |
|---|---|---|
| HTTP 2xx with a body | `ApiSuccessResponse(body)` (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:36-42`) | — |
| HTTP 2xx with no body, or HTTP 204 | `ApiEmptyResponse` (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:38-39`) | — |
| HTTP error with a JSON body | `ApiErrorResponse` (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:43-73`) | `status.error_message` from the body, else the top-level `message`, else the raw body, else the HTTP reason phrase (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:47-71`) |
| HTTP error with a non-JSON body | `ApiErrorResponse` | the raw body if non-empty, else the HTTP reason phrase (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:64-71`) |
| transport failure (no connection, DNS, timeout) | `ApiErrorResponse` (`app/src/main/java/com/baruckis/kriptofolio/api/ApiResponse.kt:31-33`) | the exception message, or `"Unknown error."` |

So `app/src/test/resources/api/error-401-invalid-key.json` yields the message `This API Key is invalid. ` and
`app/src/test/resources/api/error-400-invalid-id.json` yields `No data found for 'id': '999999999'`. **The message is never
shown to the user**: the UI shows its own fixed string instead (§5, §6). It is only visible in the
debug log.

**`error_code` is not inspected.** Nothing in the app reads `status.errorCode` or
`status.errorMessage` after a successful HTTP response. An HTTP 200 body with `error_code ≠ 0`
is a success like any other, and what happens next depends only on `data`:

- for the portfolio call, a `null` or empty `data` saves nothing and the screen reports success
  with the rows it already had (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:68-76`, `app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:75-87`);
- for the listing call, a `null` or empty `data` becomes an **empty list**, and saving it deletes
  every row of `all_cryptocurrencies` (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:124-127`,
  `app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:48-52`) — see K5.

**Null quote for an unsupported currency.** `app/src/test/resources/api/response-200-unknown-convert.json` is what
`convert=XXX` returns: HTTP 200, `error_code: 0`, and a quote keyed `XXX` with every number
`null`. No alternate name matches `XXX`, so Gson leaves `quote.currency` **null** although the
Kotlin type is non-null. Reading `it.quote.currency.price` while saving then throws a
`NullPointerException` on the disk executor (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:263`), which is an
uncaught exception — see K6. It cannot happen while the app's list of 93 codes and CoinMarketCap's
agree, and nothing checks that they do.

**Null `max_supply`.** The API fixture README reports `"max_supply": null` for 1,735 of 5,000
records (34.7 %) in the full capture; the committed fixture is a nine-record sample, so this
prevalence is recorded provenance and cannot be recomputed from that sample alone
(`app/src/test/resources/api/README.md`, section *Two things the capture revealed about the current code*).
The field is declared as non-null `Double` (`app/src/main/java/com/baruckis/kriptofolio/api/CryptocurrencyLatest.kt:36-37`).
Gson does not assign `null` to a primitive-backed field, so the value silently becomes `0.0`.
Harmless, because the field is never read.

**The timestamp.** `status.timestamp` (e.g. `2026-08-21T15:52:34.024Z`) is the only date the app
stores; it becomes `last_fetched_date` of every row written by that response
(`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:76,124`).

## 5. Portfolio screen

**pinned by** the five public portfolio captures in [the PR #23 UI inventory at its candidate HEAD](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md) (empty, data, cached refresh error, delete/undo, and Hebrew RTL); the two pure functions behind stored totals by `CalculateUtilsTest` (§1). The mixed-currency `― ― ―` state and other historical states are not in the public screenshot subset.

**Source of truth** is the `my_cryptocurrencies` table, read as `LiveData` with
`WHERE amount IS NOT NULL ORDER BY amount_fiat DESC, rank ASC` (`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:31-32`).
The list is therefore ordered by holding value, largest first, ties by rank; a row whose
`amount` is `NULL` is invisible (U4).

**On open** the screen shows the database rows without calling the network
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:98`, `app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:60,79-81`: `shouldFetch` is `false`). The
`NetworkBoundResource` first emits `LOADING(null)`, then `SUCCESS_DB(rows)`
(`app/src/main/java/com/baruckis/kriptofolio/repository/NetworkBoundResource.kt:48-63`). Nothing is auto-refreshed on open, however old the
rows are; there is no notion of staleness anywhere in the code (U5).

**Visible states** (`app/src/main/res/layout/fragment_main_list.xml:147-208`, `app/src/main/res/layout/loading_state.xml`,
`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:415-493`):

| State | Condition | What shows |
|---|---|---|
| loading, nothing yet | `LOADING` and `data == null` | a centred progress bar below the column-header card (`app/src/main/res/layout/loading_state.xml:35-49`) |
| empty | `data` is an empty list | "Your owned crypto coins list is empty! / Add your crypto via the + button below." (`app/src/main/res/layout/fragment_main_list.xml:147-174`, `app/src/main/res/values/strings.xml:81-82`), header totals `$ 0.00` / `₿ 0.00000000` / `$ 0.00` |
| data | `data` non-empty | the card list; header totals |
| refreshing | a network call in flight over existing data | the swipe-refresh spinner; the currency spinner disabled (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:116,168,197,434`) |
| error | `ERROR` after a fetch | the existing rows stay; an **indefinite** snackbar "Unable to refresh." with a **Retry** action (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:449-478`, `app/src/main/res/values/strings.xml:48-49`) |
| multi-select | one or more cards selected | a contextual action bar titled "Selected: N" with *Select all* and *Delete* (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:344-358`, `app/src/main/res/menu/menu_action_mode.xml`, `app/src/main/res/values/strings.xml:302-304`); the status bar turns black (`app/src/main/java/com/baruckis/kriptofolio/utilities/PrimaryActionModeController.kt:70-87`, `app/src/main/res/values/colors.xml:42`); swipe-to-refresh is disabled (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:264-266`) |
| undo | just after a delete | a `LENGTH_LONG` snackbar "Deleted: N" with an **Undo** action (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:502-536`, `app/src/main/res/values/strings.xml:53-54`) |

The retry action of the error snackbar starts a new fetch with the same parameters
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:455-467`); swiping the snackbar away or letting a newer one replace it
abandons a pending currency change (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:468-472`).

**Refresh triggers** — every one of them fetches `quotes/latest` for the ids in the table
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:324-346`; the ids come from `GROUP_CONCAT(id)`, `app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:38-39`),
after a fixed **1 000 ms delay** (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:28`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:307`,
`app/src/main/java/com/baruckis/kriptofolio/repository/NetworkBoundResource.kt:118-121`):

1. pull-to-refresh (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:114-118`), enabled only while the app bar is fully
   expanded (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:392-402`);
2. choosing a currency in the header spinner whose code differs from the currency the rows are
   priced in (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:144-173`) — if the rows are already in that currency the preference is written and
   the list is re-read from the database instead (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:164-166`);
3. a fiat currency change made on the settings screen (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:178-202`, same rule);
4. adding a coin whose row is in another currency, or when the rows' fetch dates disagree
   (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:350-403`; the check is `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:367-386`, then a 500 ms pause, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:394`);
5. the Retry action of the error snackbar.

**Empty portfolio, no network call.** When there are no ids the "fetch" is short-circuited to
`ApiEmptyResponse` without touching the network (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:93-96`,
`app/src/main/java/com/baruckis/kriptofolio/repository/NetworkBoundResource.kt:99-106`) and the screen reports `SUCCESS_DB`. An empty portfolio can
never show the error snackbar.

**What a successful refresh writes** (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:63-77`,
`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:71-117`): for each returned coin the name, rank, symbol, currency, price,
three percentages and the timestamp are copied into the existing row, `amount` is kept, and
`amountFiat` / `amountFiatChange24h` are recomputed (§1). A coin the API did not return keeps its
old row untouched, including its old `currency_fiat` — the NaN rule (§1) then hides the totals
until it is returned again.

**Currency change protocol.** The new code is held in the view model
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:76,302`) and written to the preference only after the fetch **succeeds**
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:482-485`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:450-452`); on failure the spinner snaps back to
the stored currency (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:476-478`). A change from the settings screen is the
other way round: the preference is written first and the fetch follows (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:178-202`), so a failed
fetch leaves the preference on the new currency and the rows in the old one — the NaN state.

**Multi-select and delete** (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:274-316`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:113-181`):

- a long press on a card, or a tap on its coin icon, selects it (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:54-58`,
  `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListItemLookup.kt`); the icon flips to show the selection;
- *Select all* selects every row currently in the adapter (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:277-283`);
- *Delete* removes the selected rows from the list with an animation, closes the action bar,
  shows the empty state if nothing is left, **deletes the rows from the database immediately**
  (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:305`, `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:407-416`, `app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:125-126`) and shows the undo
  snackbar;
- *Undo* re-inserts the same rows with `INSERT OR IGNORE` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:509-526`,
  `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:419-428`, `app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:66-67`) and restores them at their old
  positions on screen; once the snackbar times out or is swiped away the rows are forgotten
  (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:530-534`);
- selection, the deleted rows and their positions survive rotation (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:234-262`).

There is no confirmation dialog; the undo snackbar is the only safety net.

**Adding a coin** is done on the add/search screen (§6) which returns a `MyCryptocurrency` with
its amount and computed values; the portfolio screen upserts it (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:222-232`,
`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:350-403`, `app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:168-170`) with `updateAmount = true`, so
adding a coin that is **already in the portfolio replaces its amount** rather than adding to it
(`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:103-105`). This is the behaviour issue #10 asks to change.

**Header column labels and the spinner.** The spinner lists the 93 codes
(`app/src/main/res/layout/activity_main.xml:158-168`) and is set to the stored currency on every creation
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:136`). The app subtitle under the title is `""` in the full flavor and
`DEMO` in the demo flavor (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:66`, `app/src/main/res/values/strings.xml:42-43`, demo flavor `app/src/demo/res/values/strings.xml`).

## 6. Add / search screen

**pinned by** `AmountValidationTest` (the validator rule). Add/search screens and the amount dialog are not part of the public screenshot subset.

**Source of truth** is the `all_cryptocurrencies` table, `ORDER BY rank ASC`
(`app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:29-30`). The screen fetches `listings/latest` **only when the table is
empty** (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchViewModel.kt:33-34`, `app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:118,131-133,140-149`:
an empty table is reported as `null` data, and `null` data means fetch); otherwise it shows the
table as it is. A refresh (swipe, or the Retry button/snackbar) fetches unconditionally after the
1 000 ms delay (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchViewModel.kt:56-69`).

**A successful listing fetch replaces the whole table** — `DELETE` everything, then insert the
new rows (`app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:48-52`) — and **re-prices the portfolio rows** whose id appears in
the listing (`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:134-151`): the coin's data, currency and timestamp are
replaced and the two computed values are recomputed. Portfolio coins outside the top 5 000 keep
their old row.

**Visible states** (`app/src/main/res/layout/content_add_search.xml`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:218-263`):

| State | What shows |
|---|---|
| loading, nothing cached | a centred progress bar; no list, no info bar |
| data | the info bar "Last updated (<date> UTC)" (`app/src/main/res/values/strings.xml:312`; the date is the first row's `lastFetchedDate`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:235-241`) and the ranked list |
| refreshing | the swipe spinner over the list; the search action disabled (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:117,140,222`) |
| error, nothing cached | a **Retry** button and "Unable to get data. Please press retry button to try again." (`app/src/main/res/layout/loading_state.xml:51-67`, `app/src/main/res/values/strings.xml:50`) |
| error over data | the list stays; indefinite snackbar "Unable to refresh." with Retry (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:245-255`) |
| search | the info bar becomes "Results N" (`app/src/main/res/values/strings.xml:313`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:327,338`); swipe-to-refresh disabled while the search field is open (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:351,358`) |

**Search** runs against the database with `name LIKE :text OR symbol LIKE :text`
(`app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:59-60`), with the typed text wrapped in `%…%`
(`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:335-337`), 500 ms after the last keystroke
(`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:39`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:320-329`) or immediately on submit (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:313-316`). SQLite's `LIKE` is
case-insensitive for ASCII letters only; `%` and `_` in the typed text act as wildcards (U6). The
search text is kept across rotation (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:133-140`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:295-298`).

**Each row** shows rank, the coin image from
`https://s2.coinmarketcap.com/static/img/coins/128x128/<id>.png` (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:36-38`,
`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchListAdapter.kt:79-82`) with the first 3 symbol characters as the fallback,
the name and the symbol (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchListAdapter.kt:72-99`).

**The amount dialog** (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/CryptocurrencyAmountDialog.kt`, opened by a tap on a row,
`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:195-210`):

- title "How many <name> coins do you have?", hint "Enter amount", buttons **OK** (positive) and
  **Cancel** (neutral), error "Valid number is required!" (`app/src/main/res/values/strings.xml:322-326`);
- the keyboard opens immediately (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/CryptocurrencyAmountDialog.kt:133`); the input type is `numberDecimal`
  (`app/src/main/res/layout/dialog_add_crypto_amount.xml:29`). On the device that input type installs a
  digits filter in front of the field: only digits and one `.` get in, and a typed `-`, `,`,
  `e` or a second `.` is dropped before the validator ever sees it — `1.2.3` arrives as `1.23`,
  `1,5` as `15` (observed on the Android 14 emulator in English and in Lithuanian; the keyboard
  shows `-` and `,` keys, the field ignores them);
- **OK is disabled while the field is empty** (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/CryptocurrencyAmountDialog.kt:137-139`, `app/src/main/java/com/baruckis/kriptofolio/utilities/ExtensionsValidation.kt:42-48`);
- OK validates with `String.toDouble()` (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/CryptocurrencyAmountDialog.kt:161-170`, `app/src/main/java/com/baruckis/kriptofolio/utilities/ExtensionsValidation.kt:51-55`): any text
  Java's `Double.parseDouble` accepts is accepted, anything else shows "Valid number is
  required!" under the field and keeps the dialog open. Because of the filter above, the only
  keyboard input that reaches the validator and fails is a lone `.`; `0` is accepted and stores
  a zero holding. What the validator *would* accept if the filter were not there (`-5`, `1e3`,
  `Infinity`, `NaN`) is recorded as K8 and pinned by `AmountValidationTest`;
- Cancel, tapping outside and Back all discard the selection (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:121-124`, `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:155-158`,
  `app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:188-192`);
- OK computes `amountFiat` and `amountFiatChange24h` from the row's stored price and 24 h change
  (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:164-175`), returns the coin to the portfolio screen and **closes the
  add/search screen** (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/AddSearchActivity.kt:177-185`). One coin per visit.

## 7. Settings

**pinned by** `SettingsKeysTest`

Stored in the default `SharedPreferences` (`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/AppModule.kt:111`, file
`shared_prefs/com.baruckis.kriptofolio_preferences.xml` in the app's private data directory at runtime (not a repository file)). The keys are string resources marked
`translatable="false"`; the **defaults are ordinary, translatable strings** and differ by
language:

| Setting | Key (exact string) | Type | Default en / sw | Default lt | Default iw (he) | Values |
|---|---|---|---|---|---|---|
| language | `preference language` (`app/src/main/res/values/strings.xml:337`) | String | `EN` (`app/src/main/res/values/strings.xml:340`) / `SW` (`app/src/main/res/values-sw-rKE/strings.xml:90`) | `LT` (`app/src/main/res/values-lt/strings.xml:90`) | `HE` (`app/src/main/res/values-iw/strings.xml:90`) | `EN`, `HE`, `LT`, `SW` (`app/src/main/res/values/strings.xml:349-354`) |
| fiat currency | `preference fiat currency` (`app/src/main/res/values/strings.xml:358`) | String | `USD` (`app/src/main/res/values/strings.xml:361`) / `USD` (`app/src/main/res/values-sw-rKE/strings.xml:95`) | `EUR` (`app/src/main/res/values-lt/strings.xml:95`) | `ILS` (`app/src/main/res/values-iw/strings.xml:95`) | the 93 codes (`app/src/main/res/values/strings.xml:459-553`) |
| date format | `preference date format` (`app/src/main/res/values/strings.xml:557`) | String | `dd/MM/yyyy` (`app/src/main/res/values/strings.xml:560`) / `dd/MM/yyyy` (`app/src/main/res/values-sw-rKE/strings.xml:196`) | `yyyy-MM-dd` (`app/src/main/res/values-lt/strings.xml:196`) | `dd/MM/yyyy` (`app/src/main/res/values-iw/strings.xml:196`) | `dd/MM/yyyy`, `MM/dd/yyyy`, `yyyy-MM-dd` (`app/src/main/res/values/strings.xml:568-572`) |
| 24-hour time | `preference 24h switch` (`app/src/main/res/values/strings.xml:576`) | Boolean | `true` (`app/src/main/res/xml/pref_main.xml:51`; code default `true`, `app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:194,200`) | `true` | `true` | — |

The defaults are materialized into the preference file **once, on the first launch of the main
screen**, from `app/src/main/res/xml/pref_main.xml` in the language the app resolves at that moment
(`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainActivity.kt:60`, `PreferenceManager.setDefaultValues(…, false)`; the marker file
`_has_set_default_values.xml` (Android-generated marker in the app data directory, not a repository file) records that it happened). A device whose system language is
Lithuanian therefore starts with `LT`/`EUR`/`yyyy-MM-dd`; an English device with
`EN`/`USD`/`dd/MM/yyyy`; a Swahili device with `SW`/`USD`/`dd/MM/yyyy`
(`app/src/main/res/values-sw-rKE/strings.xml:90,95,196`). When a key is missing at
read time, the code falls back to the default string resolved in the **current** UI language
(`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:181-185,208-212,221-225`).

The other preference keys are actions, not stored values: `rate app`, `share app`,
`preference donate crypto`, `buy me coffee`, `contact`, `website`, `author`, `source`,
`privacy policy`, `third party software`, `license`, `app` (`app/src/main/res/values/strings.xml:585-658`).

**Language** (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:76-107`): choosing the current language does nothing (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:87-89`);
choosing another writes the key, switches the string provider (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:95`,
`app/src/main/java/com/baruckis/kriptofolio/utilities/localization/StringsLocalization.kt:34-42`) and **restarts the app's task** on the
portfolio screen (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:100-102`, `FLAG_ACTIVITY_CLEAR_TASK`). Every activity applies the stored
language to its context on creation and sets `Locale.setDefault` to it
(`app/src/main/java/com/baruckis/kriptofolio/utilities/localization/LocalizationManager.kt:31-63`, `app/src/main/java/com/baruckis/kriptofolio/ui/common/BaseActivity.kt:37-44`,
`app/src/main/java/com/baruckis/kriptofolio/App.kt:57-60`), which is what makes number formatting (§2) follow the app language rather than
the system language. The entries are shown in their own language and never translated
(`app/src/main/res/values/strings.xml:342-347`).

**Fiat currency** (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:110-126`): writes the key; the portfolio screen reacts as described in §5.
The list dialog shows "CODE - Name (sign)" entries (`app/src/main/res/values/strings.xml:363-457`, translated names).

**Date format** (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:129-145`): writes the key; the summary shows the pattern and today's date in it.

**24-hour format** (`app/src/main/res/xml/pref_main.xml:50-56`): a switch; summary `13:00` when on, `01:00 PM` when off
(`app/src/main/res/values/strings.xml:578-579`).

**Support** (`app/src/main/res/xml/pref_main.xml:60-89`): *Rate app in Google Play* opens `market://details?id=` with
the **suffix-stripped** package name, falling back to the web URL (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:148-159`, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:392-416`,
`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:432-436`); *Share with your friends* sends "I suggest this free cryptocurrencies portfolio
Android app for you: " + the Play URL through a chooser (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:162-173`, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:419-430`,
`app/src/main/res/values/strings.xml:591-592`); *Donate with crypto* and *Buy me a coffee* exist **only in the demo
flavor** (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:182`, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:207`, `BuildConfig.IS_DEMO`); *Contact* opens a `mailto:` intent to
`hello@kriptofolio.app` (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:43`, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:439-461`) with the subject
"Feedback Kriptofolio 1.2.3 for Android" (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:225-229`, `app/src/main/res/values/strings.xml:626`; see K9), and a toast if
no email app exists (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:453-459`).

**About** (`app/src/main/res/xml/pref_main.xml:91-134`): *Website*, *Author*, *View source on GitHub* open the URLs in
`app/src/main/res/values/strings.xml:634,639,644` with `ACTION_VIEW` and a toast if nothing handles it (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:354-361`);
*Privacy policy* is coded to open `https://kriptofolio.app/privacy-policy-app` in a Chrome
Custom Tab when Chrome is installed and to fall back to a plain browser intent otherwise
(`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:279-290`, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:363-389`) — but on Android 11 and newer the probe always fails and every device
gets the plain browser (see K16); *Third-party software*
and *License* navigate to the licence screens (§8); the last row shows "Kriptofolio" + the flavor
subtitle as title and the version name as summary, and is not selectable (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:326-333`,
`app/src/main/res/xml/pref_main.xml:128-132`).

## 8. Licence screens

The licence screens are described from source citations; they are not part of the public screenshot subset.

*Third-party software* (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/thirdpartysoft/LibrariesLicensesListFragment.kt`) is a
hard-coded list of **28 libraries** built from string resources
(`app/src/main/java/com/baruckis/kriptofolio/repository/LicensesRepository.kt:34-291`), each card with the library, developer, licence name,
a *Project link* button (browser intent, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/thirdpartysoft/LibrariesLicensesListFragment.kt:110-112,127-134`) and a *Read license* button that opens
the licence text screen (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/thirdpartysoft/LibrariesLicensesListFragment.kt:114-118`). The toolbar's *More* action opens Google's
`OssLicensesMenuActivity`, titled "All libraries licenses", which lists every dependency the
`oss-licenses` Gradle plugin found at build time (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/thirdpartysoft/LibrariesLicensesListFragment.kt:97-101`, `app/src/main/res/values/strings.xml:825`).

*License* opens the same text screen with the app's own Apache 2.0 notice
(`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:312-318`, `app/src/main/java/com/baruckis/kriptofolio/repository/LicensesRepository.kt:298-302`). The text screen
(`app/src/main/java/com/baruckis/kriptofolio/ui/settings/LicenseFragment.kt`) is a scrollable `TextView` titled "License" with the library
name as subtitle (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/LicenseFragment.kt:52-54,63-64`).

## 9. Localization and RTL

**pinned by** the public portfolio data captures in [the PR #23 UI inventory at its candidate HEAD](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md) for English and Hebrew RTL, and `SettingsKeysTest` for per-language defaults. Other screens and locale combinations are not captured in the public subset.

Four languages: English (default), Hebrew (`app/src/main/res/values-iw/`), Lithuanian (`app/src/main/res/values-lt/`),
Swahili (`app/src/main/res/values-sw-rKE/`). Everything in this section describes an install that carries all
four; a Play install may not (K17). The language codes stored and used are `EN`, `HE`, `LT`, `SW`
(`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/LanguageCodes.kt:23-34`); `Locale("HE")` resolves the `values-iw` folder
because Android aliases the two codes. An unknown stored code falls back to English
(`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/LanguageCodes.kt:41-42`).

The app is `supportsRtl="true"` (`app/src/main/AndroidManifest.xml:35`), so in Hebrew every screen is
mirrored: the header shows *fiat / Bitcoin* instead of *Bitcoin / fiat*, list columns run right
to left, the FAB sits bottom-left. Numbers are kept left-to-right inside their cells with
`textDirection="firstStrongLtr"` (`app/src/main/res/values/styles.xml:55,61`, `app/src/main/res/layout/activity_main.xml:87,115,131`).
The system-bar background views use physical `left`/`right` gravity on purpose
(`app/src/main/res/layout/system_bar_backgrounds.xml`).

Strings that are the same in every language are marked `translatable="false"`; the four files are
key-complete for the strings the screens use. `pref_default_language_entry` for Swahili is the
untranslated word "Swahili" (`app/src/main/res/values-sw-rKE/strings.xml:89`).

The app has **no dark theme**: the theme is `Theme.AppCompat.Light.DarkActionBar`
(`app/src/main/res/values/styles.xml:20`) and there is no `values-night` folder, so the system dark mode changes nothing.

## 10. Offline behaviour

**pinned by** the public cached-portfolio refresh-error capture in [the PR #23 UI inventory at its candidate HEAD](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md). Add/search offline states are source-described, not publicly captured here.

| Situation | Portfolio screen | Add/search screen |
|---|---|---|
| no network, tables full | shows the stored rows and totals with the stored date; pull-to-refresh → after 1 s the snackbar "Unable to refresh." with Retry; rows unchanged | shows the cached list with its date; swipe → snackbar "Unable to refresh." |
| no network, tables empty | the empty state; nothing is fetched (§5) | after 1 s the Retry button with "Unable to get data…"; Retry repeats the fetch |
| network back | Retry (or swipe) succeeds and rewrites the rows | Retry succeeds, replaces the table, re-prices the portfolio |

Nothing on either screen says how old the data is beyond the date in the header; there is no
"stale" threshold (U5). The snackbar on the portfolio screen is indefinite and blocks nothing.

## 11. Data persistence

**pinned by** `LegacyDatabaseTest`

`LegacyDatabaseTest` reads fixture files through plain SQLite JDBC. It checks their recorded
schema identity, table definitions, selected rows, stored calculations and preference values. It
does not instantiate Room, open the files through `AppDatabase`, install an old APK, or exercise an
on-device app update or migration; those remain separate checks for a future migration change.

Room database `kriptofolio-db` (`app/src/main/java/com/baruckis/kriptofolio/utilities/Constants.kt:23`), version 1, identity hash
`ad1c80913f23361aa985d56ecf84d645` (`app/schemas/com.baruckis.kriptofolio.db.AppDatabase/1.json`),
opened with `fallbackToDestructiveMigration()` (`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/AppModule.kt:89-95`) — so a build with a
different schema **deletes** the user's data on first open rather than failing. Two tables:

| Table | Row | Key | Notes |
|---|---|---|---|
| `my_cryptocurrencies` | one per portfolio coin: `my_id`, `amount`, `amount_fiat`, `amount_fiat_change_24h` + the embedded coin columns | `my_id` = CoinMarketCap id | `app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrency.kt:26-44` |
| `all_cryptocurrencies` | one per listed coin: `id`, `name`, `rank`, `symbol`, `currency_fiat`, `price_fiat`, three `price_percent_change_*`, `last_fetched_date` | `id` | `app/src/main/java/com/baruckis/kriptofolio/db/Cryptocurrency.kt:29-58`; `rank` is a `Short` (`app/src/main/java/com/baruckis/kriptofolio/db/Cryptocurrency.kt:44`) |

Money and percentages are `REAL` (`Double`); dates are `INTEGER` epoch milliseconds
(`app/src/main/java/com/baruckis/kriptofolio/db/Converters.kt`). `android:allowBackup="false"` (`app/src/main/AndroidManifest.xml:30`): there is no cloud
backup, no export, and the database is the only copy of the portfolio.

The [PR #24 candidate's fixture notes](https://github.com/baruckis/Kriptofolio/blob/3d17901b2831e15ddec707393d7795002d4374ed/app/src/test/resources/db/README.md)
document the two SQLite files in its `app/src/test/resources/db/`: one from version 1.2.1 and one
from 1.2.3, each populated with synthetic portfolio values. The candidate's
[`LegacyDatabaseTest`](https://github.com/baruckis/Kriptofolio/blob/3d17901b2831e15ddec707393d7795002d4374ed/app/src/test/java/com/baruckis/kriptofolio/behaviour/LegacyDatabaseTest.kt)
reads them through SQLite JDBC and checks fixture metadata and selected stored values. This does
not prove that Room opens either file or that an installed app migrates safely; a future schema
change still needs Room and app-update migration tests.

## 12. The demo flavor

The empty demo portfolio screenshot in [the PR #23 UI inventory at its candidate HEAD](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md) shows the local debug screen with
network disabled. It does not demonstrate the normal sandbox request; the sandbox host is retired.

Same code, different constants: application id `com.baruckis.kriptofolio.demo`
(`app/build.gradle:68-73`), toolbar subtitle `DEMO` (`app/src/main/res/values/strings.xml:43`, `app/src/demo/res/values/strings.xml`),
base URL `https://sandbox-api.coinmarketcap.com/` with CoinMarketCap's public sandbox key
(`app/src/demo/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt:25-27`), *Donate with crypto* (a dialog with two
copy-to-clipboard addresses, `app/src/main/java/com/baruckis/kriptofolio/ui/settings/DonateCryptoDialog.kt:74-82`) and *Buy me a coffee*
visible in Settings (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:182,207`), a different launcher icon. Since the sandbox
host no longer resolves, every fetch fails: the demo build today shows the empty portfolio, the
Retry state on the add screen, and never any data (`UPGRADE-NOTES.md` §7).

## 13. Undefined

Things the code does not decide, or decides by accident. Not bugs — gaps a rewrite has to fill
deliberately.

- **U1** `roundValue(null, …)` — `DecimalFormat.format(Object)` with `null` throws
  `IllegalArgumentException`; no caller passes `null` today, but the signature allows it
  (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:50`).
- **U2** A stored fiat code that is not in the 93-entry array (a future version's code, or a hand
  edited preference file) crashes the sign lookup (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:244`) and the
  spinner shows position `-1` (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:497-499`).
- **U3** Network timeouts: OkHttp's defaults (10 s connect / read / write), never configured.
- **U4** A `my_cryptocurrencies` row with `amount IS NULL`: invisible on screen, excluded from the
  ids that are refreshed, still updated by a listing fetch. Nothing writes such a row today.
- **U5** Staleness: no threshold, no indicator beyond the timestamp, no automatic refresh on open.
- **U6** Search semantics beyond ASCII: SQLite `LIKE` is case-sensitive for non-ASCII letters,
  and `%`/`_` typed by the user are wildcards.
- **U7** Two coins with the same symbol `BTC` in `all_cryptocurrencies`: the Bitcoin total uses
  whichever row SQLite returns first for `LIMIT 1` without `ORDER BY`
  (`app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:65`).
- **U8** The Bitcoin total when `all_cryptocurrencies` has no `BTC` row (the add screen was never
  opened, or the table was emptied by K5): the `LiveData` never emits and the header keeps the
  layout default `₿ 0.00000000` (`app/src/main/res/layout/activity_main.xml:112`, `app/src/main/res/values/strings.xml:71`).
- **U9** Concurrent refreshes: a second refresh started while one is in flight (swipe during a
  spinner change) creates a second `NetworkBoundResource`; the last one to finish wins and nothing
  cancels the other (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:317-322`).
- **U10** The `all_cryptocurrencies` table's currency after a currency change on the portfolio
  screen: the listing table stays in the old currency until the add screen refreshes, so the
  Bitcoin price used for the header total (§1) may be in a different currency than the total.
  Observed while recording the 1.2.1 database asset: portfolio rows in EUR, 5 000 cached coins
  still in USD, the ₿ total computed from a USD Bitcoin price.

## 14. Known behaviour (2019)

Found while extracting the contract. Recorded here, pinned by the tests where a test can reach
them, and **not fixed in this stage**. The rewrite decides each one in the pull request that
touches it.

- **K1 — "UTC" labels a local time.** `formatDate` uses the device time zone (`app/src/main/java/com/baruckis/kriptofolio/utilities/FormatUtils.kt:130`)
  while the header and the add screen's info bar say `UTC` (`app/src/main/res/values/strings.xml:67,312`). A user in
  Vilnius sees `15:41:09 UTC` for a snapshot taken at `12:41:09 UTC`. Pinned by
  `FormatUtilsTest`.
- **K2 — 7-day change is formatted with the fiat pattern.** `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:228`
  passes `ValueType.Fiat` for `pricePercentChange7d`, so a 7 d change of `2825.78 %`
  (`Black Phoenix` in the edge-case fixture) shows as `+2,825.78%` while the 1 h and 24 h changes
  next to it would show `+2825.78%`.
- **K3 — 24 h holding change is formatted with the percent pattern.** `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainRecyclerViewAdapter.kt:230` passes
  `ValueType.Percent` for `amountFiatChange24h`, so a change of `1390.68 USD` shows as
  `+1390.68 USD` without the thousands separator that the same row's `7,929.13 USD` has. (The
  designer's own sample text at `app/src/main/res/values/strings.xml:106` expected `+1,053.12 USD`.)
- **K4 — Four wrong currency signs and two malformed entries.** In `fiat_currency_sign_array`
  GEL is `₾"` (a stray quote, `app/src/main/res/values/strings.xml:234`), GHS is `₾₵` (`app/src/main/res/values/strings.xml:235`) and GTQ is `₾Q` (`app/src/main/res/values/strings.xml:236`)
  — a copy-paste run; ZAR is `Rs` (`app/src/main/res/values/strings.xml:282`, the rand sign is `R`). In the settings entries GEL
  reads `(₾))` (`app/src/main/res/values/strings.xml:392`) and SEK reads `( kr)` (`app/src/main/res/values/strings.xml:445`). Pinned by `SettingsKeysTest`.
- **K5 — An HTTP 200 with no `data` empties the coin cache.** `error_code` is never checked
  (§4). For `listings/latest` a `null` `data` is saved as an empty list, which runs
  `DELETE FROM all_cryptocurrencies` (`app/src/main/java/com/baruckis/kriptofolio/db/CryptocurrencyDao.kt:48-52`). The portfolio rows are
  not deleted, but the add screen is left with nothing until the next successful fetch.
- **K6 — An unsupported `convert` code crashes the app.** A quote keyed by an unknown code
  deserializes to a `null` `currency` object (`app/src/main/java/com/baruckis/kriptofolio/api/CryptocurrencyLatest.kt:53-63`), and the mapper
  dereferences it on a background executor (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:263,282`). Pinned by
  `ApiEnvelopeTest` (the null), not by a crash test.
- **K7 — Bitcoin total ignores the Bitcoin row's currency.** `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainViewModel.kt:233-236` divides
  the fiat total by whatever `price_fiat` the `BTC` row of `all_cryptocurrencies` holds, in
  whatever currency that table was last fetched in (U10).
- **K8 — The validator accepts any parseable double**, including `-5`, `1e300`, `Infinity` and
  `NaN` (`app/src/main/java/com/baruckis/kriptofolio/ui/addsearchlist/CryptocurrencyAmountDialog.kt:161-170`), and stores it as typed; only the
  `numberDecimal` input filter in front of it keeps such values out, and that filter is a
  property of the widget, not of the validation (§6). Zero *is* reachable and is accepted.
  Pinned by `AmountValidationTest`. The rewrite must decide the rule, not inherit the accident.
- **K9 — The feedback email subject drops the app subtitle.** `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:225-226`: the
  second line is a dangling expression, so the subject is `Feedback Kriptofolio 1.2.3 for Android`
  rather than including the subtitle. Harmless in the full flavor (its subtitle is empty).
- **K10 — Adding an owned coin overwrites its amount** instead of adding to it
  (`app/src/main/java/com/baruckis/kriptofolio/db/MyCryptocurrencyDao.kt:103-105`). This is issue #10, and 2.0 changes it by decision; the new
  rule is specified in this document before the pull request that implements it.
- **K11 — A settings-screen currency change can strand the preference.** The preference is
  written before the fetch (§5, *Currency change protocol*); a failed fetch leaves the totals at
  `― ― ―` until a refresh succeeds. The header-spinner path does not have this problem. After a
  restart in that state **nothing fetches on its own**: the repository initialises its
  "selected" code from the preference (`app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:57`), so the observer that
  would trigger a refresh sees no change (`app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:183`), and the dashes stay until
  the user pulls to refresh. Observed on the emulator while recording the 1.2.1 database asset.
- **K12 — `SEARCH` intent filter without a handler.** The add screen declares
  `android.intent.action.SEARCH` (`app/src/main/AndroidManifest.xml:70-72`) but never reads the intent; the
  system search dispatch would open the screen with no query.
- **K13 — `max_supply` and five other fields are parsed on every refresh and never used** (§4);
  the API fixture README reports `max_supply` as null for 1,735 of 5,000 records (34.7 %) in the
  full capture, against a non-null field. The committed fixture is a nine-record sample, so this
  prevalence is recorded provenance and cannot be recomputed from that sample alone
  (`app/src/test/resources/api/README.md`, section *Two things the capture revealed about the current code*).
- **K14 — Duplicate `CAD` entity** in the DOCTYPE of `app/src/main/res/values/strings.xml` (`app/src/main/res/values/strings.xml:20`); harmless, the
  second definition is ignored by the XML parser.
- **K15 — Rank is a 16-bit integer** (`app/src/main/java/com/baruckis/kriptofolio/db/Cryptocurrency.kt:44`, `cmcRank.toShort()` at
  `app/src/main/java/com/baruckis/kriptofolio/repository/CryptocurrencyRepository.kt:262`); CoinMarketCap ranks are below 32 767 today.
- **K16 — The privacy policy has not opened in a Chrome Custom Tab since v1.2.1.**
  `isChromeCustomTabsSupported()` asks `packageManager.queryIntentServices()` for an intent
  whose package is pinned to `com.android.chrome` (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:384-389`, package name
  at `app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:45`). Android 11 filters that query for any app targeting SDK 30 or higher unless the
  manifest declares a matching `<queries>` element, and there is none: `grep -c queries` on the
  **merged** manifest of both flavors returns 0, so neither the app nor `androidx.browser 1.2.0`
  contributes one. The probe therefore returns an empty list, the function returns `false`, and
  the only caller — the *Privacy policy* row — takes the plain-browser branch (`app/src/main/java/com/baruckis/kriptofolio/ui/settings/SettingsFragment.kt:379`). Dead
  since **v1.2.1** (2023-12-03), where `targetSdk` went from 29 to 34 in
  `4f727ed "chore: Project setup support for Android 14"`; v1.2.0 (targetSdk 29) was exempt from
  the filtering and did open a Custom Tab. Nothing about it is visible in a build: no error, no
  lint error, only the `QueryPermissionsNeeded` warning. The user-visible difference is a
  browser task with its own tab strip instead of an in-app tab coloured `colorPrimary`, and
  Back leaving the app instead of returning to Settings. Not pinned by a test (no test covers
  this screen).
- **K17 — The in-app language switcher may serve English on a Play install.** The app builds one
  `Resources` per language by copying the configuration and calling
  `createConfigurationContext` (`app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/LocalizationModule.kt:66-71`, one
  `@Provides` per language at `app/src/main/java/com/baruckis/kriptofolio/dependencyinjection/LocalizationModule.kt:35-57`), then reads every string out of that map
  (`app/src/main/java/com/baruckis/kriptofolio/utilities/localization/StringsLocalization.kt:48`). That only works while the resources for
  all four languages are present in the installed app. The release artifact uploaded to Play is an
  App Bundle (`AGENTS.md`, "Build commands": `bundleFullRelease`; `UPGRADE-NOTES.md` §4 records
  the bundle tasks passing), `app/build.gradle` has no `bundle { language { enableSplit = false } }`
  block, and no `com.google.android.play:core` dependency exists anywhere in the project — so
  Play's default applies and a device receives only the language splits it asked for. On such an
  install, picking a language the device does not have should fall back to the default resources
  (English strings) while `Locale.setDefault` still switches number, date and RTL handling
  (`app/src/main/java/com/baruckis/kriptofolio/utilities/localization/LocalizationManager.kt:48-63`), giving English text with the chosen locale's formatting.
  The public Hebrew capture in [the PR #23 UI inventory at its candidate HEAD](https://github.com/baruckis/Kriptofolio/blob/24ece1bf00409129620eea9968b8ca28fee3614b/docs/ui-inventory.md) confirms the local `demoDebug` build displays
  the selected language and RTL layout. **Unverified on a Play install:** that screenshot is not
  from the Play app bundle, so it does not test language-split delivery or this possible fallback.
  Confirming it needs a Play-installed build on a device without the extra locales. Lint reports
  the mechanism as `AppBundleLocaleChanges`.
- **K18 — The main screen's floating action button has no accessibility label.** The
  `FloatingActionButton` at `app/src/main/res/layout/activity_main.xml:189-197` — the add-coin action wired
  at `app/src/main/java/com/baruckis/kriptofolio/ui/mainlist/MainListFragment.kt:206` — declares no `android:contentDescription`, and nothing sets one
  at runtime (a grep for `contentDescription` and `importantForAccessibility` across the Kotlin
  sources returns nothing). A screen reader announces an unlabeled button for the screen's
  primary control, in all four languages. Elsewhere the 2019 code does use the idiom
  deliberately: the decorative images in `app/src/main/res/layout/dialog_donate_crypto.xml` and `app/src/main/res/layout/flipview_front_custom.xml`
  carry `contentDescription="@null"`. The string the label would need already exists in all four
  locales (`activity_add_search_title`). This document has no other accessibility statement:
  nothing else in the app was audited for it in this stage.

## 15. Insights (2.0, proposed)

*Proposed — pending Andrius' confirmation, which the Insights pull request needs before it is
opened. Nothing in this section exists in 1.2.3.* It turns the four privacy-and-scope decisions
for Insights (what leaves the device; on-device first, bring-your-own-key second, nothing third;
one provider; content rules) into what the screen shows, so the feature's tests can be written from
it before its code.

**One screen, one button, one answer.** The screen shows an anonymised summary of the portfolio
(coin symbols, each coin's share of the total in per cent, each coin's 24 h change in per cent —
never an amount, never a fiat value, never a fiat code), a button, and after the button the
generated comment with a fixed disclaimer underneath. The comment is not stored; leaving the
screen discards it.

**What leaves the device.** On the proposed on-device path, nothing. On the proposed BYOK path,
the anonymised summary above and the versioned prompt go from the device straight to the endpoint
the user configured; no Kriptofolio server is involved. The BYOK request-body test must assert
that no absolute amount or fiat code is included.

**Five situations the screen must render:**

| Situation | What the screen shows |
|---|---|
| on-device model available (`checkFeatureStatus` says so) | the summary, the button, the answer; a line saying the answer was generated on this device |
| on-device unsupported, no key configured | the summary, no button; an explanation that this device cannot run the model, and a link to Settings to enter a key for an OpenAI-compatible endpoint; no key is offered by the app |
| on-device unsupported, key configured | the summary, the button, the answer; a line naming the endpoint host the summary was sent to |
| offline | on-device: works as normal; BYOK: the button is disabled with an "offline" explanation; the summary is still shown |
| the model or endpoint returns an error | the summary stays; an error line with Retry; no partial answer |

**Content rules.** The comment is educational — concentration, diversification, what the 24 h
moves mean — and never says buy, sell or hold; the disclaimer is a fixed string shown on every
answer; the prompt is a versioned file in the repository. The demo flavor uses a bundled fake
answer. For BYOK, the configured key is an authentication credential sent to the configured endpoint,
not part of the request body. The proposal requires encrypted on-device storage for that key; it
does not specify a portfolio export, and 1.2.3 has no export feature. These are proposed requirements,
not current app behavior.
