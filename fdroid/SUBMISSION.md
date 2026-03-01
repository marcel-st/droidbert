# F-Droid submission package

This file provides copy/paste text and a checklist for submitting Droidbert to the official F-Droid `fdroiddata` repository.

## Current app metadata

- App ID: `com.droidbert`
- Current version: `0.2.3`
- Current versionCode: `5`
- Source repository: `https://github.com/marcel-st/droidbert`
- Latest metadata commit: `6511d3a2c758522384e0b24ca5b561ac5fb1e829`
- Latest release tag: `v0.2.3`

## Files to use in fdroiddata

- Metadata file in this repo: `fdroid/com.droidbert.yml`
- Fastlane metadata path: `fastlane/metadata/android/en-US/`

For `fdroiddata`, add/copy to:

- `metadata/com.droidbert.yml`

## Suggested merge request title

`New app: Droidbert (com.droidbert)`

## Suggested merge request description

```markdown
## New app submission: Droidbert (`com.droidbert`)

This MR adds metadata for **Droidbert**, an Android WebView app that shows the daily Dilbert comic from `https://dilbert.xo.nl/` with a simplified reading UI.

### Included

- `metadata/com.droidbert.yml`
- Fastlane metadata in upstream source (`fastlane/metadata/android/en-US`)

### Build details

- Source: `https://github.com/marcel-st/droidbert.git`
- Build system: Gradle (`gradle: yes`)
- Current version: `0.2.3` (`versionCode 5`)
- Commit/tag for current version: `v0.2.3`

### Notes for reviewers

- The app requires network access to load content from `https://dilbert.xo.nl/`.
- No trackers, ads, or non-free binary blobs are bundled in the app source.
- Release signing in upstream CI is optional and environment-driven; F-Droid builds are unaffected.
```

## Pre-submission checklist

- [ ] Verify app builds in a clean environment using the recipe in `metadata/com.droidbert.yml`.
- [ ] Confirm license is correct (`Apache-2.0`) and consistent with `LICENSE` in source repo.
- [ ] Confirm anti-features are not needed (or add if reviewer identifies one).
- [ ] Confirm categories are appropriate (`Internet`, `Reading`).
- [ ] Confirm English metadata text quality (title/short/full description/changelog).
- [ ] Open MR against `https://gitlab.com/fdroid/fdroiddata`.

## Optional local validation commands

If you have a local checkout of `fdroiddata`:

```bash
./gradlew -p /path/to/droidbert app:assembleRelease
fdroid lint metadata/com.droidbert.yml
fdroid checkupdates com.droidbert
fdroid build -v -l com.droidbert
```
