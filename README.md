# Zazeks: Rock–Paper–Scissors with Gesture Recognition

This repository contains the in-progress migration of the Zazeks gesture-driven Rock–Paper–Scissors game from a Python/FastAPI backend to a Spring Boot backend paired with the existing Android client. The goal is to provide the same real-time gameplay, matchmaking, and player management features that the FastAPI stack exposed while simplifying local development.

---

## Project layout

| Path | Description |
|------|-------------|
| `java-backend/` | Spring Boot 3 service that currently backs the game APIs and multiplayer WebSocket. |
| `android/` | Android client written in Java with HTTP/WebSocket integrations and configurable backend endpoints. |
| `backend/` | Legacy FastAPI service kept only as a reference for feature parity. |
| `docs/` | High-level architecture notes, testing strategy, and demo scripts. |
| `model/`, `defay_1x9/`, `frontend/` | Assets from the original prototype (not yet integrated with the Java backend). |

---

## Current status and remaining work

### Spring Boot backend

**Implemented**
- JWT-based authentication with registration/login endpoints.
- Player profile, avatar, and leaderboard operations backed by an in-memory data store.
- REST CRUD for single-player game history plus duplicate-submission protection.
- WebSocket matchmaking (`/ws/multiplayer`) with battle lifecycle management and result persistence.
- Administrative actions for moderating users and game records.

**To be reimplemented before feature parity**
- Replace the in-memory database with the PostgreSQL schema that existed in the FastAPI service.
- Persist avatars and other binary assets outside of process memory.
- Restore ML-powered inference for `/model/detect` (the current implementation returns a deterministic hash-based stub).
- Reintroduce analytics/log streaming and structured audit trails from the Python stack.

### Android client

**Implemented**
- Connects to configurable HTTP/WebSocket endpoints for authentication, play, and leaderboards.
- Streams camera frames to `/model/detect` and renders bounding boxes/gesture results when provided.
- Includes debug and release signing configuration templates.

**To be reimplemented**
- Switch the HTTP bridge to the restored Java inference service or enable on-device TensorFlow Lite once the backend endpoint is updated.
- Re-enable offline caching and background sync that depended on the FastAPI endpoints.
- Update UI copy/screenshots after parity testing with the new backend.

Legacy FastAPI code remains available for comparison during the migration but should not be deployed for new development.

---

## Backend API surface (Spring Boot)

All endpoints expect JSON unless stated otherwise. Authenticated routes require an `Authorization: Bearer <token>` header obtained from `POST /auth/login`.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/auth/register` | Create a player with username, password, optional Base64 avatar. |
| `POST` | `/auth/login` | Obtain a JWT for subsequent requests. |
| `GET` | `/users/leaderboard/offline` | Offline leaderboard sorted by total wins. |
| `GET` | `/users/leaderboard/online` | Online leaderboard sorted by multiplayer wins. |
| `GET` | `/users/{userId}` | Fetch the authenticated player’s profile and statistics. |
| `GET` | `/users/{userId}/avatar` | Retrieve the player avatar as a Base64 string. |
| `PUT` | `/users/{userId}` | Update username and/or avatar. |
| `POST` | `/games` | Save a single-player match result. |
| `GET` | `/games/{gameId}` | Retrieve a specific single-player match. |
| `GET` | `/games/user/{userId}` | List matches for the authenticated player. |
| `PUT` | `/games/add-win/{userId}` | Increment the win counter (utility action used by the client). |
| `POST` | `/multiplayer/result` | Persist a multiplayer match result. |
| `POST` | `/model/detect` | Upload a JPEG frame for gesture detection (stubbed until ML integration returns). |
| `DELETE` | `/admin/users/{userId}` | Remove a non-admin user. |
| `POST` | `/admin/admins` | Grant admin role to a user. |
| `DELETE` | `/admin/admins/{userId}` | Revoke admin role. |
| `DELETE` | `/admin/games/{gameId}` | Delete any recorded game. |
| `DELETE` | `/admin/users/{userId}/photo` | Remove avatar media. |
| `PUT` | `/admin/users/{userId}/username` | Force-update a username. |
| `WS` | `/ws/multiplayer` | Matchmaking and game orchestration for multiplayer battles. |

Swagger/OpenAPI generation has not been wired up yet; use the integration tests in `java-backend/src/test/java` as executable documentation.

---

## Running the backend

```
cd java-backend
# First-time setup: ensure the Gradle wrapper JAR is downloaded.
./gradlew wrapper
# Start the Spring Boot service (equivalent to the legacy FastAPI app).
./gradlew bootRun
```

The service listens on <http://localhost:8080>. Configuration such as JWT lifetimes reads from environment variables via `com.zazeks.config.Settings`.

During development you can reset the in-memory store between manual tests by restarting the process.

---

## Android client quick start

```
cd android
# Assemble a debug build
./gradlew assembleDebug
# Run JVM unit tests for the Android module
./gradlew testDebugUnitTest
```

Update `src/main/assets/config/backend.json` to point the app to your backend instance (see below). Install the resulting APK with `adb install -r build/outputs/apk/debug/android-debug.apk` or by using the VS Code/Android Studio device manager.

The client currently expects the `/model/detect` endpoint to be reachable over HTTP. If you test against the Java backend before the ML rewrite lands, the placeholder inference will respond with pseudo-random gestures derived from the uploaded bytes.

---

## VS Code workflow

1. **Recommended extensions**
   - *Extension Pack for Java* (includes Language Support for Java, Debugger for Java, and the Gradle Tasks explorer).
   - *Android Extension Pack* or at least *Android Emulator* + *ADB Interface* so VS Code can surface devices and logcat.
   - *Kotlin* support is optional but useful for reading Gradle Kotlin DSL files.

2. **Integrated terminal commands**
   - Backend: `cd java-backend && ./gradlew bootRun` (VS Code exposes this as the `run` task in the Gradle explorer, so you can trigger it with `./gradlew run` from the command palette if you prefer the alias).
   - Backend tests: `cd java-backend && ./gradlew test`.
   - Android build: `cd android && ./gradlew assembleDebug`.

   You can add these commands to `.vscode/tasks.json` to launch them with <kbd>Ctrl</kbd>/<kbd>Cmd</kbd>+<kbd>Shift</kbd>+<kbd>B</kbd>. When defining tasks, set the `cwd` to either `java-backend` or `android` to avoid path issues.

3. **Debugging & devices**
   - Use the Java extension’s *Run and Debug* view to attach to the Spring Boot app. Configure a `Java: Launch` profile pointing at `com.zazeks.app.Application`—VS Code will reuse the Gradle build and allow breakpoints.
   - Install the *Android* extension’s ADB integration to list physical devices or emulators in the *Devices* panel. Ensure an emulator is running (`Android SDK` tools) or plug in a device with USB debugging enabled.
   - Create an `android` launch configuration with `"request": "launch"` and `"appSrcRoot": "${workspaceFolder}/android"` so you can trigger `assembleDebug` + deploy directly from VS Code.
   - For emulator debugging, start an Android Virtual Device (`avdmanager` or Android Studio), then use the *Run on Android* command palette entry supplied by the extension pack.

---

## End-to-end testing from VS Code

The goal is to exercise the full stack—backend REST/WebSocket plus the Android client—in one workspace.

1. **Backend verification**
   - `cd java-backend && ./gradlew test` runs the JUnit integration suite (`src/test/java/com/zazeks/web/*IntegrationTest.java`), covering REST endpoints, JWT flows, and WebSocket matchmaking. Failures usually highlight parity gaps with the legacy FastAPI behaviour.

2. **Android unit tests**
   - `cd android && ./gradlew testDebugUnitTest` executes JVM tests for view models, repositories, and HTTP bridges without needing an emulator.

3. **Android UI tests**
   - `cd android && ./gradlew connectedDebugAndroidTest` requires an emulator or device. Launch the emulator via the VS Code device panel, then trigger the task from the Gradle explorer or a custom task entry.

4. **Smoke-testing the full flow**
   - Start the backend (`./gradlew bootRun`).
   - Deploy the debug APK to an emulator using `adb install` or VS Code’s *Run on Android* command.
   - From the emulator, register a user, play a single-player round, then open matchmaking so the WebSocket traffic is exercised. Watch backend logs in the terminal to confirm game persistence.
   - For inference checks, upload static images via the in-app gallery until the real model is restored.

Because the backend stores everything in memory, restarting `bootRun` gives you a clean slate for repeated test loops.

---

## Contributing

1. Fork and clone the repository.
2. Create a feature branch and ensure both backend (`./gradlew test`) and Android (`./gradlew testDebugUnitTest`) checks pass before opening a PR.
3. Update this README or the Android documentation when you add or change API endpoints so the migration guide stays current.

