## Droidbert v0.3.0

Major release: Droidbert is now a native API-based comic reader instead of a WebView wrapper.

### Highlights

- Replaced embedded web rendering with a native Material 3 reader UI.
- Added direct comic loading from `api/comic.php`:
  - latest comic via `latest=1`
  - specific comic via `date=YYYY-MM-DD`
- Added in-app date picker and previous/next navigation.
- Added Settings screen to configure API base URL at runtime.
- Added native GIF decoding/rendering for comic images.

### Technical changes

- Networking: OkHttp-based API requests.
- Image loading: Coil with GIF support.
- Architecture: Removed WebView-based page manipulation and JavaScript injection.
- Tooling: Updated Android Gradle Plugin to 8.6.1 and standardized local build compatibility.

### Documentation and metadata updates

- Updated README and changelog for the native app architecture.
- Updated fastlane metadata and changelog entry for versionCode 15.
- Updated F-Droid metadata and summary to reflect native API-driven behavior.

### Versioning

- versionName: `0.3.0`
- versionCode: `15`
- Git tag: `v0.3.0`

### Validation

- `./gradlew :app:compileDebugKotlin` passed.
