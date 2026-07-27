# FocusGuard

A powerful Android application for maintaining focus and managing distractions.

## Project Structure

```
focusguard/
├── app/                          # Main Android app module
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/          # Kotlin source files
│   │   │   ├── res/             # Resources (layouts, strings, colors, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # Unit tests
│   ├── build.gradle.kts         # App module build configuration
│   └── proguard-rules.pro       # ProGuard rules for release builds
├── build.gradle.kts             # Root build configuration
├── settings.gradle.kts          # Project settings
├── gradle.properties            # Gradle properties
├── build.sh                     # Alternative build script
└── README.md                    # This file
```

## Requirements

- Android SDK 24 or higher
- Kotlin 1.9.10
- Gradle 8.1.0
- Java 11

## Build Instructions

### Using Gradle (Recommended)

1. Clone the repository:
```bash
git clone https://github.com/Echonode-dev/focusguard.git
cd focusguard
```

2. Build the app:
```bash
./gradlew build
```

3. Install debug APK:
```bash
./gradlew installDebug
```

4. Build release APK:
```bash
./gradlew assembleRelease
```

### Using build.sh Script

```bash
chmod +x build.sh
./build.sh
```

## Dependencies

- androidx.core:core-ktx
- androidx.appcompat:appcompat
- androidx.activity:activity-ktx
- androidx.fragment:fragment-ktx
- com.google.android.material:material
- androidx.constraintlayout:constraintlayout
- org.jetbrains.kotlin:kotlin-stdlib
- org.jetbrains.kotlinx:kotlinx-coroutines-android
- com.squareup.okhttp3:okhttp
- com.squareup.retrofit2:retrofit
- com.google.code.gson:gson

## Development

### Project Configuration

All build configuration is centralized in:
- `gradle.properties` - Version and SDK information
- `app/build.gradle.kts` - App-specific dependencies and build types
- `build.gradle.kts` - Root-level plugins and repositories

### Running Tests

```bash
./gradlew test
```

### Running Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

## Troubleshooting

### Build Errors

1. **Gradle sync issues**: Run `./gradlew clean build`
2. **SDK not found**: Update `local.properties` with correct SDK path
3. **Kotlin compilation errors**: Ensure JDK 11+ is installed

### Common Issues

- **"AAPT not found"**: Install Android build tools
- **"Kotlin not found"**: Check Kotlin plugin in build.gradle.kts
- **APK signing issues**: Verify keystore in `~/.android/debug.keystore`

## License

This project is licensed under the MIT License.

## Contributors

- Echonode-dev

## Support

For issues and feature requests, please visit: https://github.com/Echonode-dev/focusguard/issues
