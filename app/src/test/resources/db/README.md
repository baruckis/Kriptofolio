# Real database files from released versions

Two SQLite databases and two preference files, each written by a **released, signed build** of
this app running on a clean emulator, holding a **fictional portfolio**. They are the upgrade
contract for the 2.0 rewrite: whatever the database module becomes, it has to open these files
and read the same rows back, because they are byte-for-byte what a user's phone contains.

| File | Written by | Fiat | Where it came from |
|---|---|---|---|
| `kriptofolio-v1.2.1.db` | 1.2.1 (versionCode 4, 2023-12-03), GitHub release `v1.2.1`, `kriptofolio-v1-2-1-app-full-release.apk` | EUR | `/data/data/com.baruckis.kriptofolio/databases/kriptofolio-db` |
| `kriptofolio-v1.2.1-preferences.xml` | same install | — | `/data/data/com.baruckis.kriptofolio/shared_prefs/com.baruckis.kriptofolio_preferences.xml` |
| `kriptofolio-v1.2.3.db` | 1.2.3 (versionCode 6, 2026-08-21), GitHub release `v1.2.3`, `kriptofolio-v1-2-3-app-full-release.apk` | USD | as above |
| `kriptofolio-v1.2.3-preferences.xml` | same install | — | as above |

Both databases carry Room schema version 1 with identity hash `ad1c80913f23361aa985d56ecf84d645`,
the hash in `app/schemas/com.baruckis.kriptofolio.db.AppDatabase/1.json`. `LegacyDatabaseTest`
opens each with a plain SQLite driver and checks that hash, the `CREATE TABLE` statements against
the schema file, and the rows.

## The portfolio is fictional

Nobody's real holdings are in this repository. Both files contain the same synthetic portfolio,
typed in through the app's own add-coin dialog on the emulator:

| Coin | id | Amount |
|---|---|---|
| Bitcoin | 1 | 0.25 |
| Ethereum | 1027 | 2 |
| Litecoin | 2 | 10 |
| Dogecoin | 74 | 1000 |

Round numbers, four large-cap coins, nothing that resembles anyone's real position. The prices
and percentages in the rows are the live CoinMarketCap values at the minute each file was
recorded (2026-09-03) — real market data, not real user data.

## How each file was made

Exactly this, so it can be repeated for the next version:

1. Boot a fresh emulator: `Kriptofolio_API34` (Android 14, `default` system image, arm64 — a
   *non-Play* image, because pulling `/data/data` needs `adb root`), started with `-wipe-data`.
   The emulator's time zone was `Europe/Vilnius`; that matters only for the screenshots, not for
   the files.
2. Install the APK attached to the GitHub release of that version (`gh release download vX.Y.Z
   -p '*.apk'`). The release APKs carry the production API key, so the app fetches real data.
3. Launch the app; open the add screen (this fills `all_cryptocurrencies` with the 5 000-coin
   listing); add the four coins above through the search field and the amount dialog.
4. Change settings so the preference file carries non-default values:
   - 1.2.1: fiat currency **EUR**, date format `yyyy-MM-dd`, 24-hour format **off**; then return
     to the portfolio screen, **pull to refresh** and wait for the rows to show EUR (the rows are
     re-priced only when a fetch succeeds, and after a cold start nothing fetches on its own —
     see `docs/BEHAVIOUR.md` §5, *Currency change protocol*, and K11); then open the add screen
     and pull to refresh there too, because the `all_cryptocurrencies` table is re-priced only
     by that screen (U10) and would otherwise stay in USD;
   - 1.2.3: fiat currency left at **USD** (the default), date format `MM/dd/yyyy`, 24-hour
     format **off**.
5. `adb shell am force-stop com.baruckis.kriptofolio`, then `adb root` and
   `adb pull /data/data/com.baruckis.kriptofolio/databases/` and
   `.../shared_prefs/com.baruckis.kriptofolio_preferences.xml`.
6. Room uses write-ahead logging, so the pull produces `kriptofolio-db`, `kriptofolio-db-wal` and
   `kriptofolio-db-shm`. On the host: `sqlite3 kriptofolio-db "PRAGMA wal_checkpoint(TRUNCATE);"`
   folds the WAL into the main file. The committed `.db` is that single file; the `-wal` and
   `-shm` companions are not committed. The file header still says WAL mode, which is what Room
   expects and what SQLite recreates on open.
7. Rename to `kriptofolio-vX.Y.Z.db` and `kriptofolio-vX.Y.Z-preferences.xml`.

The first two attempts at the 1.2.1 file were thrown away. In the first, the app had been
stopped between writing the EUR preference and finishing the EUR refresh, leaving the preference
on EUR and every row still in USD. In the second, the portfolio rows were in EUR but the 5 000
cached coins were still in USD, because only the add screen re-prices that table. Both states
are real (a user gets the first when a refresh fails, K11, and the second every time the currency
is changed without visiting the add screen, U10), but a contract file should be unambiguous, so it
was recorded a third time with both refreshes allowed to complete.

| File | Size | `last_fetched_date` (UTC) |
|---|---|---|
| `kriptofolio-v1.2.1.db` | 475 136 bytes | 2026-09-03 13:39:41 |
| `kriptofolio-v1.2.3.db` | 458 752 bytes | 2026-09-03 13:24:39 |

## What is deliberately not here

- The `_has_set_default_values.xml` marker file: one boolean, no information.
- A database from 1.2.2: it has the same code as 1.2.3 apart from the target SDK, and the
  schema is identical across every published version.
- Any file from a real device.
