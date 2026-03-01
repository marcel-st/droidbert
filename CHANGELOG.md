# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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
