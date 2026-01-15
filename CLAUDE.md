# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FieldTask5 is a fork of ODK Collect with custom "smap" functionality. It's an Android data collection application built with a multi-module Gradle architecture supporting 16 product flavors for different organizations.

**Key Facts:**
- Base application ID: `org.smap.smapTask.android`
- Version code starts at 7000 (smap versioning)
- Min SDK: 21 (Android 5.0), Target SDK: 35 (Android 15)
- Custom JavaRosa library: `5.1.4-smap` (in `.local-m2/` Maven repo)

## Build Commands

### Building the App

```bash
# Build debug APK for standard flavor
./gradlew assembleStandardDebug

# Build debug APK for fieldTaskMax flavor
./gradlew assembleFieldTaskMaxDebug

# Build release APK (requires signing configuration)
./gradlew assembleStandardRelease

# Build all variants
./gradlew build
```

### Running Tests

```bash
# Run all local unit tests (Robolectric + JUnit)
./gradlew test

# Run unit tests for specific flavor
./gradlew testStandardDebugUnitTest

# Run instrumented tests on connected device/emulator
./gradlew connectedStandardDebugAndroidTest

# Run all connected tests (all flavors)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "org.odk.collect.android.SpecificTest"
```

### Code Quality

```bash
# Run all code quality checks (PMD, ktlint, Checkstyle, Lint)
./gradlew checkCode

# Run individual checks
./gradlew ktlintCheck
./gradlew checkstyle
./gradlew pmd
./gradlew lintDebug

# Auto-format Kotlin code
./gradlew ktlintFormat
```

### Other Common Tasks

```bash
# Clean build artifacts
./gradlew clean

# Install debug APK to connected device
./gradlew installStandardDebug

# Uninstall from device
./gradlew uninstallStandardDebug

# Generate dependency tree
./gradlew dependencies

# List all available tasks
./gradlew tasks --all
```

## Custom JavaRosa Dependency

FieldTask5 uses a customized version of JavaRosa (`5.1.4-smap`) with smap-specific enhancements. The built artifact is **checked into version control** in the `.local-m2/` directory, so new developers can build immediately without setting up the JavaRosa repository.

**Location:**
```
.local-m2/org/getodk/javarosa/5.1.4-smap/
├── javarosa-5.1.4-smap.jar    (681KB)
├── javarosa-5.1.4-smap.module
└── javarosa-5.1.4-smap.pom
```

**Dependency Resolution Order:**
1. Project-local repo: `.local-m2/` (checked in, works out of box)
2. User's local repo: `~/.m2/repository/` (for active development)
3. Remote repos: Maven Central, JitPack, etc.

**For Most Developers:**
- No action needed - the custom javarosa is already in the repo
- Build works immediately after clone

**For Developers Modifying JavaRosa:**

If you need to make changes to the custom JavaRosa library:

1. Clone the smap JavaRosa repository (separate from this project)
2. Make your changes in the JavaRosa codebase
3. Build and publish to your local Maven repository:
   ```bash
   cd /path/to/javarosa
   ./gradlew publishToMavenLocal
   ```
4. Gradle will automatically use your local `~/.m2/` version (checked second)
5. Once stable, rebuild and commit updated artifacts to `.local-m2/`:
   ```bash
   cd /path/to/fieldTask5
   cp ~/.m2/repository/org/getodk/javarosa/5.1.4-smap/* .local-m2/org/getodk/javarosa/5.1.4-smap/
   git add .local-m2/
   git commit -m "Update custom javarosa to version X"
   ```

**Note:** The `.local-m2/` directory is explicitly allowed in `.gitignore` despite the name starting with a dot.

## Architecture Overview

### Multi-Module Structure

The project uses 43 Gradle modules organized by feature:

- **collect_app** - Main application module
- **Feature modules**: forms, geo, maps, entities, audio-recorder, qr-code, etc.
- **Platform modules**: androidshared, settings, strings, icons
- **Testing modules**: test-shared, test-forms, androidtest, fragments-test

### Design Patterns

**MVVM with ViewModels:**
- Activities/Fragments handle UI only
- ViewModels manage UI state with LiveData/Flow
- Example: `BlankFormListViewModel`, `FormEntryViewModel`

**Dependency Injection (Dagger 2):**
```kotlin
// In Activity/Fragment
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DaggerUtils.getComponent(this).inject(this)
}
```
- Component: `AppDependencyComponent`
- Module: `AppDependencyModule`
- All injectable dependencies use `@Inject` annotation

**Repository Pattern:**
- `FormsRepository` - Form data access
- `InstancesRepository` - Instance data access
- `ProjectsRepository` - Project management
- Abstracts database/storage details from business logic

**Reactive Programming:**
- Primary: LiveData (lifecycle-aware observables)
- Secondary: Kotlin Flow with `flowOnBackground` scheduler
- Convert Flow to LiveData: `flow.asLiveData()`

### Smap Customizations

Smap-specific code is located in:
- `collect_app/src/main/java/au/smap/fieldTask/` - Java/Kotlin source
- `collect_app/src/smap/res/` - Resource overrides
- Key classes: `SmapLoginActivity`, `SmapMain`, `SplashScreenActivity`

**Important:** Changes annotated with "smap" comments indicate custom functionality that differs from upstream ODK Collect.

## Code Organization

### Source Directories

**Main Application (`collect_app/src/main/java/`):**
- `org.odk.collect.android.activities/` - Activity classes
- `org.odk.collect.android.fragments/` - Fragment implementations
- `org.odk.collect.android.widgets/` - Form question widgets
- `org.odk.collect.android.formentry/` - Form filling logic
- `org.odk.collect.android.database/` - SQLite database layer
- `au.smap.fieldTask/` - Smap customizations

**Test Sources:**
- `src/test/java/` - Local unit tests (289 files, Robolectric + JUnit)
- `src/androidTest/java/` - Instrumented tests (184 files, Espresso)

### Key Configuration Files

- `gradle/libs.versions.toml` - Version catalog for dependencies
- `config/quality.gradle` - Code quality tool configuration
- `secrets.gradle` - API keys (git-ignored, see `secrets.gradle.example`)
- `.local-m2/` - Project-local Maven repo (checked in) containing custom JavaRosa 5.1.4-smap

## Development Guidelines

### Code Style

**Language Preference:**
- Write all new code in Kotlin
- Existing Java code can remain, but migrate when making significant changes

**UI Development:**
- Prefer Jetpack Compose for new UI components
- Legacy XML layouts acceptable for minor changes
- Use Material 3 components: `MaterialButton`, `TextInputLayout`, etc.

**Naming Conventions:**
- Follow existing patterns in the codebase
- ViewModels: `*ViewModel` (e.g., `BlankFormListViewModel`)
- Repositories: `*Repository` (e.g., `FormsRepository`)
- Activities: `*Activity` (e.g., `SmapLoginActivity`)

### Testing Requirements

- Tests required for new features and bug fixes
- Use Robolectric for Android component unit tests
- Use Espresso for UI automation tests
- Mock dependencies with Mockito
- Page Object pattern for Espresso tests (see `collect_app/src/androidTest/java/feature/`)

### Common Patterns

**View Binding:**
```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
```

**ViewModel with LiveData:**
```kotlin
class MyViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data

    fun updateData(value: String) {
        _data.value = value
    }
}
```

**Using Dimension Resources:**
```xml
<!-- Use dimension resources instead of hardcoded dp values -->
android:padding="@dimen/margin_standard"
android:layout_marginTop="@dimen/margin_large"
```

Available dimensions: `margin_extra_extra_small` (4dp), `margin_extra_small` (8dp), `margin_small` (12dp), `margin_standard` (16dp), `margin_large` (24dp), `margin_extra_large` (32dp), `margin_extra_extra_large` (48dp)

## Product Flavors

The app supports 16 flavors with unique application IDs:
- `standard` - Default Smap flavor
- `fieldTaskMax` - Extended feature set
- `kontrolid`, `kontrolid_corporate` - Kontrolid variants
- Others: `plan`, `xxx1-4`, `meqa`, `pop`, `tdh`, `informEd`, `pangolin`, `stl2`, `ljstracker`, `bps`

Build commands use flavor name: `assembleFieldTaskMaxDebug`, `testKontrolidDebugUnitTest`, etc.

## Required Secrets

Create `secrets.gradle` in the root directory (see `secrets.gradle.example`):

```groovy
ext {
    MAPBOX_ACCESS_TOKEN = "your_token_here"
    MAPBOX_DOWNLOADS_TOKEN = "your_token_here"
    GOOGLE_MAPS_API_KEY = "your_key_here"
    // ... other secrets
}
```

Without these, map features and some tests will not work.

## Database and Storage

**Database:**
- Uses raw SQLite (no Room ORM)
- Multiple databases: forms, instances, entities
- Migrations in `collect_app/src/main/java/org/odk/collect/android/database/`

**Storage:**
- Uses Android scoped storage
- `StoragePathProvider` manages file locations
- Test data in `src/test/resources/` and `src/androidTest/assets/`

## Important Notes

**Widget System:**
- Form questions rendered using `QuestionWidget` framework
- Extend `QuestionWidget` for custom question types
- Smap custom widgets in `au.smap.fieldTask.widgets/`

**Map Engines:**
- Three implementations: Mapbox, osmdroid (OpenStreetMap), Google Maps
- User-selectable via preferences
- MapFragment abstraction provides shared interface

**Content Providers:**
- `FormsProvider` - External form access
- `InstanceProvider` - External instance access
- `TraceProvider` (Smap) - Custom trace data

**Visibility in Layouts:**
- Use `View.GONE` for hidden elements that shouldn't take space
- Use `View.INVISIBLE` only if space should be reserved

## Smap Functionality

### Overview

Smap (Survey Map) customizations extend ODK Collect with task management, location tracking, AWS integration, and organization-specific features. The smap codebase is located in `au/smap/fieldTask/` and integrates with the base ODK Collect architecture.

### Key Smap Components

**Main Activities:**
- `SplashScreenActivity` - Entry point, handles initial authentication check
- `SmapLoginActivity` - User authentication with token and password support
- `SmapMain` - Primary interface with 3 tabs: Tasks, Forms, Map
- `NFCActivity` - NFC tag scanning for task triggering
- `NotificationActivity` - FCM notification handling

**Task Management:**
- `DownloadTasksTask` - Downloads assigned tasks from server
- `InstanceSyncTask` - Auto-uploads completed instances
- `TaskEntry` - Task data model with geofencing and scheduling
- Task workflow: download → accept/reject → complete → auto-upload

**AWS Integration:**
- Uses AWS Cognito for identity management
- DynamoDB for device registration and FCM token storage
- `DeviceRegistrationService` - Registers device with AWS on startup
- `NotificationService` - Handles FCM push notifications from server

**Location Services:**
- `LocationService` - Foreground service for continuous tracking
- `TraceProvider` - Content provider for GPS trail data
- Flavor-specific `LocationRegister` classes control behavior
- Geofencing support for location-triggered tasks

**Custom Widgets:**
- `SmapFormWidget` - Launches sub-forms from within forms
- `NfcWidget` - NFC tag integration
- `SmapChartLineWidget`, `SmapChartHorizontalBarWidget` - Data visualization
- `GeoCompoundWidget` - Enhanced geography capture

**External Data Handlers:**
- `SmapRemoteDataHandlerLookup` - Remote data lookups
- `SmapRemoteDataHandlerSearch` - Server-side search
- `SmapRemoteDataHandlerGetMedia` - Media file retrieval
- `SmapRemoteDataHandlerLookupImagelabels` - Image label lookup

### Smap Database Extensions

**Forms Table (additional columns):**
- `PROJECT` - Project identifier
- `TASKS_ONLY` - Flag for task-only forms
- `READ_ONLY` - Read-only flag
- `SEARCH_LOCAL_DATA` - Local data search flag
- `SOURCE` - Data source identifier

**Instances Table (task columns):**
- `T_TITLE` - Task title
- `T_TASK_TYPE` - Task type
- `T_SCHED_START`, `T_SCHED_FINISH` - Scheduled times
- `T_ACT_START`, `T_ACT_FINISH` - Actual times
- `T_ADDRESS` - Task address
- `GEOMETRY`, `GEOMETRY_TYPE` - Spatial data
- `T_TASK_STATUS` - Task status (pending, accepted, completed, etc.)
- `T_ASS_ID` - Assignment ID
- `ACT_LON`, `ACT_LAT`, `SCHED_LON`, `SCHED_LAT` - Location coordinates

**Trace Database:**
- Separate database for GPS trail storage
- Tracks device movement for user location history
- Columns: _ID, SOURCE, LAT, LON, TIME

### Smap Settings Keys

Key preference keys in `GeneralSharedPreferencesSmap`:
- `KEY_SMAP_USE_TOKEN` - Enable token authentication
- `KEY_SMAP_AUTH_TOKEN` - Authentication token
- `KEY_SMAP_USER_LOCATION` - Enable location tracking
- `KEY_SMAP_REGISTRATION_ID` - FCM registration token
- `KEY_SMAP_ODK_STYLE_MENUS` - Use ODK-style menus
- Admin overrides for sync, location, delete, video, guidance

### Product Flavors

17 active flavors (portero deprecated):
- `standard` - Default smap flavor
- `fieldTaskMax` - Extended features
- `ljstracker` - Location tracking focused
- `bps` - Minimal location features
- Others: kontrolid, kontrolid_corporate, plan, xxx1-4, meqa, pop, tdh, informEd, pangolin, stl2

Each flavor can customize:
- `LocationRegister.java` - Location behavior
- Resources (icons, strings)
- AndroidManifest settings

### Testing Smap Features

**Unit Tests (23 files in `src/test/java/au/smap/`):**
- Authentication: SmapLoginTaskTest, SmapChangeOrganisationTaskTest
- Tasks: DownloadTasksTaskTest, InstanceSyncTaskTest
- AWS: DeviceRegistrationServiceTest, CognitoCredentialsProviderTest
- Widgets: SmapFormWidgetTest, NfcWidgetTest, Chart tests
- Location: LocationServiceTest, GeofenceEntryTest
- External data: Remote handler tests
- Database: FormsDAOTest, InstancesDAOTest, SmapTraceDatabaseHelperTest
- Notifications: NotificationServiceTest, SmapRegisterForMessagingTaskTest

**Instrumented Tests (4 files in `src/androidTest/java/au/smap/`):**
- SmapLoginActivityTest - Login UI flow
- SmapMainTest - Main activity and tabs
- SmapTaskListFragmentTest - Task list rendering
- NFCActivityTest - NFC functionality

**Run smap tests:**
```bash
./gradlew testStandardDebugUnitTest --tests "au.smap.fieldTask.*"
./gradlew connectedStandardDebugAndroidTest --tests "au.smap.*"
```

## Known TODOs and Future Work

The following TODOs are documented for future implementation. These are not blockers but represent opportunities for improvement:

### Data Integrity
**SmapMain.java:819** - Duplicate instance cleanup
- Issue: Multiple instances can point to the same file path, causing potential data corruption
- Impact: Data integrity risk in edge cases
- Priority: Medium
- Recommendation: Implement cleanup logic to detect and resolve duplicate instances, keeping the most recent or complete version

### Performance Optimization
**InstanceSyncTask.java:161-162** - Form definition caching
- Issue: Form definitions are looked up on every sync without caching
- Impact: Performance degradation on large datasets with many instances
- Priority: Medium
- Recommendation: Implement FormDefinitionCache with LRU eviction policy and invalidation on form updates

### User Experience
**DownloadTasksTask.java:876** - Download progress notification
- Issue: Progress notification code is commented out
- Impact: User experience during downloads
- Priority: Low
- Recommendation: Re-enable progress notification or remove commented code if no longer needed
