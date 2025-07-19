# Gemini Project Configuration: rd-watch

This document provides Gemini with essential context about the `rd-watch` project, ensuring its assistance aligns with the established architecture, conventions, and workflows.

## 1. Project Overview

- **Core Functionality:** An Android TV application built with Kotlin. It serves as an open-source, modern alternative to media aggregation and streaming applications like "Stremio" and "Syncler".
- **User Profile:** The primary developer is experienced in Python and TypeScript and is relatively new to Kotlin. Assistance should be clear, idiomatic, and follow modern Kotlin/Jetpack Compose best practices.

## 2. Tech Stack & Architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose for UI, supplemented by Android TV-specific libraries (Leanback).
- **Architecture:** Follows modern Android app architecture principles.
  - **Dependency Injection:** Hilt is used extensively for managing dependencies.
  - **Asynchronous Operations:** Kotlin Coroutines are the standard for async tasks.
  - **Networking:** Retrofit, OkHttp, and Moshi for API communication.
  - **Database:** Room for local persistence.
  - **Data Storage:** DataStore for simple key-value storage.
  - **Background Jobs:** WorkManager for deferrable background tasks.
  - **Image Loading:** Coil for loading images in Compose.

## 3. Coding Style & Conventions

- **Idiomatic Kotlin:** Code should adhere to the official Kotlin style guides and modern Android development practices.
- **Jetpack Compose:** UI should be built following Compose best practices.
- **Immutability:** Prefer immutable data structures (`val`, `List`) over mutable ones where possible.
- **Clarity for Newcomer:** Given the developer's background, provide clear explanations for Kotlin-specific idioms or complex architectural patterns when making changes.

## 4. Testing

The project has a well-defined testing structure located in `app/src/test/` and `app/src/androidTest/`.

- **Test Runner:** `HiltTestRunner` is the custom test instrumentation runner.
- **Frameworks:** JUnit, Mockk, and `kotlinx-coroutines-test`.
- **Dependency Injection in Tests:** Hilt is used to manage dependencies in all tests.
  - **Base Classes:** Tests should inherit from `HiltTestBase` (for unit tests) or `HiltInstrumentedTestBase` (for instrumented tests).
  - **Test Modules:** Hilt's `@TestInstallIn` is used to replace production modules with test doubles (e.g., `TestNetworkModule`, `TestDatabaseModule`).
  - **Fakes:** The project uses "Fake" implementations (e.g., `FakeMovieRepository`) for testing higher-level components against predictable data without mocking every layer. New tests for similar components should adopt this pattern.
- **Coroutine Testing:** The `MainDispatcherRule` is a `TestWatcher` used to manage `Dispatchers.Main` in tests. It should be included in any test class that involves coroutines.

## 5. Important Commands

- **Build:** To compile the application and assemble a debug build.
  ```bash
  ./gradlew assembleDebug
  ```
- **Lint:** To run a detailed static analysis and check for issues.
  ```bash
  ./ktlint-summary.sh
  ```
- **Run Unit Tests:** To execute all unit tests on the JVM.
  ```bash
  ./gradlew test
  ```
- **Run Instrumented Tests:** To execute tests on a connected Android device or emulator. (Note: An `adb` tunnel is set up for the development environment).
  ```bash
  ./gradlew connectedAndroidTest
  ```
