# Kriptofolio 1.2.3 — behaviour specification

*The contract the 2.0 rewrite is measured against. Extracted from the code on `master` at
`238d6b3` (the code that ships as 1.2.3, versionCode 6; nothing under `app/src/main` has changed
since the `v1.2.3` tag). Every statement cites the file and line
that produces the behaviour, in the form `path:line`; paths are relative to `app/src/main/`
unless they start with `app/` or `res/`.*

This document describes **what the app does**, not what it should do. Where the code does
something surprising, the surprise is recorded under *Known behaviour (2019)* and left alone:
this stage adds documents and tests, it does not fix. The rewrite decides each item there with a
note in the same pull request that changes it. Where the code leaves something undefined, it is
listed under *Undefined* rather than guessed.

The characterization tests in `app/src/test` pin the sections marked **pinned by** with a test
class name; a section without that mark is pinned by the UI inventory (`docs/ui-inventory.md`)
or by nothing yet.

---

## 1. Portfolio maths

**pinned by** `CalculateUtilsTest` (the two functions), `LegacyDatabaseTest` (the stored results on real files); the sums and the NaN rule live inside a ViewModel and are pinned by the UI inventory only

Two pure functions are the whole of the arithmetic, and both work on `Double`
(`java/com/baruckis/kriptofolio/utilities/CalculateUtils.kt:24-29`):

| Quantity | Formula | Where |
|---|---|---|
| holding value in fiat | `amount × priceFiat`; `null` amount gives `null` | `CalculateUtils.kt:24-25` |
| holding 24 h change in fiat | `amountFiat × (percentChange24h / 100)`; `null` amountFiat gives `null` | `CalculateUtils.kt:28-29` |

Both values are **stored**, not derived on display: they are written into the
`my_cryptocurrencies` row whenever the coin is added (`ui/addsearchlist/AddSearchActivity.kt:170-174`),
updated from the network (`db/MyCryptocurrencyDao.kt:107-112`) or re-priced from a fresh listing
(`db/MyCryptocurrencyDao.kt:142-146`). The list on screen shows the stored numbers.

**Portfolio total in fiat** is the sum over the user's coins of the stored `amountFiat`, with a
`null` counted as `0.0` (`ui/mainlist/MainViewModel.kt:200-216`). **Portfolio 24 h change** is the
same sum over `amountFiatChange24h` (`MainViewModel.kt:160-176`). Summation is plain `Double`
addition in list order (`MainViewModel.kt:142-154`).

**The NaN rule.** If any coin in the list is priced in a fiat currency other than the currently
selected one, both totals are `NaN` (`MainViewModel.kt:146-150`), and the screen shows
`― ― ―` instead of a number (`MainViewModel.kt:171-173` and `:225-227`, text from
`res/values/strings.xml:76`). This is the state between choosing a new currency and the refresh
that re-prices the coins in it.

**Portfolio total in Bitcoin** is `totalFiat / priceOfBitcoin`, where the Bitcoin price is the row
of the `all_cryptocurrencies` table whose `symbol` is `BTC` (`MainViewModel.kt:179-193`, `:233-236`;
query `db/CryptocurrencyDao.kt:65`, code from `res/values/strings.xml:78`). It is shown with the
`₿` sign (`strings.xml:79`). The Bitcoin row's own fiat currency is **not** checked against the
selected one (see *Known behaviour* K7).

**The "last updated" date** of the portfolio is the `lastFetchedDate` of the first coin in the
list, but only if every coin carries the same date; otherwise it is `null` and the header shows no
date at all (`MainViewModel.kt:252-262`, `ui/mainlist/MainActivity.kt:97-102`).

## 2. Number formatting

**pinned by** `FormatUtilsTest`

Three `DecimalFormat` patterns (`utilities/Constants.kt:24-26`, wrapped by
`utilities/FormatUtils.kt:37-41`):

| `ValueType` | pattern | used for |
|---|---|---|
| `Crypto` | `#,##0.00000000` | coin amounts, the Bitcoin total |
| `Fiat` | `#,##0.00` | prices, holding values, fiat totals, the 24 h fiat change in the header |
| `Percent` | `##0.00` | percentage changes |

`roundValue` (`FormatUtils.kt:50-54`) builds a `DecimalFormat` with the pattern **and the JVM
default locale** (no locale is passed), sets `RoundingMode.DOWN` and formats. So:

- values are **truncated**, never rounded: `0.999999999` BTC shows as `0.99999999`, `-0.005 %`
  shows as `-0.00`, `1.999` USD shows as `1.99`;
- a value smaller than the last digit shows as zero: a price of `2.27e-19` shows as `0.00`, which
  is what the smallest coin in `app/src/test/resources/api/listings-edge-cases.json` looks like on
  screen; the 28 orders of magnitude in that file collapse to "0.00 … 3,741,731,042.48";
- `null` is formatted as `DecimalFormat.format(null)` would — see *Undefined* U1; in practice the
  callers pass a non-null value or substitute `0.0` first (`FormatUtils.kt:72-74`);
- the grouping and decimal separators come from the **default locale**, which the app sets to its
  own UI language on every start (§9). English, Hebrew and Swahili format `1,234.56`; Lithuanian
  formats `1 234,56` with a non-breaking space as the grouping separator. The pattern's grouping
  size (3) and digit counts do not vary by locale.

**Sign and colour** (`getSpannableValueStyled`, `FormatUtils.kt:57-92`):

- `null` is treated as `0.0` (`:72-74`);
- a positive value gets a `+` **appended to the left text** and the green colour
  `colorForValueChangePositive` (`:77-80`, `res/values/colors.xml:29,74` = `#2E7D32`);
- a negative value keeps the `-` that `DecimalFormat` produces and gets the red colour
  `colorForValueChangeNegative` (`:81-83`, `colors.xml:30,73` = `#C62828`);
- zero gets no sign and the neutral list text colour (`:60`, `colors.xml:39`);
- `NaN` is rendered as the caller's `textIfNaN` between `left` and `right`, and is coloured as
  zero because `NaN > 0` and `NaN < 0` are both false (`:76-87`);
- the colour is applied as a foreground span or as a background span depending on the caller
  (`:63-68`); the header's 24 h change uses the background style (`MainViewModel.kt:185-187`),
  every list cell uses the foreground style (`ui/mainlist/MainRecyclerViewAdapter.kt:227-230`).

**What each text on screen is built from** (`MainRecyclerViewAdapter.kt:198-230`,
`MainViewModel.kt:182-246`, `MainActivity.kt:97-123`):

| Text | Construction |
|---|---|
| rank | `rank` as an integer string (`:198`) |
| icon fallback | first 3 characters of the symbol (`:200`, `Constants.kt:29`, `FormatUtils.kt:96-99`) |
| amount | `Crypto(amount) + " " + symbol` (`:224`) |
| price | `Fiat(priceFiat) + " " + currencyCode` (`:225`) |
| holding value | `Fiat(amountFiat) + " " + currencyCode` (`:226`) |
| 1 h / 7 d change | `Percent(change1h) + "%"` + `" / "` + **`Fiat`**`(change7d) + "%"` (`:227-228`, separator `strings.xml:88`) — see K2 |
| 24 h price change | `Percent(change24h) + "%"` (`:229`) |
| 24 h holding change | **`Percent`**`(amountFiatChange24h) + " " + currencyCode` (`:230`) — see K3 |
| header total | `sign + " " + Fiat(total)` or `sign + " ― ― ―"` (`MainViewModel.kt:225-227`) |
| header Bitcoin total | `"₿ " + Crypto(total / btcPrice)` or `"₿ ― ― ―"` (`:242-244`) |
| header 24 h change | `" " + sign + " " [+ "+"] + Fiat(change) + " "` on a coloured background (`:185-187`) |
| header date | `"Total holdings value"` + `" (<date> UTC)"` when a date exists (`MainActivity.kt:98-100`, `strings.xml:66-67`) — see K1 |
| column headers | the sign is substituted into `"Price %1$s\nAmount %2$s"` and `"± amount %1$s 24 h"` (`MainActivity.kt:121-122`, `strings.xml:86-87`) |

**The fiat sign** is looked up by code in two parallel string arrays,
`fiat_currency_code_array` (`res/values/strings.xml:109-203`) and `fiat_currency_sign_array`
(`strings.xml:205-299`), zipped by index (`repository/CryptocurrencyRepository.kt:233-245`). Both
arrays have 93 entries, in the same order as the settings entries and values
(`strings.xml:363-457`, `:459-553`) and as the 93 codes accepted from the API (§3). A code that is
not in the array throws `NoSuchElementException` (`CryptocurrencyRepository.kt:244`, see U2). Four
signs are wrong in the array — see K4.

## 3. Date and time formatting

**pinned by** `FormatUtilsTest`

`formatDate` (`FormatUtils.kt:107-137`) formats a `Date` with the user's date pattern (§7)
followed by a space and a time pattern — `HH:mm:ss` for 24-hour, `hh:mm:ss` for 12-hour
(`Constants.kt:34-35`, `FormatUtils.kt:102-105,128`). In 12-hour mode the localized AM/PM word
(`strings.xml:56-57` and its translations) is appended after another space (`:114-122,133`); the
pattern itself never contains `a`. A `null` date or a `null` pattern gives `""` (`:109`).

`SimpleDateFormat` is created with `Locale.getDefault()` and **no explicit time zone**
(`:130`), so the instant is rendered in the **device's** time zone — while the surrounding label
says `UTC` (`strings.xml:67,312`). See K1.

The date being formatted is the server's `status.timestamp` from the response that last updated
the row (`CryptocurrencyRepository.kt:262-265,281-284`), parsed by Gson's default `Date` adapter
from the ISO 8601 string CoinMarketCap sends, and stored as epoch milliseconds
(`db/Converters.kt:30-37`, schema column `last_fetched_date INTEGER`). It is not the device time
at which the refresh happened.

The settings screen shows a preview of each date pattern applied to today's date, without a time
part (`ui/settings/SettingsFragment.kt:348-352`).

## 4. The CoinMarketCap envelope

**pinned by** `ApiEnvelopeTest`

**Requests.** Two endpoints (`api/ApiService.kt:32-46`), both `GET`, both with the API key in the
`X-CMC_PRO_API_KEY` header added by an interceptor (`api/AuthenticationInterceptor.kt:32-34`,
`Constants.kt:31`):

| Call | Query | Used by |
|---|---|---|
| `v1/cryptocurrency/listings/latest` | `convert=<fiat>&limit=5000` (`Constants.kt:33`) | the add/search screen, §6 |
| `v1/cryptocurrency/quotes/latest` | `convert=<fiat>&id=<comma separated ids>` | the portfolio screen, §5 |

The base URL is `https://pro-api.coinmarketcap.com/` in the full flavor and
`https://sandbox-api.coinmarketcap.com/` in the demo flavor
(`app/src/full/java/.../ConstantsFlavor.kt:25`, `app/src/demo/java/.../ConstantsFlavor.kt:25`).
OkHttp is configured **not** to retry on connection failure (`dependencyinjection/AppModule.kt:63`).
Body logging is on in debug builds only (`AppModule.kt:71`). Timeouts are OkHttp's defaults (U3).

**Response shape.** The body is deserialized with Gson into `CoinMarketCap<T>` — `status`, `data`,
plus three fields (`statusCode`, `error`, `message`) that CoinMarketCap does not send
(`api/CoinMarketCap.kt:25-31`). `status` carries `timestamp` (a `Date`), `error_code`,
`error_message`, `elapsed`, `credit_count` (`CoinMarketCap.kt:33-42`). For `listings/latest`,
`data` is a list; for `quotes/latest`, `data` is a map keyed by the id as a string
(`ApiService.kt:38,46`; the map is flattened to a list before saving,
`CryptocurrencyRepository.kt:65-72`).

**Which fields are read.** Of everything in `CryptocurrencyLatest` (`api/CryptocurrencyLatest.kt:24-46`)
the app uses `id`, `name`, `symbol`, `cmc_rank` and, from the quote, `price`,
`percent_change_1h`, `percent_change_24h`, `percent_change_7d`
(`CryptocurrencyRepository.kt:262-265`). `slug`, `circulating_supply`, `total_supply`,
`max_supply`, `date_added`, `num_market_pairs`, `last_updated`, `volume_24h`, `market_cap` and the
quote's `last_updated` are parsed and never read.

**The quote key.** The quote object is keyed by the fiat code. The app maps it onto one field with
`@SerializedName(value = "USD", alternate = [ … ])` listing the other 92 codes
(`CryptocurrencyLatest.kt:53-63`). The 93 codes are exactly, and in the same order as, the codes
in `res/values/strings.xml:109-203`.

**How the envelope is classified** (`api/ApiResponse.kt:35-75`, `utilities/LiveDataCallAdapter.kt:43-51`):

| What arrives | Classified as | Message |
|---|---|---|
| HTTP 2xx with a body | `ApiSuccessResponse(body)` (`:36-42`) | — |
| HTTP 2xx with no body, or HTTP 204 | `ApiEmptyResponse` (`:38-39`) | — |
| HTTP error with a JSON body | `ApiErrorResponse` (`:43-73`) | `status.error_message` from the body, else the top-level `message`, else the raw body, else the HTTP reason phrase (`:47-71`) |
| HTTP error with a non-JSON body | `ApiErrorResponse` | the raw body if non-empty, else the HTTP reason phrase (`:64-71`) |
| transport failure (no connection, DNS, timeout) | `ApiErrorResponse` (`:31-33`) | the exception message, or `"Unknown error."` |

So `error-401-invalid-key.json` yields the message `This API Key is invalid. ` and
`error-400-invalid-id.json` yields `No data found for 'id': '999999999'`. **The message is never
shown to the user**: the UI shows its own fixed string instead (§5, §6). It is only visible in the
debug log.

**`error_code` is not inspected.** Nothing in the app reads `status.errorCode` or
`status.errorMessage` after a successful HTTP response. An HTTP 200 body with `error_code ≠ 0`
is a success like any other, and what happens next depends only on `data`:

- for the portfolio call, a `null` or empty `data` saves nothing and the screen reports success
  with the rows it already had (`CryptocurrencyRepository.kt:68-76`, `MyCryptocurrencyDao.kt:75-87`);
- for the listing call, a `null` or empty `data` becomes an **empty list**, and saving it deletes
  every row of `all_cryptocurrencies` (`CryptocurrencyRepository.kt:124-127`,
  `CryptocurrencyDao.kt:48-52`) — see K5.

**Null quote for an unsupported currency.** `response-200-unknown-convert.json` is what
`convert=XXX` returns: HTTP 200, `error_code: 0`, and a quote keyed `XXX` with every number
`null`. No alternate name matches `XXX`, so Gson leaves `quote.currency` **null** although the
Kotlin type is non-null. Reading `it.quote.currency.price` while saving then throws a
`NullPointerException` on the disk executor (`CryptocurrencyRepository.kt:263`), which is an
uncaught exception — see K6. It cannot happen while the app's list of 93 codes and CoinMarketCap's
agree, and nothing checks that they do.

**Null `max_supply`.** 34.7 % of real records carry `"max_supply": null` against a non-null
`Double` field (`CryptocurrencyLatest.kt:36-37`). Gson does not assign `null` to a primitive-backed
field, so the value silently becomes `0.0`. Harmless, because the field is never read.

**The timestamp.** `status.timestamp` (e.g. `2026-08-21T15:52:34.024Z`) is the only date the app
stores; it becomes `last_fetched_date` of every row written by that response
(`CryptocurrencyRepository.kt:76,124`).

## 5. Portfolio screen

**pinned by** `docs/ui-inventory.md` (the states and the `― ― ―` totals); the arithmetic behind the totals by `CalculateUtilsTest`

**Source of truth** is the `my_cryptocurrencies` table, read as `LiveData` with
`WHERE amount IS NOT NULL ORDER BY amount_fiat DESC, rank ASC` (`MyCryptocurrencyDao.kt:31-32`).
The list is therefore ordered by holding value, largest first, ties by rank; a row whose
`amount` is `NULL` is invisible (U4).

**On open** the screen shows the database rows without calling the network
(`MainViewModel.kt:98`, `CryptocurrencyRepository.kt:60,79-81`: `shouldFetch` is `false`). The
`NetworkBoundResource` first emits `LOADING(null)`, then `SUCCESS_DB(rows)`
(`repository/NetworkBoundResource.kt:48-63`). Nothing is auto-refreshed on open, however old the
rows are; there is no notion of staleness anywhere in the code (U5).

**Visible states** (`res/layout/fragment_main_list.xml:147-208`, `res/layout/loading_state.xml`,
`ui/mainlist/MainListFragment.kt:415-493`):

| State | Condition | What shows |
|---|---|---|
| loading, nothing yet | `LOADING` and `data == null` | a centred progress bar below the column-header card (`loading_state.xml:35-49`) |
| empty | `data` is an empty list | "Your owned crypto coins list is empty! / Add your crypto via the + button below." (`fragment_main_list.xml:147-174`, `strings.xml:81-82`), header totals `$ 0.00` / `₿ 0.00000000` / `$ 0.00` |
| data | `data` non-empty | the card list; header totals |
| refreshing | a network call in flight over existing data | the swipe-refresh spinner; the currency spinner disabled (`MainListFragment.kt:116,168,197,434`) |
| error | `ERROR` after a fetch | the existing rows stay; an **indefinite** snackbar "Unable to refresh." with a **Retry** action (`MainListFragment.kt:449-478`, `strings.xml:48-49`) |
| multi-select | one or more cards selected | a contextual action bar titled "Selected: N" with *Select all* and *Delete* (`MainListFragment.kt:344-358`, `res/menu/menu_action_mode.xml`, `strings.xml:302-304`); the status bar turns black (`utilities/PrimaryActionModeController.kt:70-87`, `colors.xml:42`); swipe-to-refresh is disabled (`MainListFragment.kt:264-266`) |
| undo | just after a delete | a `LENGTH_LONG` snackbar "Deleted: N" with an **Undo** action (`MainListFragment.kt:502-536`, `strings.xml:53-54`) |

The retry action of the error snackbar starts a new fetch with the same parameters
(`MainListFragment.kt:455-467`); swiping the snackbar away or letting a newer one replace it
abandons a pending currency change (`:468-472`).

**Refresh triggers** — every one of them fetches `quotes/latest` for the ids in the table
(`MainViewModel.kt:324-346`; the ids come from `GROUP_CONCAT(id)`, `MyCryptocurrencyDao.kt:38-39`),
after a fixed **1 000 ms delay** (`Constants.kt:28`, `MainViewModel.kt:307`,
`NetworkBoundResource.kt:118-121`):

1. pull-to-refresh (`MainListFragment.kt:114-118`), enabled only while the app bar is fully
   expanded (`:392-402`);
2. choosing a currency in the header spinner whose code differs from the currency the rows are
   priced in (`:144-173`) — if the rows are already in that currency the preference is written and
   the list is re-read from the database instead (`:164-166`);
3. a fiat currency change made on the settings screen (`:178-202`, same rule);
4. adding a coin whose row is in another currency, or when the rows' fetch dates disagree
   (`MainViewModel.kt:350-403`; the check is `:367-386`, then a 500 ms pause, `:394`);
5. the Retry action of the error snackbar.

**Empty portfolio, no network call.** When there are no ids the "fetch" is short-circuited to
`ApiEmptyResponse` without touching the network (`CryptocurrencyRepository.kt:93-96`,
`NetworkBoundResource.kt:99-106`) and the screen reports `SUCCESS_DB`. An empty portfolio can
never show the error snackbar.

**What a successful refresh writes** (`CryptocurrencyRepository.kt:63-77`,
`MyCryptocurrencyDao.kt:71-117`): for each returned coin the name, rank, symbol, currency, price,
three percentages and the timestamp are copied into the existing row, `amount` is kept, and
`amountFiat` / `amountFiatChange24h` are recomputed (§1). A coin the API did not return keeps its
old row untouched, including its old `currency_fiat` — the NaN rule (§1) then hides the totals
until it is returned again.

**Currency change protocol.** The new code is held in the view model
(`MainViewModel.kt:76,288`) and written to the preference only after the fetch **succeeds**
(`MainListFragment.kt:482-485`, `MainViewModel.kt:436-438`); on failure the spinner snaps back to
the stored currency (`MainListFragment.kt:476-478`). A change from the settings screen is the
other way round: the preference is written first and the fetch follows (`:178-202`), so a failed
fetch leaves the preference on the new currency and the rows in the old one — the NaN state.

**Multi-select and delete** (`MainListFragment.kt:274-316`, `MainRecyclerViewAdapter.kt:113-181`):

- a long press on a card, or a tap on its coin icon, selects it (`MainRecyclerViewAdapter.kt:54-58`,
  `ui/mainlist/MainListItemLookup.kt`); the icon flips to show the selection;
- *Select all* selects every row currently in the adapter (`MainListFragment.kt:277-283`);
- *Delete* removes the selected rows from the list with an animation, closes the action bar,
  shows the empty state if nothing is left, **deletes the rows from the database immediately**
  (`:305`, `MainViewModel.kt:407-416`, `MyCryptocurrencyDao.kt:125-126`) and shows the undo
  snackbar;
- *Undo* re-inserts the same rows with `INSERT OR IGNORE` (`MainListFragment.kt:509-526`,
  `MainViewModel.kt:419-428`, `MyCryptocurrencyDao.kt:66-67`) and restores them at their old
  positions on screen; once the snackbar times out or is swiped away the rows are forgotten
  (`:530-534`);
- selection, the deleted rows and their positions survive rotation (`:234-262`).

There is no confirmation dialog; the undo snackbar is the only safety net.

**Adding a coin** is done on the add/search screen (§6) which returns a `MyCryptocurrency` with
its amount and computed values; the portfolio screen upserts it (`MainListFragment.kt:222-232`,
`MainViewModel.kt:350-403`, `CryptocurrencyRepository.kt:168-170`) with `updateAmount = true`, so
adding a coin that is **already in the portfolio replaces its amount** rather than adding to it
(`MyCryptocurrencyDao.kt:103-105`). This is the behaviour issue #10 asks to change.

**Header column labels and the spinner.** The spinner lists the 93 codes
(`res/layout/activity_main.xml:158-168`) and is set to the stored currency on every creation
(`MainListFragment.kt:136`). The app subtitle under the title is `""` in the full flavor and
`DEMO` in the demo flavor (`MainActivity.kt:66`, `strings.xml:42-43`, flavor `strings.xml`).

## 6. Add / search screen

**pinned by** `docs/ui-inventory.md`, `AmountValidationTest` (the dialog's validator)

**Source of truth** is the `all_cryptocurrencies` table, `ORDER BY rank ASC`
(`CryptocurrencyDao.kt:29-30`). The screen fetches `listings/latest` **only when the table is
empty** (`ui/addsearchlist/AddSearchViewModel.kt:33-34`, `CryptocurrencyRepository.kt:118,131-133,140-149`:
an empty table is reported as `null` data, and `null` data means fetch); otherwise it shows the
table as it is. A refresh (swipe, or the Retry button/snackbar) fetches unconditionally after the
1 000 ms delay (`AddSearchViewModel.kt:56-69`).

**A successful listing fetch replaces the whole table** — `DELETE` everything, then insert the
new rows (`CryptocurrencyDao.kt:48-52`) — and **re-prices the portfolio rows** whose id appears in
the listing (`MyCryptocurrencyDao.kt:134-151`): the coin's data, currency and timestamp are
replaced and the two computed values are recomputed. Portfolio coins outside the top 5 000 keep
their old row.

**Visible states** (`res/layout/content_add_search.xml`, `AddSearchActivity.kt:218-263`):

| State | What shows |
|---|---|
| loading, nothing cached | a centred progress bar; no list, no info bar |
| data | the info bar "Last updated (<date> UTC)" (`strings.xml:312`; the date is the first row's `lastFetchedDate`, `AddSearchActivity.kt:235-241`) and the ranked list |
| refreshing | the swipe spinner over the list; the search action disabled (`:117,140,222`) |
| error, nothing cached | a **Retry** button and "Unable to get data. Please press retry button to try again." (`loading_state.xml:51-67`, `strings.xml:50`) |
| error over data | the list stays; indefinite snackbar "Unable to refresh." with Retry (`:245-255`) |
| search | the info bar becomes "Results N" (`strings.xml:313`, `:327,338`); swipe-to-refresh disabled while the search field is open (`:351,358`) |

**Search** runs against the database with `name LIKE :text OR symbol LIKE :text`
(`CryptocurrencyDao.kt:59-60`), with the typed text wrapped in `%…%`
(`AddSearchActivity.kt:335-337`), 500 ms after the last keystroke
(`Constants.kt:39`, `:320-329`) or immediately on submit (`:313-316`). SQLite's `LIKE` is
case-insensitive for ASCII letters only; `%` and `_` in the typed text act as wildcards (U6). The
search text is kept across rotation (`:133-140`, `:295-298`).

**Each row** shows rank, the coin image from
`https://s2.coinmarketcap.com/static/img/coins/128x128/<id>.png` (`Constants.kt:36-38`,
`ui/addsearchlist/AddSearchListAdapter.kt:79-82`) with the first 3 symbol characters as the fallback,
the name and the symbol (`:72-99`).

**The amount dialog** (`ui/addsearchlist/CryptocurrencyAmountDialog.kt`, opened by a tap on a row,
`AddSearchActivity.kt:195-210`):

- title "How many <name> coins do you have?", hint "Enter amount", buttons **OK** (positive) and
  **Cancel** (neutral), error "Valid number is required!" (`strings.xml:322-326`);
- the keyboard opens immediately (`:133`); the input type is `numberDecimal`
  (`res/layout/dialog_add_crypto_amount.xml:29`). On the device that input type installs a
  digits filter in front of the field: only digits and one `.` get in, and a typed `-`, `,`,
  `e` or a second `.` is dropped before the validator ever sees it — `1.2.3` arrives as `1.23`,
  `1,5` as `15` (observed on the Android 14 emulator in English and in Lithuanian; the keyboard
  shows `-` and `,` keys, the field ignores them);
- **OK is disabled while the field is empty** (`:137-139`, `utilities/ExtensionsValidation.kt:42-48`);
- OK validates with `String.toDouble()` (`:161-170`, `ExtensionsValidation.kt:51-55`): any text
  Java's `Double.parseDouble` accepts is accepted, anything else shows "Valid number is
  required!" under the field and keeps the dialog open. Because of the filter above, the only
  keyboard input that reaches the validator and fails is a lone `.`; `0` is accepted and stores
  a zero holding. What the validator *would* accept if the filter were not there (`-5`, `1e3`,
  `Infinity`, `NaN`) is recorded as K8 and pinned by `AmountValidationTest`;
- Cancel, tapping outside and Back all discard the selection (`:121-124`, `:155-158`,
  `AddSearchActivity.kt:188-192`);
- OK computes `amountFiat` and `amountFiatChange24h` from the row's stored price and 24 h change
  (`AddSearchActivity.kt:164-175`), returns the coin to the portfolio screen and **closes the
  add/search screen** (`:177-185`). One coin per visit.

## 7. Settings

**pinned by** `SettingsKeysTest`

Stored in the default `SharedPreferences` (`AppModule.kt:111`, file
`shared_prefs/com.baruckis.kriptofolio_preferences.xml`). The keys are string resources marked
`translatable="false"`; the **defaults are ordinary, translatable strings** and differ by
language:

| Setting | Key (exact string) | Type | Default en / sw | Default lt | Default iw (he) | Values |
|---|---|---|---|---|---|---|
| language | `preference language` (`strings.xml:337`) | String | `EN` (`:340`) | `LT` (`values-lt/strings.xml:90`) | `HE` (`values-iw/strings.xml:90`) | `EN`, `HE`, `LT`, `SW` (`strings.xml:349-354`) |
| fiat currency | `preference fiat currency` (`:358`) | String | `USD` (`:361`, `values-sw-rKE/strings.xml:95`) | `EUR` (`values-lt:95`) | `ILS` (`values-iw:95`) | the 93 codes (`:459-553`) |
| date format | `preference date format` (`:557`) | String | `dd/MM/yyyy` (`:560`) | `yyyy-MM-dd` (`values-lt:196`) | `dd/MM/yyyy` (`values-iw:196`) | `dd/MM/yyyy`, `MM/dd/yyyy`, `yyyy-MM-dd` (`:568-572`) |
| 24-hour time | `preference 24h switch` (`:576`) | Boolean | `true` (`res/xml/pref_main.xml:51`; code default `true`, `CryptocurrencyRepository.kt:194,200`) | `true` | `true` | — |

The defaults are materialized into the preference file **once, on the first launch of the main
screen**, from `pref_main.xml` in the language the app resolves at that moment
(`MainActivity.kt:60`, `PreferenceManager.setDefaultValues(…, false)`; the marker file
`_has_set_default_values.xml` records that it happened). A device whose system language is
Lithuanian therefore starts with `LT`/`EUR`/`yyyy-MM-dd`; an English or Swahili device starts
with `EN`/`USD`/`dd/MM/yyyy` (`values-sw-rKE/strings.xml:90,95,196`). When a key is missing at
read time, the code falls back to the default string resolved in the **current** UI language
(`CryptocurrencyRepository.kt:181-185,208-212,221-225`).

The other preference keys are actions, not stored values: `rate app`, `share app`,
`preference donate crypto`, `buy me coffee`, `contact`, `website`, `author`, `source`,
`privacy policy`, `third party software`, `license`, `app` (`strings.xml:585-658`).

**Language** (`SettingsFragment.kt:76-107`): choosing the current language does nothing (`:87-89`);
choosing another writes the key, switches the string provider (`:95`,
`utilities/localization/StringsLocalization.kt:34-42`) and **restarts the app's task** on the
portfolio screen (`:100-102`, `FLAG_ACTIVITY_CLEAR_TASK`). Every activity applies the stored
language to its context on creation and sets `Locale.setDefault` to it
(`utilities/localization/LocalizationManager.kt:31-63`, `ui/common/BaseActivity.kt:37-44`,
`App.kt:57-60`), which is what makes number formatting (§2) follow the app language rather than
the system language. The entries are shown in their own language and never translated
(`strings.xml:342-347`).

**Fiat currency** (`:110-126`): writes the key; the portfolio screen reacts as described in §5.
The list dialog shows "CODE - Name (sign)" entries (`strings.xml:363-457`, translated names).

**Date format** (`:129-145`): writes the key; the summary shows the pattern and today's date in it.

**24-hour format** (`pref_main.xml:50-56`): a switch; summary `13:00` when on, `01:00 PM` when off
(`strings.xml:578-579`).

**Support** (`pref_main.xml:60-89`): *Rate app in Google Play* opens `market://details?id=` with
the **suffix-stripped** package name, falling back to the web URL (`:148-159`, `:392-416`,
`:432-436`); *Share with your friends* sends "I suggest this free cryptocurrencies portfolio
Android app for you: " + the Play URL through a chooser (`:162-173`, `:419-430`,
`strings.xml:591-592`); *Donate with crypto* and *Buy me a coffee* exist **only in the demo
flavor** (`:182`, `:207`, `BuildConfig.IS_DEMO`); *Contact* opens a `mailto:` intent to
`hello@kriptofolio.app` (`Constants.kt:43`, `:439-461`) with the subject
"Feedback Kriptofolio 1.2.3 for Android" (`:225-229`, `strings.xml:626`; see K9), and a toast if
no email app exists (`:453-459`).

**About** (`pref_main.xml:91-134`): *Website*, *Author*, *View source on GitHub* open the URLs in
`strings.xml:634,639,644` with `ACTION_VIEW` and a toast if nothing handles it (`:354-361`);
*Privacy policy* opens `https://kriptofolio.app/privacy-policy-app` in a Chrome Custom Tab when
Chrome is installed, else a plain browser intent (`:279-290`, `:363-389`); *Third-party software*
and *License* navigate to the licence screens (§8); the last row shows "Kriptofolio" + the flavor
subtitle as title and the version name as summary, and is not selectable (`:326-333`,
`pref_main.xml:128-132`).

## 8. Licence screens

**pinned by** `docs/ui-inventory.md`

*Third-party software* (`ui/settings/thirdpartysoft/LibrariesLicensesListFragment.kt`) is a
hard-coded list of **28 libraries** built from string resources
(`repository/LicensesRepository.kt:34-291`), each card with the library, developer, licence name,
a *Project link* button (browser intent, `:110-112,127-134`) and a *Read license* button that opens
the licence text screen (`:114-118`). The toolbar's *More* action opens Google's
`OssLicensesMenuActivity`, titled "All libraries licenses", which lists every dependency the
`oss-licenses` Gradle plugin found at build time (`:97-101`, `strings.xml:825`).

*License* opens the same text screen with the app's own Apache 2.0 notice
(`SettingsFragment.kt:312-318`, `LicensesRepository.kt:298-302`). The text screen
(`ui/settings/LicenseFragment.kt`) is a scrollable `TextView` titled "License" with the library
name as subtitle (`:52-54,63-64`).

## 9. Localization and RTL

**pinned by** `docs/ui-inventory.md` (RTL screenshots), `SettingsKeysTest` (per-locale defaults)

Four languages: English (default), Hebrew (`res/values-iw/`), Lithuanian (`res/values-lt/`),
Swahili (`res/values-sw-rKE/`). The language codes stored and used are `EN`, `HE`, `LT`, `SW`
(`dependencyinjection/LanguageCodes.kt:23-34`); `Locale("HE")` resolves the `values-iw` folder
because Android aliases the two codes. An unknown stored code falls back to English
(`LanguageCodes.kt:41-42`).

The app is `supportsRtl="true"` (`AndroidManifest.xml:35`), so in Hebrew every screen is
mirrored: the header shows *fiat / Bitcoin* instead of *Bitcoin / fiat*, list columns run right
to left, the FAB sits bottom-left. Numbers are kept left-to-right inside their cells with
`textDirection="firstStrongLtr"` (`res/values/styles.xml:55,61`, `activity_main.xml:87,115,131`).
The system-bar background views use physical `left`/`right` gravity on purpose
(`res/layout/system_bar_backgrounds.xml`).

Strings that are the same in every language are marked `translatable="false"`; the four files are
key-complete for the strings the screens use. `pref_default_language_entry` for Swahili is the
untranslated word "Swahili" (`values-sw-rKE/strings.xml:89`).

The app has **no dark theme**: the theme is `Theme.AppCompat.Light.DarkActionBar`
(`styles.xml:20`) and there is no `values-night` folder, so the system dark mode changes nothing.

## 10. Offline behaviour

**pinned by** `docs/ui-inventory.md`

| Situation | Portfolio screen | Add/search screen |
|---|---|---|
| no network, tables full | shows the stored rows and totals with the stored date; pull-to-refresh → after 1 s the snackbar "Unable to refresh." with Retry; rows unchanged | shows the cached list with its date; swipe → snackbar "Unable to refresh." |
| no network, tables empty | the empty state; nothing is fetched (§5) | after 1 s the Retry button with "Unable to get data…"; Retry repeats the fetch |
| network back | Retry (or swipe) succeeds and rewrites the rows | Retry succeeds, replaces the table, re-prices the portfolio |

Nothing on either screen says how old the data is beyond the date in the header; there is no
"stale" threshold (U5). The snackbar on the portfolio screen is indefinite and blocks nothing.

## 11. Data persistence

**pinned by** `LegacyDatabaseTest`

Room database `kriptofolio-db` (`Constants.kt:23`), version 1, identity hash
`ad1c80913f23361aa985d56ecf84d645` (`app/schemas/com.baruckis.kriptofolio.db.AppDatabase/1.json`),
opened with `fallbackToDestructiveMigration()` (`AppModule.kt:89-95`) — so a build with a
different schema **deletes** the user's data on first open rather than failing. Two tables:

| Table | Row | Key | Notes |
|---|---|---|---|
| `my_cryptocurrencies` | one per portfolio coin: `my_id`, `amount`, `amount_fiat`, `amount_fiat_change_24h` + the embedded coin columns | `my_id` = CoinMarketCap id | `db/MyCryptocurrency.kt:26-44` |
| `all_cryptocurrencies` | one per listed coin: `id`, `name`, `rank`, `symbol`, `currency_fiat`, `price_fiat`, three `price_percent_change_*`, `last_fetched_date` | `id` | `db/Cryptocurrency.kt:29-58`; `rank` is a `Short` (`:44`) |

Money and percentages are `REAL` (`Double`); dates are `INTEGER` epoch milliseconds
(`db/Converters.kt`). `android:allowBackup="false"` (`AndroidManifest.xml:30`): there is no cloud
backup, no export, and the database is the only copy of the portfolio.

The two real database files in `app/src/test/resources/db/` (one written by 1.2.1, one by 1.2.3,
both from a synthetic portfolio) are what the 2.0 migration tests open.

## 12. The demo flavor

**pinned by** `docs/ui-inventory.md`

Same code, different constants: application id `com.baruckis.kriptofolio.demo`
(`app/build.gradle:63-66`), toolbar subtitle `DEMO` (`strings.xml:43`, `app/src/demo/res/values/strings.xml`),
base URL `https://sandbox-api.coinmarketcap.com/` with CoinMarketCap's public sandbox key
(`app/src/demo/java/.../ConstantsFlavor.kt:25-27`), *Donate with crypto* (a dialog with two
copy-to-clipboard addresses, `ui/settings/DonateCryptoDialog.kt:74-82`) and *Buy me a coffee*
visible in Settings (`SettingsFragment.kt:182,207`), a different launcher icon. Since the sandbox
host no longer resolves, every fetch fails: the demo build today shows the empty portfolio, the
Retry state on the add screen, and never any data (`UPGRADE-NOTES.md` §7).

## 13. Undefined

Things the code does not decide, or decides by accident. Not bugs — gaps a rewrite has to fill
deliberately.

- **U1** `roundValue(null, …)` — `DecimalFormat.format(Object)` with `null` throws
  `IllegalArgumentException`; no caller passes `null` today, but the signature allows it
  (`FormatUtils.kt:50`).
- **U2** A stored fiat code that is not in the 93-entry array (a future version's code, or a hand
  edited preference file) crashes the sign lookup (`CryptocurrencyRepository.kt:244`) and the
  spinner shows position `-1` (`MainListFragment.kt:497-499`).
- **U3** Network timeouts: OkHttp's defaults (10 s connect / read / write), never configured.
- **U4** A `my_cryptocurrencies` row with `amount IS NULL`: invisible on screen, excluded from the
  ids that are refreshed, still updated by a listing fetch. Nothing writes such a row today.
- **U5** Staleness: no threshold, no indicator beyond the timestamp, no automatic refresh on open.
- **U6** Search semantics beyond ASCII: SQLite `LIKE` is case-sensitive for non-ASCII letters,
  and `%`/`_` typed by the user are wildcards.
- **U7** Two coins with the same symbol `BTC` in `all_cryptocurrencies`: the Bitcoin total uses
  whichever row SQLite returns first for `LIMIT 1` without `ORDER BY`
  (`CryptocurrencyDao.kt:65`).
- **U8** The Bitcoin total when `all_cryptocurrencies` has no `BTC` row (the add screen was never
  opened, or the table was emptied by K5): the `LiveData` never emits and the header keeps the
  layout default `₿ 0.00000000` (`activity_main.xml:112`, `strings.xml:71`).
- **U9** Concurrent refreshes: a second refresh started while one is in flight (swipe during a
  spinner change) creates a second `NetworkBoundResource`; the last one to finish wins and nothing
  cancels the other (`MainViewModel.kt:317-322`).
- **U10** The `all_cryptocurrencies` table's currency after a currency change on the portfolio
  screen: the listing table stays in the old currency until the add screen refreshes, so the
  Bitcoin price used for the header total (§1) may be in a different currency than the total.

## 14. Known behaviour (2019)

Found while extracting the contract. Recorded here, pinned by the tests where a test can reach
them, and **not fixed in this stage**. The rewrite decides each one in the pull request that
touches it.

- **K1 — "UTC" labels a local time.** `formatDate` uses the device time zone (`FormatUtils.kt:130`)
  while the header and the add screen's info bar say `UTC` (`strings.xml:67,312`). A user in
  Vilnius sees `15:41:09 UTC` for a snapshot taken at `12:41:09 UTC`. Pinned by
  `FormatUtilsTest`.
- **K2 — 7-day change is formatted with the fiat pattern.** `MainRecyclerViewAdapter.kt:228`
  passes `ValueType.Fiat` for `pricePercentChange7d`, so a 7 d change of `2825.78 %`
  (`Black Phoenix` in the edge-case fixture) shows as `+2,825.78%` while the 1 h and 24 h changes
  next to it would show `+2825.78%`.
- **K3 — 24 h holding change is formatted with the percent pattern.** `:230` passes
  `ValueType.Percent` for `amountFiatChange24h`, so a change of `1390.68 USD` shows as
  `+1390.68 USD` without the thousands separator that the same row's `7,929.13 USD` has. (The
  designer's own sample text at `strings.xml:106` expected `+1,053.12 USD`.)
- **K4 — Four wrong currency signs and two malformed entries.** In `fiat_currency_sign_array`
  GEL is `₾"` (a stray quote, `strings.xml:234`), GHS is `₾₵` (`:235`) and GTQ is `₾Q` (`:236`)
  — a copy-paste run; ZAR is `Rs` (`:282`, the rand sign is `R`). In the settings entries GEL
  reads `(₾))` (`:392`) and SEK reads `( kr)` (`:445`). Pinned by `SettingsKeysTest`.
- **K5 — An HTTP 200 with no `data` empties the coin cache.** `error_code` is never checked
  (§4). For `listings/latest` a `null` `data` is saved as an empty list, which runs
  `DELETE FROM all_cryptocurrencies` (`CryptocurrencyDao.kt:48-52`). The portfolio rows are
  not deleted, but the add screen is left with nothing until the next successful fetch.
- **K6 — An unsupported `convert` code crashes the app.** A quote keyed by an unknown code
  deserializes to a `null` `currency` object (`CryptocurrencyLatest.kt:53-63`), and the mapper
  dereferences it on a background executor (`CryptocurrencyRepository.kt:263,282`). Pinned by
  `ApiEnvelopeTest` (the null), not by a crash test.
- **K7 — Bitcoin total ignores the Bitcoin row's currency.** `MainViewModel.kt:233-236` divides
  the fiat total by whatever `price_fiat` the `BTC` row of `all_cryptocurrencies` holds, in
  whatever currency that table was last fetched in (U10).
- **K8 — The validator accepts any parseable double**, including `-5`, `1e300`, `Infinity` and
  `NaN` (`CryptocurrencyAmountDialog.kt:161-170`), and stores it as typed; only the
  `numberDecimal` input filter in front of it keeps such values out, and that filter is a
  property of the widget, not of the validation (§6). Zero *is* reachable and is accepted.
  Pinned by `AmountValidationTest`. The rewrite must decide the rule, not inherit the accident.
- **K9 — The feedback email subject drops the app subtitle.** `SettingsFragment.kt:225-226`: the
  second line is a dangling expression, so the subject is `Feedback Kriptofolio 1.2.3 for Android`
  rather than including the subtitle. Harmless in the full flavor (its subtitle is empty).
- **K10 — Adding an owned coin overwrites its amount** instead of adding to it
  (`MyCryptocurrencyDao.kt:103-105`). This is issue #10 and changes in 2.0 by decision (master
  plan §3.1), with its own specification added to this document before PR 2.7.
- **K11 — A settings-screen currency change can strand the preference.** The preference is
  written before the fetch (§5, *Currency change protocol*); a failed fetch leaves the totals at
  `― ― ―` until a refresh succeeds. The header-spinner path does not have this problem.
- **K12 — `SEARCH` intent filter without a handler.** The add screen declares
  `android.intent.action.SEARCH` (`AndroidManifest.xml:70-72`) but never reads the intent; the
  system search dispatch would open the screen with no query.
- **K13 — `max_supply` and five other fields are parsed on every refresh and never used** (§4);
  `max_supply` is `null` in 34.7 % of records against a non-null field.
- **K14 — Duplicate `CAD` entity** in the `strings.xml` DOCTYPE (`strings.xml:20`); harmless, the
  second definition is ignored by the XML parser.
- **K15 — Rank is a 16-bit integer** (`Cryptocurrency.kt:44`, `cmcRank.toShort()` at
  `CryptocurrencyRepository.kt:262`); CoinMarketCap ranks are below 32 767 today.

## 15. Insights (2.0, proposed)

*Proposed — pending Andrius' confirmation of ADR-027 before PR 2.9 (deadline 2026-10-20). Nothing
in this section exists in 1.2.3.* It turns the four ADR-027 answers into what the screen shows, so
the tests for PR 2.9 can be written from it.

**One screen, one button, one answer.** The screen shows an anonymised summary of the portfolio
(coin symbols, each coin's share of the total in per cent, each coin's 24 h change in per cent —
never an amount, never a fiat value, never a fiat code), a button, and after the button the
generated comment with a fixed disclaimer underneath. The comment is not stored; leaving the
screen discards it.

**What leaves the device.** On the on-device path, nothing. On the BYOK path, exactly the
anonymised summary above and the versioned prompt, sent from the device straight to the endpoint
the user configured; no Kriptofolio server is involved. A test asserts that the request body
contains no absolute amount and no fiat code.

**Four situations the screen must render:**

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
answer. The BYOK key is stored encrypted on the device and is excluded from the portfolio export.
