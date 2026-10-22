# Releasing Kriptofolio

How a merged `master` becomes a release on Google Play. Merging a pull request never releases
anything (`AGENTS.md`, "Merged is not released"). This file replaces section 7 of
`UPGRADE-NOTES.md`, which recorded the procedure for 1.2.2.

**Owner only** marks a step that needs the owner's accounts, keys or decision: the CoinMarketCap
key, the signing key, the Play Console, tags, GitHub releases and the release branches. A
contributor or an agent may prepare everything else, but never performs an owner step, and never
holds the key or the signing key.

## Release gates

Three checks that a machine can make. CI runs them on every pull request as the `release-gates`
job of the `check` workflow, so `master` stays releasable. Run them locally the same way:

```bash
python3 tools/release_gates.py
./gradlew assembleFullRelease assembleDemoRelease bundleFullRelease
```

1. The CoinMarketCap key constant of the `full` flavor is empty.
2. Every locale in `fastlane/metadata/android/` has `changelogs/<versionCode>.txt` for the current
   `versionCode`, not empty and at most 500 characters, the limit of Google Play's "What's new".
3. Both flavors assemble in release mode, unsigned, and the `full` bundle that goes to Google Play
   builds.

The gates cannot check the key that goes into the release build, the signature, the upgrade path
on a device, or what users see in the Play Console. The steps below cover those.

## Steps

### 1. Release pull request — owner only

A pull request into `master` (for a 1.3.x hotfix, into `release/1.3`; see step 9) that:

- sets `versionCode` one higher than every `versionCode` ever uploaded to Google Play, from any
  branch including `release/1.3`, and sets `versionName` in `app/build.gradle`;
- adds `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` for every locale
  (currently `en-US` and `lt-LT`). F-Droid and its downstream repositories read these files; a
  missing file means no release notes for those users, as issue #4 showed from 2020 to 2026.

Anyone may draft the changelog text; the owner decides the version and merges. The pull request
needs a green `check` workflow, including `release-gates`.

### 2. Build with the key — owner only

`app/src/full/java/com/baruckis/kriptofolio/utilities/ConstantsFlavor.kt` ships with an empty
`API_SERVICE_AUTHENTICATION_KEY`. A release built from a clean checkout installs, launches and
shows "Unable to get data" on every screen, without any build error. Until 2.0.0, when users enter
their own key, the owner supplies the key to the release build only:

1. Put the real key into the constant.
2. `./gradlew bundleFullRelease`, then sign the bundle with the release key.
3. Put the empty string back, and confirm with `git status` and `python3 tools/release_gates.py`
   that nothing is left.

Forgetting step 1 ships a dead app; forgetting step 3 publishes the key into a public history for
good. `tools/release_gates.py` also works as a local `pre-commit` hook (`.git/hooks/pre-commit`
calling it); hooks are never pushed, so it protects only the clone where it is installed.

### 3. Check the signing certificate — owner only

The bundle must be signed with the same upload key as every earlier release. Compare the
certificate digest with the upload key certificate on the Play Console's "App integrity" page, or
with the previous artifact:

```bash
keytool -printcert -jarfile app-full-release.aab | grep "SHA256:"
apksigner verify --print-certs <previous>.apk | grep "SHA-256 digest"
```

A mismatch cannot be fixed after upload: an update signed with another key cannot replace the
installed app.

### 4. Test the upgrade path on a device — owner only

Install the **previously published** release, create real data in it, turn networking off, and
install the new build over it. With no network, what the screens show can only come from the
existing database. Portfolios exist only on the device, and until the Room KMP step adds a tested
migration, a schema mismatch deletes them silently (`fallbackToDestructiveMigration()`). Never use
a device or emulator that holds a real portfolio.

### 5. Upload with a staged rollout — owner only

Upload the signed bundle to the Play Console and start a staged rollout. If a problem appears,
**halt** the rollout; do not roll back, because the database cannot be downgraded
(`docs/decisions/006`). A compliance requirement counts as met when the Play Console warning is
gone, not when the upload succeeds.

The `demo` flavor is not published: its sandbox API no longer exists, so it can only show an error
state. It must still build (release gate 3).

### 6. Tag and publish the source release — owner only

Tag the release commit `v<versionName>` (for example `v1.3.0`), following the existing tags, and
create the matching GitHub release with the changelog text.

### 7. For 1.3.0 only: the exhibit branch — owner only

At the 1.3.0 release commit, create the branch `modernization-2026`. It is kept forever beside
`legacy` and `Part-1` to `Part-5`, and `legacy...modernization-2026` shows the whole modernization
(`docs/decisions/006`). Add it to the protected branches in the repository settings.

### 8. For 1.3.0 only: the maintenance branch — owner only

Create `release/1.3` from the tag `v1.3.0`. From then on, `master` carries unreleased 2.0.0 work
and is not released from until 2.0.0.

### 9. 1.3.x hotfixes

A hotfix is a pull request into `release/1.3`, reviewed like any other change, then released with
steps 1 to 6 from that branch. The same fix is cherry-picked to `master` in its own pull request,
together with the hotfix's changelog files, so `master` keeps the record of every published
`versionCode`. `master` keeps its own `versionCode`; the 2.0.0 release pull request counts from the
highest one published (step 1).
No 2.0.0 work is released through 1.3.x. Deleting `release/1.3` after 2.0.0 is in production is the
owner's decision.

## After the release

- Confirm in the Play Console that the rollout reached 100 % before announcing anything; the
  article about the modernization is published after 1.3.0 reaches all users
  (`docs/decisions/006`).
- Close the release milestone.
