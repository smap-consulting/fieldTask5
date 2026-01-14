# FieldTask5

FieldTask5 is an Android data collection application for Monitoring & Evaluation and Case Management in field environments, including locations without data connectivity. It is part of the Smap framework for digitalization projects and is a modernized fork of ODK Collect with custom "smap" functionality.

**Key Features:**
- Offline data collection with automatic sync when connected
- Task management with geofencing and scheduling
- AWS Cognito integration for device management
- Push notifications via Firebase Cloud Messaging
- Custom form widgets for charts, NFC, and sub-forms
- Multi-organization support with 16 product flavors
- Location tracking with customizable behavior per flavor

**Built With:**
- Android SDK 21+ (Android 5.0 Lollipop to Android 15)
- Kotlin & Java
- Multi-module Gradle architecture (43 modules)
- Custom JavaRosa library (5.1.4-smap)

Further details on installing and testing FieldTask on an Android device can be found in the [Smap documentation](https://www.smap.com.au/docs/fieldTask.html). You will need to [set up an account](https://www.smap.com.au/docs/getting-started.html#create-an-account-on-the-hosted-server) on the free hosted server to test the app.

## Table of Contents
* [The Smap Ecosystem](#the-smap-ecosystem)
* [Setting Up Your Development Environment](#setting-up-your-development-environment)
* [Building the Application](#building-the-application)
* [Running Tests](#running-tests)
* [Product Flavors](#product-flavors)
* [Custom JavaRosa Dependency](#custom-javarosa-dependency)
* [Issues and Bug Reporting](#issues-and-bug-reporting)
* [Contributing](#contributing)
* [Related Repositories](#related-repositories)
* [Acknowledgements](#acknowledgements)

## The Smap Ecosystem

FieldTask5 is designed to work as part of the Smap ecosystem, which provides a complete solution for mobile data collection and case management. The ecosystem consists of multiple interconnected components:

### System Architecture

```
┌─────────────────┐
│   FieldTask5    │ ◄─── Mobile data collection (this repository)
│  (Android App)  │
└────────┬────────┘
         │ HTTPS/REST API
         ▼
┌─────────────────┐
│  Smap Server    │ ◄─── Backend server, API, data storage
│   (Java)        │
└────────┬────────┘
         │
    ┌────┴────┬──────────┬─────────────┐
    ▼         ▼          ▼             ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│WebForm │ │ Smap   │ │PostgreSQL│ │   AWS    │
│(Client)│ │ Client │ │ Database │ │ Services │
└────────┘ └────────┘ └──────────┘ └──────────┘
```

### Components

|Component     |Purpose                                                    |Technology      |
|------------- |---------------------------------------------------------- |----------------|
|**FieldTask5**    |Mobile data collection app (this repository)           |Android/Kotlin  |
|**JavaRosa**      |Form processing engine with smap extensions            |Java            |
|**Smap Server**   |Backend API, data storage, user management             |Java            |
|**WebForm**       |Browser-based form completion                          |JavaScript      |
|**Smap Client**   |Web-based administration and data analysis             |JavaScript      |
|**PostgreSQL**    |Relational database for data storage                   |SQL             |
|**AWS**           |Device registration, push notifications (optional)     |Cloud Services  |

### Data Flow

1. **Form Creation**: Forms are designed in Smap Client or uploaded as XLSForm
2. **Form Distribution**: Server pushes forms to registered FieldTask devices
3. **Data Collection**: Field workers complete forms offline on FieldTask
4. **Automatic Sync**: When connected, FieldTask auto-uploads completed surveys
5. **Data Processing**: Server validates, stores in PostgreSQL, triggers workflows
6. **Analysis**: Data accessible via Smap Client, APIs, or direct database queries

## Setting Up Your Development Environment

### Prerequisites

- **Java Development Kit (JDK) 17** or higher
- **Android Studio** (latest stable version recommended)
- **Git** for version control
- **A Google account** for Firebase and Maps API setup
- **A Mapbox account** for map functionality

### Step-by-Step Setup

#### 1. Clone the Repository

```bash
git clone https://github.com/smap-consulting/fieldTask5.git
cd fieldTask5
```

The `master` branch contains the latest stable code.

#### 2. Open in Android Studio

1. Launch Android Studio
2. Select "Open" and navigate to the cloned `fieldTask5` directory
3. Android Studio will automatically sync Gradle dependencies
4. Wait for indexing to complete

#### 3. Configure API Keys

Create a `secrets.gradle` file in the project root directory based on the example:

```bash
cp secrets.gradle.example secrets.gradle
```

Edit `secrets.gradle` and add your API keys:

```groovy
ext {
    MAPBOX_ACCESS_TOKEN = "pk.eyJ1..." // Your Mapbox access token
    MAPBOX_DOWNLOADS_TOKEN = "sk.ey..." // Your Mapbox downloads token
    GOOGLE_MAPS_API_KEY = "AIzaSy..." // Your Google Maps API key
}
```

**Getting API Keys:**

- **Mapbox**: [Create a free account](https://account.mapbox.com/auth/signup/) and get your tokens from the [account page](https://account.mapbox.com/)
- **Google Maps**: [Enable Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/start) and create an API key

#### 4. Configure Firebase (Google Services)

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use an existing one
3. Add an Android app with package name: `org.smap.smapTask.android`
4. Download `google-services.json`
5. Place it in `collect_app/google-services.json`

**Note:** The project includes a placeholder `google-services.json` that allows builds to succeed, but push notifications won't work without a valid configuration.

#### 5. Select Build Variant

In Android Studio:
1. Go to **Build > Select Build Variant**
2. For `collect_app`, select `standardDebug`

Note.  Custom code for many of the variants is not included in GitHub.  Hence you are probably best to stick to "standard" or create your own variant using one of the other included variants as the starting point.

#### 6. Build and Run

Click the green **Run** button or press `Shift+F10` to build and launch in an emulator or connected device.

## Building the Application

### Build Commands

```bash
# Build debug APK for standard flavor
./gradlew assembleStandardDebug

# Build release APK (requires signing configuration)
./gradlew assembleStandardRelease

# Build all variants
./gradlew build

# Clean build artifacts
./gradlew clean

# Install debug APK to connected device
./gradlew installStandardDebug
```

### Build Output

APK files are generated in:
```
collect_app/build/outputs/apk/{flavor}/{buildType}/
```

For example:
```
collect_app/build/outputs/apk/standard/debug/collect_app-standard-debug.apk
```

## Running Tests

FieldTask5 has comprehensive test coverage with 2000+ unit and instrumentation tests.

### Run All Unit Tests

```bash
./gradlew test
```

### Run Tests for Specific Flavor

```bash
./gradlew testStandardDebugUnitTest
```

### Run Instrumented Tests (requires device/emulator)

```bash
# Run all instrumented tests
./gradlew connectedStandardDebugAndroidTest

# Run specific test class
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.odk.collect.android.YourTest
```

### Code Quality Checks

```bash
# Run all quality checks (PMD, ktlint, Checkstyle, Lint)
./gradlew checkCode

# Auto-format Kotlin code
./gradlew ktlintFormat
```

### Test Reports

After running tests, view HTML reports at:
- Unit tests: `{module}/build/reports/tests/testDebugUnitTest/index.html`
- Instrumented tests: `{module}/build/reports/androidTests/connected/index.html`

## Flavor Customization

Each flavor can customize:
- **Icons and branding** (`src/{flavor}/res/`)
- **Location behavior** (`LocationRegister.java` in flavor directory)
- **AndroidManifest settings**
- **Strings and resources**

### Building Specific Flavors

```bash
# Build fieldTaskMax debug
./gradlew assembleFieldTaskMaxDebug

# Run tests for fieldTaskMax flavor
./gradlew testFieldTaskMaxDebugUnitTest
```

## Custom JavaRosa Dependency

FieldTask5 uses a **custom version of JavaRosa** (5.1.4-smap) with smap-specific enhancements. The built artifact is checked into version control in the `.local-m2/` directory, so **you don't need to build JavaRosa separately** for normal development.

### For Most Developers (No Action Needed)

The custom JavaRosa is already included in the repository:
```
.local-m2/org/getodk/javarosa/5.1.4-smap/
├── javarosa-5.1.4-smap.jar (681KB)
├── javarosa-5.1.4-smap.module
└── javarosa-5.1.4-smap.pom
```
Just clone and build - it works out of the box! 🎉

### For Developers Modifying JavaRosa

If you need to make changes to JavaRosa:

1. Clone the [smap JavaRosa repository](https://github.com/smap-consulting/javarosa)
2. Make your changes in the JavaRosa codebase
3. Build and publish to your local Maven repository:
   ```bash
   cd /path/to/javarosa
   ./gradlew publishToMavenLocal
   ```
4. Gradle automatically uses your `~/.m2/repository/` version (higher priority than `.local-m2/`)
5. When stable, copy the updated artifacts back:
   ```bash
   cd /path/to/fieldTask5
   cp ~/.m2/repository/org/getodk/javarosa/5.1.4-smap/* \
      .local-m2/org/getodk/javarosa/5.1.4-smap/
   git add .local-m2/
   git commit -m "Update custom javarosa"
   ```

## Issues and Bug Reporting

We use GitHub Issues to track bugs, feature requests, and development tasks.

### Reporting Issues

**For FieldTask5 mobile app issues:**
- Use the [FieldTask5 Issues page](https://github.com/smap-consulting/fieldTask5/issues)
- Examples: app crashes, UI bugs, form rendering issues, offline sync problems

**For server, web clients, or web forms:**
- Use the [Smap Server Issues page](https://github.com/smap-consulting/smapserver2/issues)
- Examples: server API issues, web form problems, data processing bugs

### Creating a Good Bug Report

When reporting a bug, please include:

1. **Clear title**: Brief description of the issue
2. **Environment**:
   - FieldTask version (from About screen)
   - Android version and device model
   - Server version (if relevant)
3. **Steps to reproduce**:
   ```
   1. Open the app
   2. Navigate to Forms > Get Blank Form
   3. Select form "Example Survey"
   4. App crashes
   ```
4. **Expected behavior**: What should happen
5. **Actual behavior**: What actually happens
6. **Screenshots/logs**: If applicable
7. **Form definition**: Attach XLSForm or XML if form-specific

### Feature Requests

For feature requests:
- Use the [FieldTask5 Issues page](https://github.com/smap-consulting/fieldTask5/issues)
- Use label: `enhancement`
- Describe the use case and expected behavior
- Explain why the feature would be valuable

### Issue Labels

Common labels used in this repository:
- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Documentation improvements
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention needed
- `priority: high` - Urgent issues
- `smap-specific` - Smap customizations (not in upstream ODK)

## Contributing

Contributions are welcome! Please follow these guidelines:

### Development Workflow

1. **Fork the repository** on GitHub
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Make your changes** following the code style guidelines
4. **Write tests** for new functionality
5. **Run quality checks**: `./gradlew checkCode`
6. **Commit with clear messages**: `git commit -m "Add feature: description"`
7. **Push to your fork**: `git push origin feature/your-feature-name`
8. **Open a Pull Request** with a clear description

### Code Style

- **Language**: Write all new code in Kotlin (existing Java is acceptable)
- **Formatting**: Run `./gradlew ktlintFormat` before committing
- **Architecture**: Follow MVVM pattern with ViewModels and LiveData
- **Testing**: Include unit tests for business logic, UI tests for user flows

### Commit Messages

Follow conventional commit format:
```
<type>: <description>

<optional body>

<optional footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Example:
```
feat: add geofencing support for task notifications

Implements geofencing to trigger push notifications when users
enter or exit task location boundaries.

Closes #123
```

## Related Repositories

The complete Smap ecosystem includes multiple repositories:

|Name          |GitHub Repository                             |Purpose                    |
|------------- |--------------------------------------------- |---------------------------|
|FieldTask5    |https://github.com/smap-consulting/fieldTask5 |Mobile data collection app |
|JavaRosa      |https://github.com/smap-consulting/javarosa   |Form processing engine     |
|SmapServer2   |https://github.com/smap-consulting/smapserver2|Backend server & API       |
|WebForm       |https://github.com/nap2000/webform            |Browser-based form client  |
|SmapClient    |https://github.com/nap2000/prop-smapserver    |Administrative web client  |
|Documentation |https://github.com/nap2000/docs               |User documentation         |

## Additional Resources

- **Documentation**: [https://www.smap.com.au/docs/](https://www.smap.com.au/docs/)
- **Server Downloads**: [https://www.smap.com.au/docs/server-admin-versions.html](https://www.smap.com.au/docs/server-admin-versions.html)
- **Getting Started Guide**: [https://www.smap.com.au/docs/getting-started.html](https://www.smap.com.au/docs/getting-started.html)
- **FieldTask Documentation**: [https://www.smap.com.au/docs/fieldTask.html](https://www.smap.com.au/docs/fieldTask.html)

## Acknowledgements

This project:
- Is a fork of [ODK Collect](https://github.com/getodk/collect) by Open Data Kit
- Includes the Android SDK from [Mapbox](https://www.mapbox.com/)
- Uses [Google Maps Platform](https://developers.google.com/maps) for mapping
- Integrates [Firebase](https://firebase.google.com/) for push notifications
- Built with [Gradle](https://gradle.org/) and [Android Jetpack](https://developer.android.com/jetpack)

**Special thanks** to the ODK community for creating and maintaining the excellent ODK Collect codebase that serves as the foundation for FieldTask5.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

**Questions?** Open an issue or contact the Smap team at [https://www.smap.com.au](https://www.smap.com.au)
