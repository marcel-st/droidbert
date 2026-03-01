# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- Default comic URL changed to `https://dilbert.xo.nl/` because the legacy Dilbert site no longer serves the comics feed.

## [0.1.0] - 2026-03-01

### Added
- Initial Android app scaffold (`droidbert`) with Gradle and Kotlin setup.
- Full-screen WebView that loads the daily Dilbert mobile web experience.
- JavaScript injection that removes the top search UI while keeping comic content.
- In-app loading indicator shown during page loads.
- Back-button navigation inside the WebView.
- GitHub Actions workflow to build and attach a downloadable APK on tags (`v*`).
- Project documentation for running locally and downloading APKs from Releases.
