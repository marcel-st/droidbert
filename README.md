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

- Loads `https://dilbert.com/` in a full-screen WebView.
- Mimics mobile web behavior.
- Hides the search bar/header search controls via JavaScript after each page load.
- Shows a loading indicator while pages are loading.
- Supports back navigation inside the WebView.

## Download APK (no local build required)

1. Open the GitHub **Releases** page for this repository.
2. Download the `app-debug.apk` asset from the latest release.
3. Install it on your Android device.

## Release process (tagging + APK asset)

This repository includes a GitHub Actions workflow at `.github/workflows/release-apk.yml`.

- Trigger: push a tag matching `v*` (for example `v0.1.0`).
- Result: GitHub builds the app and creates a release with `app-debug.apk` attached.

Example commands:

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

## Changelog

See `CHANGELOG.md` for release notes.
