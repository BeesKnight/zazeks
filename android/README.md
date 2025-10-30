# Zazeks Android app

This guide walks through preparing, configuring, and testing the Android client while the backend migration to Spring Boot is underway.

## Prerequisites

- Android Studio Iguana (or newer) **or** the Android command-line tools with Gradle 8.5+
- Android SDK 34 with platform tools and at least one emulator image (Android 7.0+ supported)
- Java 17 (use the JDK bundled with Android Studio or Temurin 17)

## Project structure

```
android/
├── build.gradle          # Module-level Gradle file
├── signing/              # Debug/release signing configuration (template provided)
├── src/main/assets/      # Backend configuration and ML assets packaged into the APK
└── src/main/java/...     # Application sources
```

`src/main/assets` ships configuration files such as `config/backend.json`, demo models, and camera presets. They are included in every build variant.

## APK signing

1. Copy `signing/signing.properties.example` to `signing/signing.properties`.
2. Generate a keystore if you do not already have one:
   ```bash
   keytool -genkeypair -v \
     -storetype PKCS12 \
     -keystore signing/my-release-key.jks \
     -alias my-key-alias \
     -keyalg RSA -keysize 2048 -validity 3650
   ```
3. Update `signing/signing.properties` with the keystore path and passwords:
   ```
   storeFile=signing/my-release-key.jks
   storePassword=<keystore password>
   keyAlias=<key alias>
   keyPassword=<key password>
   ```

When the file exists, Gradle signs both debug and release builds automatically.

## Backend configuration

The app reads connection details from `src/main/assets/config/backend.json`. Point it at the Spring Boot service (default port 8080):

```json
{
  "backendBaseUrl": "http://10.0.2.2:8080",
  "detectionEndpoint": "/model/detect",
  "timeoutSeconds": 15
}
```

Use `10.0.2.2` when running on an emulator; physical devices should reference your workstation IP. The detection endpoint stays the same while the Java backend regains ML support. If you experiment with on-device inference, replace `backendBaseUrl` with `"device"` and update the client code to bypass HTTP calls.

## Building from the command line

```bash
cd android
./gradlew assembleDebug   # Produces app-debug.apk
./gradlew assembleRelease # Produces app-release.apk
```

> If you are setting up the project on a new machine, run `./gradlew wrapper` once to download the Gradle wrapper JAR.

Outputs appear under `android/build/outputs/apk/<buildType>/`.

## Installing a build

1. Enable Developer Mode and USB debugging on your device (or boot an emulator).
2. Verify that the device is visible: `adb devices`.
3. Install the APK:
   ```bash
   adb install -r build/outputs/apk/debug/android-debug.apk
   ```
4. Launch the **Zazeks** app.

## Packaging checks

- Android Studio: *Build* → *Analyze APK* to inspect embedded assets (`assets/config/backend.json`, ML labels, camera presets).
- Command line: `./gradlew verifyReleaseResources` fails the build if required resources are missing.

## Demo flow

1. Start the Spring Boot backend (`cd ../java-backend && ./gradlew bootRun`).
2. Launch the app and register a new player.
3. Play a single-player round to exercise the `/games` endpoint.
4. Join matchmaking to verify WebSocket connectivity and `/multiplayer/result`.
5. Observe backend logs for saved games; restart the backend for a clean slate.

When the ML pipeline is reattached, the `/model/detect` endpoint will return real gestures; until then it provides a deterministic placeholder response.

## VS Code tips

See the root `README.md` for VS Code automation, including tasks for `./gradlew assembleDebug` and device debugging.

