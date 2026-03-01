# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- Synced `fdroid/com.droidbert.yml` build/version fields to `v0.2.9` (`versionCode 11`) while keeping the configured `AutoUpdateMode` unchanged.

## [0.2.9] - 2026-03-01

### Changed
- Updated `fdroid/com.droidbert.yml` with repaired `AutoUpdateMode` (`Versionv%v`) after F-Droid validation feedback.

## [0.2.8] - 2026-03-01

### Changed
- Updated `fdroid/com.droidbert.yml` and normalized `AutoUpdateMode` to `Version v%v` for tag-based update tracking.

## [0.2.7] - 2026-03-01

### Changed
- Removed embedded metadata from `logo/droidbert.webp` and `app/src/main/res/drawable/droidbert.webp` (EXIF/XMP/ICC stripped) to satisfy build/release checks.

## [0.2.6] - 2026-03-01

### Changed
- Performed end-to-end release validation for workflow guard stability.
- Aligned `fdroid/com.droidbert.yml` to release `0.2.6` (`versionCode 8`, tag `v0.2.6`).
- Added explicit documentation note to keep README/changelog updated with every functional or workflow change.

## [0.2.5] - 2026-03-01

### Changed
- Verified `fdroid/com.droidbert.yml` and aligned F-Droid metadata to release `0.2.4` (`versionCode 6`, tag `v0.2.4`).
- Fixed `Version and Changelog Guard` workflow `release-apk-name-check` step by correcting heredoc indentation in the Python asset-name validation block.
- Fixed `release-apk-name-check` JSON parsing by replacing stdin-based Python parsing with direct `gh --jq` asset-name validation.

## [0.2.4] - 2026-03-01

### Added
- Apache-2.0 project license file.
- F-Droid metadata template `fdroid/com.droidbert.yml`.
- Submission documentation `fdroid/SUBMISSION.md` and `fdroid/FDROIDDATA_QUICKSTART.md`.
- Helper script `fdroid/copy-to-fdroiddata.sh` to copy metadata into a local `fdroiddata` checkout.
- Fastlane metadata for app listing text and changelog under `fastlane/metadata/android/en-US/`.

### Changed
- Release signing config now remains optional when signing env vars are absent, enabling F-Droid/local unsigned release builds.

## [0.2.3] - 2026-03-01

### Changed
- Release workflow now builds a consistently signed `release` APK from repository signing secrets instead of a runner-specific debug-signed APK.
- Documented one-time uninstall requirement for users who installed older builds signed with a different key.

## [0.2.2] - 2026-03-01

### Added
- Custom launcher icon assets using `logo/droidbert.webp` as source.
- New padded logo drawable `ic_launcher_logo_inset.xml` for consistent icon framing.

### Changed
- Adaptive and legacy launcher icon resources now use the Droidbert logo.
- Launcher icon foreground inset adjusted to `20dp` for improved mask-safe centering.

## [0.2.1] - 2026-03-01

### Added
- _Describe changes here._

## [0.2.0] - 2026-03-01

### Added
- _Describe changes here._

### Changed
- Default comic URL changed to `https://dilbert.xo.nl/` because the legacy Dilbert site no longer serves the comics feed.
- Release APK asset naming now uses `droidbert-<version>.apk`.

## [0.1.0] - 2026-03-01

### Added
- Initial Android app scaffold (`droidbert`) with Gradle and Kotlin setup.
- Full-screen WebView that loads the daily Dilbert mobile web experience.
- JavaScript injection that removes the top search UI while keeping comic content.
- In-app loading indicator shown during page loads.
- Back-button navigation inside the WebView.
- GitHub Actions workflow to build and attach a downloadable APK on tags (`v*`).
- Project documentation for running locally and downloading APKs from Releases.
