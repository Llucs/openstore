# OpenStore - Project Memory

## Architecture
- **API Layer**: `com.llucs.openstore.api.FdroidApiService` - Central F-Droid API operations (index download, APK download, icon URL resolution, repo probing)
- **Data Layer**: Room database (`AppDatabase`) with entities (`AppEntity`, `RepoEntity`, `VersionEntity`) and DAOs
- **Repository Layer**: `RepoRepository` for data orchestration, `TokenManager` for secure storage
- **UI Layer**: Jetpack Compose with Material 3, MVVM pattern using AndroidViewModel + StateFlow

## F-Droid API Usage Rules
- Repository index is downloaded as a signed JAR (`index-v1.jar`), verified via JAR signature (SHA-256 fingerprint matching)
- The extracted `index-v1.json` contains all apps, packages, and versions
- Always normalize URLs to end with `/` before appending paths
- Icon URLs are resolved via candidate fallback chain: `icons-{640,480,320,240,160,128,96,72,48}/` + `icons/` + raw path
- APK download always includes SHA-256 verification before marking as complete
- Repository fingerprint verification is mandatory for trusted repos

## Known Limitations
- Single version per app stored (latest only, by versionCode)
- No incremental sync (full replace on each sync)
- No index-v2 format support (v1 only)
- Category filtering uses keyword matching, not official F-Droid categories
- No multi-language support beyond Portuguese (pt-BR default)

## Design Decisions
- Kotlin 2.0.x with Compose Compiler Plugin (avoids manual compiler extension versioning)
- Jetpack Compose BOM for unified Compose dependency management
- Material 3 with dynamic color (Material You) on Android 12+
- Custom rounded corner Shapes for consistent UI identity
- Custom Typography with SemiBold headlines
- EncryptedSharedPreferences for token/credentials storage
- WorkManager for background repo sync with periodic schedule (12h)
- Navigation via Jetpack Navigation Compose with bottom bar (Store/Repos/About/Settings)

## Token System
- Auth token stored via EncryptedSharedPreferences (AndroidX Security Crypto)
- Token passed as HTTP Bearer header in authenticated API requests
- Settings screen for token input, visible only from About/overflow menu
- No hardcoded secrets anywhere in the codebase

## Color & Theming
- Dynamic color (Material You) used on Android 12+ (API 31)
- Fallback to light/dark colorScheme on older devices
- Surface container colors for cards (`surfaceContainerLow`, `surfaceContainer`)
- Transparent status/nav bars with edge-to-edge rendering
