# Droidbert

Android app for reading Dilbert comics with a native interface backed by the Daily Dilbert image API.

## Requirements

- Android Studio (latest stable)
- Android SDK 35
- JDK 17

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync complete.
3. Run the `app` configuration on an emulator or device.

## Behavior

- Loads comic images directly from `https://dilbert.xo.nl/api/comic.php`.
- Shows a native comic reader UI optimized for phones.
- Supports `Latest`, date picker lookup, and previous/next comic navigation.
- Decodes and renders GIF comic images natively.
- Includes a Settings screen to change API base URL at runtime.

## App icon

- Launcher icon uses `logo/droidbert.webp` as source image.
- App resources reference `app/src/main/res/drawable/droidbert.webp` with adaptive and legacy launcher mappings.
- Foreground logo is centered with `20dp` inset padding via `app/src/main/res/drawable/ic_launcher_logo_inset.xml`.
- Logo WebP assets are kept free of embedded EXIF/XMP/ICC metadata for release compliance.

## Download APK (no local build required)

1. Open the GitHub **Releases** page for this repository.
2. Download the `droidbert-<version>.apk` asset from the latest release.
3. Install it on your Android device.

If Android reports the APK is blocked by an installed app, the installed app was signed with a different key. Uninstall the currently installed Droidbert once, then install the new APK.

## Release process (tagging + APK asset)

This repository includes a GitHub Actions workflow at `.github/workflows/release-apk.yml`.

- Trigger: push a tag matching `v*` (for example `v0.1.0`).
- Result: GitHub builds a signed `release` APK and creates a release with `droidbert-<version>.apk` attached.

Required GitHub repository secrets for signing:

- `ANDROID_KEYSTORE_BASE64` (base64-encoded keystore file)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Example commands:

```bash
git tag -a v0.3.0 -m "Release v0.3.0"
git push origin v0.3.0
```

## Automated version bump + changelog workflow

- Run the **Bump Version** workflow (`.github/workflows/bump-version.yml`) manually from GitHub Actions.
- Provide a semantic version (example: `0.2.0`).
- The workflow will:
	- increment `versionCode`
	- set `versionName`
	- add a new dated section in `CHANGELOG.md`
	- create and push tag `v<version>`

## Changelog/version guard workflow

- The **Version and Changelog Guard** workflow (`.github/workflows/version-changelog-guard.yml`) enforces:
	- `CHANGELOG.md` must be updated when `app/build.gradle.kts` changes in PRs
	- `CHANGELOG.md` must include `## [Unreleased]`
	- pushed release tags (`v*`) must match `versionName`
	- changelog must contain the matching version section
	- completed releases must include APK asset `droidbert-<version>.apk`

## F-Droid metadata

This repository contains metadata and helper files to simplify submission to the official F-Droid `fdroiddata` repository.

- App metadata template: `fdroid/com.droidbert.yml`
- Submission checklist and MR text: `fdroid/SUBMISSION.md`
- Quickstart commands: `fdroid/FDROIDDATA_QUICKSTART.md`
- Helper script to copy metadata into a local `fdroiddata` checkout: `fdroid/copy-to-fdroiddata.sh`
- Fastlane listing metadata: `fastlane/metadata/android/en-US/`

Keep `Builds`, `CurrentVersion`, and `CurrentVersionCode` in `fdroid/com.droidbert.yml` aligned with `app/build.gradle.kts` and the latest release tag.
Use `AutoUpdateMode: Version +v%v` with `UpdateCheckMode: Tags` for tag-based F-Droid update detection.
Current metadata is aligned to `v0.3.3` / `versionCode 18`.

Example helper usage:

```bash
./fdroid/copy-to-fdroiddata.sh /path/to/fdroiddata
```

## Changelog

See `CHANGELOG.md` for release notes.

Maintenance policy: every functional or workflow/configuration change should include matching updates to `README.md` and `CHANGELOG.md`.
