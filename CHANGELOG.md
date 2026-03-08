# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.3.6] - 2026-03-08

### Fixed
- Improved legacy fallback root resolution for self-hosted deployments under subpaths (for example `/daily_dilbert/`) so `get_comics.php` and `/comics/...` are resolved correctly.
- Added explicit server-down messaging when the configured host returns plain `404 page not found` for both API and legacy endpoints.

## [0.3.5] - 2026-03-08

### Changed
- Release refresh: published a new APK build from the latest stable code and metadata.

## [0.3.4] - 2026-03-08

### Fixed
- Added automatic fallback for servers that do not expose `api/comic.php` by using `get_comics.php` + `/comics/...` image paths.
- Fixed comic loading on `dilbert.xo.nl` where the API route returns `404`, including latest/date/prev/next navigation.

## [0.3.3] - 2026-03-08

### Fixed
- First launch now tries `1989-04-16` and automatically falls back to latest available comic if that date is not present on the configured API server.
- Improved `404` handling so date-not-found is only shown for real API "comic not found" responses.

## [0.3.2] - 2026-03-08

### Fixed
- App startup now opens the first historical comic (`1989-04-16`) on first launch instead of trying latest/today.
- App now stores the last successfully viewed comic date and resumes from that comic when reopened.

## [0.3.1] - 2026-03-08

### Fixed
- Improved API error handling so missing or invalid API endpoints no longer show only a generic network message.
- Added explicit guidance on-screen when the configured API endpoint returns `404` for latest comic loading.
- Added API URL normalization so entering a site root in Settings automatically resolves to `/api/comic.php`.

## [0.3.0] - 2026-03-08

### Added
- Native comic reader UI with Material 3 layout, loading states, and image-focused reading surface.
- Direct API integration with `api/comic.php` for `latest` and date-based comic loading.
- In-app date picker and previous/next comic navigation by date.
- Runtime Settings screen to configure the API base URL without rebuilding the app.

### Changed
- Replaced the WebView-based implementation with native network/image loading using OkHttp + Coil (GIF support).
- Upgraded build tooling and local build compatibility (AGP 8.6.1, JDK 21 pinning, SDK setup guidance).

## [0.2.12] - 2026-03-01

### Changed
- F-Droid metadata now stores the app summary in `fdroid/com.droidbert/en-US/summary.txt` (instead of `Summary:` in YAML) to satisfy `fdroiddata` `make-summary-translatable.py` checks.
- Updated F-Droid helper docs/scripts to copy localized summary metadata alongside `com.droidbert.yml`.

## [0.2.11] - 2026-03-01

### Changed
- Updated `fdroid/com.droidbert.yml` to use schema-valid `AutoUpdateMode: Version` and aligned release metadata to `v0.2.11` (`versionCode 13`).

## [0.2.10] - 2026-03-01

### Changed
- Synced `fdroid/com.droidbert.yml` build/version fields to `v0.2.10` (`versionCode 12`) while keeping the configured `AutoUpdateMode` (`Version +v%v`) unchanged.

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
