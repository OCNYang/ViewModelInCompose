# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Kotlin Multiplatform (KMP) library providing ViewModel utilities for Compose Multiplatform. Targets Android, iOS, Desktop (JVM), and Web (JS/Wasm).

## Build Commands

```bash
# Run demo app
./gradlew :composeApp:run                          # Desktop
./gradlew :composeApp:assembleDebug                # Android
./gradlew :composeApp:wasmJsBrowserDevelopmentRun  # Web (Wasm)
./gradlew :composeApp:jsBrowserDevelopmentRun      # Web (JS)

# Run tests
./gradlew :library:check      # Library tests
./gradlew :composeApp:check   # Demo app tests

# Run single test class (example)
./gradlew :library:jvmTest --tests "com.ocnyang.viewmodelincompose.ExampleUnitTest"
```

iOS: Open `iosApp` directory in Xcode.

## Modules

- **library**: Core KMP library with ViewModel utilities
- **composeApp**: Demo application showcasing the library

## Library Architecture

The library (`library/src/commonMain/kotlin/com/ocnyang/viewmodelincompose/`) provides:

### EventEffect (`eventeffect/`)
Lifecycle-aware one-time event handling for Compose:
- `EventChannel<T>`: Channel-based event emitter for ViewModels. Events are buffered and guaranteed delivery.
- `EventEffect()`: Composable that collects events from a Flow, respecting lifecycle state (default: STARTED).

Usage pattern:
```kotlin
// ViewModel
private val _events = EventChannel<UiEvent>()
val events: Flow<UiEvent> = _events.flow

// Composable
EventEffect(viewModel.events) { event -> /* handle */ }
```

### StateBus (`statebus/`)
Cross-screen state sharing with automatic cleanup:
- Two-tier ViewModel architecture (Activity-level StateBus + per-screen listener ViewModels)
- Automatic listener tracking and resource cleanup
- SavedStateHandle integration for process death recovery
- Thread-safe with ConcurrentHashMap + AtomicInteger

Note: StateBus currently uses Android-specific APIs (`android.util.Log`, `SavedStateHandle`) and is not fully multiplatform yet.

## Key Dependencies

- Compose Multiplatform 1.9.1
- Kotlin 2.2.20
- JetBrains Lifecycle ViewModel Compose (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`)
- Compose Hot Reload plugin for development
