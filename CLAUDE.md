# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project using Compose Multiplatform, targeting Android, iOS, Desktop (JVM), and Web (JS/Wasm). The project appears to be a library/demo for ViewModel usage in Compose Multiplatform.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug

# Desktop (JVM)
./gradlew :composeApp:run

# Web (Wasm - modern browsers)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS - legacy browsers)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run tests
./gradlew :viewmodelincompose:check
./gradlew :composeApp:check
```

iOS builds require Xcode - open the `iosApp` directory in Xcode.

## Architecture

### Modules

- **composeApp**: Demo application showcasing the library across all platforms
- **viewmodelincompose**: The core library module (KMP library)

### Source Set Structure

Both modules follow standard KMP source set conventions:
- `commonMain`: Shared code for all platforms
- `androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`: Platform-specific implementations
- `commonTest`: Shared tests
- `androidDeviceTest`: Android instrumentation tests

### Key Dependencies

- Compose Multiplatform 1.9.1
- Kotlin 2.2.20
- AndroidX Lifecycle ViewModel Compose (multiplatform version via JetBrains)
- Compose Hot Reload plugin enabled for development
