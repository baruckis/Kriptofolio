# UI inventory — Kriptofolio 1.2.3, the "before" pictures

*Every screen of the app as shipped in 1.2.3, in every state the code can put it in, in the four
languages it ships, photographed before the 2.0 rewrite replaces the UI. This is the visual half
of the contract; the behavioural half is `docs/BEHAVIOUR.md`.*

The screenshots themselves are **not in this repository** — 105 PNGs of 1080×2400 pixels
belong next to the article material, not in a public code base. This document refers to them
by file name: `<screen>-<state>-<locale>.png`, locale codes `en`, `lt`, `iw` (Hebrew, the
right-to-left locale, named after its resource folder `values-iw`), `sw` (Swahili, `values-sw-rKE`).
The 2.0 screenshot tests (ADR-012) will produce the "after" set with the same names.

## How they were captured

| | |
|---|---|
| App | the **released** 1.2.3 full-flavor APK from GitHub release `v1.2.3` (versionCode 6), plus a locally built `demoDebug` for the demo-only screens |
| Device | emulator `Kriptofolio_API34`: Android 14, Pixel 6 profile (1080×2400, 420 dpi), 3-button navigation, time zone `Europe/Vilnius`, wiped before the run |
| Data | a **synthetic** portfolio typed in through the app: Bitcoin 0.25, Ethereum 2, Litecoin 10, Dogecoin 1000 — the same fictional portfolio the Stage 1 database assets hold; prices are live CoinMarketCap values of 2026-09-03 |
| Locale switching | through the app's own Settings → Language, which is how a user does it; the system locale stayed `en-US` |
| Network states | `svc wifi disable` + `svc data disable` for "no network"; the emulator console's `network speed gsm` to hold a loading state long enough to photograph |
| Automation | `adb` + `uiautomator dump`, driven by a script in the author's private workspace; animations off (`*_animation_scale 0`) |
| Date | 2026-09-03 |

Android 14 was chosen on purpose: it is the newest version on which the 1.2.3 layout is still
drawn *inside* the system bars, which is how the app looked to almost every user until Android 15.
The edge-to-edge behaviour on Android 15/16 was photographed during Stage 0.5 and is not repeated
here.

## Screen 1 — Portfolio (`MainActivity` + `MainListFragment`)

The launcher screen: a collapsing header with the totals, a column-header card, the list of
holdings, a FAB. Behaviour: `docs/BEHAVIOUR.md` §5.

| State | Condition (from the code) | What shows | en | lt | iw | sw |
|---|---|---|---|---|---|---|
| empty | `my_cryptocurrencies` has no rows (`fragment_main_list.xml:162`) | header totals at zero, "Your owned crypto coins list is empty! / Add your crypto via the + button below." | `portfolio-empty-en` | `portfolio-empty-lt` | `portfolio-empty-iw` | `portfolio-empty-sw` |
| data | rows present | four cards ordered by holding value, header total in fiat and in ₿, 24 h change on a green/red background, date of the last fetch | `portfolio-data-en` | `portfolio-data-lt` | `portfolio-data-iw` | `portfolio-data-sw` |
| loading (refresh over data) | a fetch in flight after pull-to-refresh (`MainListFragment.kt:114-118`) | the swipe spinner over the list; the currency spinner disabled | `portfolio-loading-en` | `portfolio-loading-lt` | `portfolio-loading-iw` | `portfolio-loading-sw` |
| error (refresh over data) | `Status.ERROR` after a failed fetch (`:449-478`) | the rows stay; indefinite snackbar "Unable to refresh." + RETRY | `portfolio-error-en` | `portfolio-error-lt` | `portfolio-error-iw` | `portfolio-error-sw` |
| multi-select, one item | long press on a card (`:344-358`) | contextual action bar "Selected: 1", black status bar, the card's icon flipped, select-all and delete actions | `portfolio-multiselect-en` | `portfolio-multiselect-lt` | `portfolio-multiselect-iw` | `portfolio-multiselect-sw` |
| multi-select, all | *Select all* (`:277-283`) | "Selected: 4", every icon flipped | `portfolio-selectall-en` | `portfolio-selectall-lt` | `portfolio-selectall-iw` | `portfolio-selectall-sw` |
| undo after delete | *Delete* on a selection (`:284-310`, `:502-536`) | the card gone, totals recomputed, snackbar "Deleted: 1" + UNDO for a few seconds | `portfolio-undo-snackbar-en` | `portfolio-undo-snackbar-lt` | `portfolio-undo-snackbar-iw` | `portfolio-undo-snackbar-sw` |
| fiat dropdown | tap on the currency spinner in the header (`activity_main.xml:158-168`) | the 93-code dropdown over the header | `portfolio-fiat-dropdown-en` | `portfolio-fiat-dropdown-lt` | `portfolio-fiat-dropdown-iw` | `portfolio-fiat-dropdown-sw` |
| totals undefined | rows priced in another currency than the selected one (`MainViewModel.kt:146-150`) | `― ― ―` in place of both totals | `portfolio-nan-mixed-currency-en` | — | — | — |
| loading, nothing yet | `LOADING` with `data == null` (`loading_state.xml:35-49`) | a progress bar under the column card | *not captured, see below* | | | |
| data, other settings | date format `MM/dd/yyyy`, 24-hour off | the header date in the other pattern with the `PM` word (`FormatUtils.kt:114-133`) | `portfolio-data-after-settings-en` | — | — | — |
| landscape | rotation | the same screen, header shorter | `portfolio-data-landscape-en` | — | — | — |
| system dark mode | `cmd uimode night yes` | **identical to light** — the app has no dark theme (`styles.xml:20`) | `portfolio-data-systemdark-en` | — | — | — |

In Hebrew the whole screen mirrors: totals read *fiat / ₿*, the column card runs right to left,
the FAB is bottom-left, and every number stays left-to-right inside its cell
(`textDirection="firstStrongLtr"`). `portfolio-data-iw` is the reference RTL picture of the app.

## Screen 2 — Add / search (`AddSearchActivity`)

Reached from the FAB. A ranked list of all coins with a search action in the toolbar; tapping a
row opens the amount dialog. Behaviour: `docs/BEHAVIOUR.md` §6.

| State | Condition (from the code) | What shows | en | lt | iw | sw |
|---|---|---|---|---|---|---|
| loading, nothing cached | first open with an empty `all_cryptocurrencies` table (`AddSearchViewModel.kt:33-34`) | a progress bar in the middle of an otherwise empty screen; no info bar | `add-loading-nodata-en` | — | — | — |
| error, nothing cached | no network on that first open (`loading_state.xml:51-67`) | a RETRY button and "Unable to get data. Please press retry button to try again." | `add-error-nodata-en` | `add-error-nodata-lt` | `add-error-nodata-iw` | `add-error-nodata-sw` |
| data | table filled | the info bar "Last updated (<date> UTC)" and the ranked list with icons | `add-data-en` | `add-data-lt` | `add-data-iw` | `add-data-sw` |
| refreshing | pull-to-refresh over data (`AddSearchActivity.kt:113-121`) | the swipe spinner; the search action disabled | `add-refreshing-en` | `add-refreshing-lt` | `add-refreshing-iw` | `add-refreshing-sw` |
| error over data | no network + pull-to-refresh (`:245-255`) | the list stays; snackbar "Unable to refresh." + RETRY | `add-error-withdata-en` | `add-error-withdata-lt` | `add-error-withdata-iw` | `add-error-withdata-sw` |
| search | the search action expanded, "bit" typed (`:304-344`) | the info bar becomes "Results N", the list filtered | `add-search-en` | `add-search-lt` | `add-search-iw` | `add-search-sw` |
| amount dialog | tap on a row (`CryptocurrencyAmountDialog.kt`) | "How many Bitcoin coins do you have?", an empty numeric field with the keyboard up, CANCEL and a disabled OK | `add-dialog-amount-en` | `add-dialog-amount-lt` | `add-dialog-amount-iw` | `add-dialog-amount-sw` |
| amount dialog, invalid | a lone `.` and OK (`:161-170`) | the field's error bubble "Valid number is required!" | `add-dialog-amount-error-en` | `add-dialog-amount-error-lt` | `add-dialog-amount-error-iw` | `add-dialog-amount-error-sw` |

The first-open loading state exists once per install and was captured only in English; the
other three locales were switched to after the table was already filled, and the code never
shows that spinner again (`CryptocurrencyRepository.kt:131-133`).

## Screen 3 — Settings (`SettingsActivity` + `SettingsFragment`)

Reached from the overflow menu. Three preference categories. Behaviour: `docs/BEHAVIOUR.md` §7.

| State | Condition | What shows | en | lt | iw | sw |
|---|---|---|---|---|---|---|
| general (top) | screen opened | Language, Fiat currency (with today's summary), Date format (with today's date), the 24-hour switch; then Support | `settings-general-en` | `settings-general-lt` | `settings-general-iw` | `settings-general-sw` |
| about (bottom) | scrolled to the end | Website, Author, View source, Privacy policy, Third-party software, License, the version row | `settings-about-en` | `settings-about-lt` | `settings-about-iw` | `settings-about-sw` |
| language dialog | tap Language (`pref_main.xml:20-28`) | a radio list: English, עִברִית, Lietuvių, Swahili — never translated | `settings-dialog-language-en` | `settings-dialog-language-lt` | `settings-dialog-language-iw` | `settings-dialog-language-sw` |
| fiat dialog | tap Fiat currency (`:30-38`) | the 93-entry radio list "CODE - Name (sign)" | `settings-dialog-fiat-en` | `settings-dialog-fiat-lt` | `settings-dialog-fiat-iw` | `settings-dialog-fiat-sw` |
| date format dialog | tap Date format (`:40-48`) | three patterns | `settings-dialog-dateformat-en` | `settings-dialog-dateformat-lt` | `settings-dialog-dateformat-iw` | `settings-dialog-dateformat-sw` |
| support (demo only) | `BuildConfig.IS_DEMO` (`SettingsFragment.kt:182,207`) | *Donate with crypto* and *Buy me a coffee* rows between Share and Contact | `demo-settings-support-en` | — | — | — |
| donate dialog (demo only) | tap Donate with crypto (`DonateCryptoDialog.kt`) | two copy-to-clipboard addresses and a "Got it" button | `demo-settings-dialog-donate-en` | — | — | — |

The full flavor has no dialog on this screen beyond the three list dialogs; every other row
leaves the app (browser, mail, share sheet, Play) and is listed under *not captured*.

## Screen 4 — Licences (`LibrariesLicensesListFragment`, `LicenseFragment`, `OssLicensesMenuActivity`)

Reached from Settings → Third-party software and Settings → License. Behaviour:
`docs/BEHAVIOUR.md` §8.

| State | Condition | What shows | en | lt | iw | sw |
|---|---|---|---|---|---|---|
| library list | Third-party software | 28 cards: library, developer, licence name, *Project link* and *Read license* buttons; toolbar action *More* | `licenses-list-en` | `licenses-list-lt` | `licenses-list-iw` | `licenses-list-sw` |
| one library's text | *Read license* on the first card | the licence text screen titled "License", the library as subtitle | `licenses-library-text-en` | `licenses-library-text-lt` | `licenses-library-text-iw` | `licenses-library-text-sw` |
| all libraries (Google) | *More* (`:97-101`) | Google's `OssLicensesMenuActivity`, "All libraries licenses", a plain list of every dependency | `licenses-oss-menu-en` | `licenses-oss-menu-lt` | `licenses-oss-menu-iw` | `licenses-oss-menu-sw` |
| the app's own licence | Settings → License | the same text screen with the Apache 2.0 notice | `licenses-app-text-en` | `licenses-app-text-lt` | `licenses-app-text-iw` | `licenses-app-text-sw` |

## The demo flavor

Same screens, different constants (`docs/BEHAVIOUR.md` §12). Captured from `demoDebug` built
from this commit, installed beside the full flavor:

| State | What shows | File |
|---|---|---|
| portfolio, empty | the empty state with the `DEMO` subtitle under the title | `demo-portfolio-empty-en` |
| add/search, first open | the RETRY state — the sandbox host does not resolve, so the demo build can never load data | `demo-add-error-nodata-en` |
| settings, support | the two donation rows the full flavor hides | `demo-settings-support-en` |
| donate dialog | the two addresses | `demo-settings-dialog-donate-en` |

## Not captured, and why

| State | Why |
|---|---|
| Portfolio "loading, nothing yet" (progress bar under the column card) | the portfolio screen never fetches without rows to fetch for (`CryptocurrencyRepository.kt:93-96`), so this state lasts one frame between `LOADING(null)` and `SUCCESS_DB` on a cold start; the emulator could not catch it. It exists in the code, not in practice. |
| Add/search "loading, nothing cached" in lt, iw, sw | happens once per install (see Screen 2) |
| Add/search "error over data" retry succeeding | a transition, not a state — the result is `add-data-*` |
| Undo restoring the deleted card | the snackbar lasts 2.75 s; the automation tapped UNDO too late every time and the coin was re-added by hand instead. The restored state is `portfolio-data-*`. A human can do it; the script could not. |
| Settings rows that leave the app: Rate (Play Store), Share (share sheet), Contact (mail client), Website / Author / Source (browser), Privacy policy (Chrome Custom Tab) | system UI of other apps, none of which is installed on a `default` emulator image; each falls back to a toast "No application can handle this request." which was seen but is not part of this app's UI |
| Buy me a coffee (demo) | a browser link, same as above |
| The 12-hour time format on screen | a settings change, not a screen state; the effect is pinned by `FormatUtilsTest` |
| Dark theme | there is none (`styles.xml:20`); `portfolio-data-systemdark-en` is the proof — identical to `portfolio-data-en` |
| Android 15/16 edge-to-edge rendering | Stage 0.5 material, `blog-material/screenshots/stage-0.5/` |
| Tablet / large window | the app has no adaptive layout; a tablet shows the phone layout stretched, which the 2.0 Compose UI must not assume either (Play's API 37 resizability requirement) |

## Facts the captures established

Things the pictures showed that reading the code did not, each now a line in
`docs/BEHAVIOUR.md`:

1. **The timestamp says UTC and shows local time.** On the Vilnius emulator the header read
   `15:41:09 UTC` for a snapshot the API stamped `12:41:09Z`. (K1)
2. **The amount field cannot receive an invalid number from the keyboard.** The `numberDecimal`
   input filter drops `-`, `,`, `e` and a second `.` before the validator runs; `1.2.3` became
   `1.23` and was accepted — replacing the existing Bitcoin amount, which is issue #10 seen
   live. The only way to reach "Valid number is required!" is a lone `.`. (K8, K10)
3. **A Lithuanian comma is dropped, not rejected.** Typing `1,5` in the Lithuanian UI produced
   `15`; the decimal separator is the dot in every language, and the Lithuanian keyboard's comma
   key does nothing. (§6)
4. **Undo is a three-second window** with no other confirmation; the delete has already hit the
   database when the snackbar appears. (§5)
5. **No dark theme.** (§9)
6. **The demo build is an error screen.** Every fetch fails on the retired sandbox host; the demo
   never shows data. (§12)
7. **Changing currency from Settings can strand the preference.** Stopping the app between the
   preference write and the refresh left `EUR` in the preferences and `USD` in every row; the
   totals showed `― ― ―` until the next refresh. (K11, `portfolio-nan-mixed-currency-en`)

## Counts

| | |
|---|---|
| screens | 4 (+ Google's licence activity) |
| screen × state combinations photographed | 33 |
| locales | 4, one RTL |
| files in `before/` | 105 (19 MB) |
| code changes in this pull request | 0 |
