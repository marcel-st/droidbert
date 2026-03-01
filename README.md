# Droidbert

Android app that shows the daily Dilbert comic in a mobile-style WebView, with the top search UI removed.

## Requirements

- Android Studio (latest stable)
- Android SDK 35
- JDK 17

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync complete.
3. Run the `app` configuration on an emulator or device.

## Behavior

- Loads `https://dilbert.xo.nl/` in a full-screen WebView.
- Mimics mobile web behavior.
- Hides the search bar/header search controls via JavaScript after each page load.
- Shows a loading indicator while pages are loading.
- Supports back navigation inside the WebView.

## Download APK (no local build required)

1. Open the GitHub **Releases** page for this repository.
2. Download the `droidbert-<version>.apk` asset from the latest release.
3. Install it on your Android device.

## Release process (tagging + APK asset)

This repository includes a GitHub Actions workflow at `.github/workflows/release-apk.yml`.

- Trigger: push a tag matching `v*` (for example `v0.1.0`).
- Result: GitHub builds the app and creates a release with `droidbert-<version>.apk` attached.

Example commands:

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
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

## Changelog

See `CHANGELOG.md` for release notes.
