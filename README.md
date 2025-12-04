# ViewModelInCompose

[![](https://jitpack.io/v/OCNYang/ViewModelInCompose.svg)](https://jitpack.io/#OCNYang/ViewModelInCompose)

[中文文档](README_ZH.md)

A Kotlin Multiplatform library providing enhanced ViewModel utilities for Compose Multiplatform.

## Platforms

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## Features

- **LaunchedEffectOnce**: Execute side effects only once per page lifecycle
- **EventEffect**: Lifecycle-aware one-time event handling from ViewModel to UI
- **StateBus**: Cross-screen state sharing with automatic lifecycle management
- **SharedViewModel**: Share ViewModels across multiple screens with automatic cleanup

## Installation

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ...
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.OCNYang:ViewModelInCompose:<version>")
}
```

> Replace `<version>` with the latest version shown in the badge above, or check [releases](https://github.com/OCNYang/ViewModelInCompose/releases).

---

## LaunchedEffectOnce

A `LaunchedEffect` that executes only once per page lifecycle, surviving recomposition and configuration changes.When jumping to the next page and returning, it will not be re-executed. It will only be executed when reopening after exiting the current page.

### When to Use

- One-time initialization (data loading, analytics tracking)
- Side effects that should not repeat on recomposition or screen rotation

### Comparison with Standard LaunchedEffect

| Behavior | LaunchedEffect | LaunchedEffectOnce |
|----------|----------------|-------------------|
| Recomposition | May re-execute | Does not re-execute |
| When returning from the next page | Re-execute     | Does not re-execute |
| Configuration change | Re-executes    | Does not re-execute |
| Page recreation | Re-executes    | Re-executes |

### Usage

```kotlin
@Composable
fun MyScreen() {
    // Executes only once per page lifecycle
    LaunchedEffectOnce {
        viewModel.loadData()
        analytics.trackPageView()
    }
}
```

### With Keys

```kotlin
@Composable
fun UserScreen(userId: String) {
    // Re-executes only when userId changes
    LaunchedEffectOnce(userId) {
        viewModel.loadUser(userId)
    }
}
```

---

## EventEffect

Lifecycle-aware event collection for handling one-time UI events from ViewModel.

### When to Use

- Navigation events
- Showing toasts or snackbars
- One-time UI actions triggered by ViewModel

### Features

- Lifecycle-aware: only collects events when UI is visible
- Events are guaranteed to be delivered (won't be lost during config changes)
- Each event is consumed exactly once

### Usage

#### 1. Define Events in ViewModel

```kotlin
class MyViewModel : ViewModel() {
    private val _events = EventChannel<UiEvent>()
    val events: Flow<UiEvent> = _events.flow

    fun onSubmit() {
        viewModelScope.launch {
            _events.send(UiEvent.ShowToast("Success!"))
            _events.send(UiEvent.NavigateBack)
        }
    }
}

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data object NavigateBack : UiEvent
}
```

#### 2. Collect Events in Composable

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    EventEffect(viewModel.events) { event ->
        when (event) {
            is UiEvent.ShowToast -> showToast(event.message)
            is UiEvent.NavigateBack -> navigator.navigateBack()
        }
    }
}
```

---

## StateBus

Cross-screen state sharing solution with automatic lifecycle management.

### When to Use

- Pass data between screens (e.g., selection result from screen B back to screen A)
- Share temporary state across multiple screens
- Need automatic cleanup when no screens are observing

### Features

- **Automatic Listener Tracking**: Tracks observer count automatically
- **Automatic Cleanup**: State is cleared when no observers remain
- **Thread-Safe**: Uses synchronized locks and atomic counters
- **Configuration Change Survival**: State persists across screen rotation
- **Process Death Recovery**: Android platform supports state restoration via SavedStateHandle
- **Kotlin Multiplatform**: Works on all supported platforms

### Quick Start

#### 1. Provide StateBus at Root

```kotlin
@Composable
fun App() {
    ProvideStateBus {
        MyNavHost()
    }
}
```

#### 2. Set State (Sender Screen)

```kotlin
@Composable
fun ScreenB() {
    val stateBus = LocalStateBus.current

    Button(onClick = {
        stateBus.setState<Person>(selectedPerson)
        navigator.navigateBack()
    }) {
        Text("Confirm Selection")
    }
}
```

#### 3. Observe State (Receiver Screen)

```kotlin
@Composable
fun ScreenA() {
    val stateBus = LocalStateBus.current
    val person = stateBus.observeState<Person?>()

    Text("Selected: ${person?.name ?: "None"}")
}
```

### Using Custom Keys

When using generic types that may conflict (e.g., `Result<String>` vs `Result<Int>`), specify a custom key:

```kotlin
// Use explicit keys to avoid type erasure conflicts
val userResult = stateBus.observeState<Result<User>?>(stateKey = "userResult")
val orderResult = stateBus.observeState<Result<Order>?>(stateKey = "orderResult")
```

### API Reference

| API | Description |
|-----|-------------|
| `ProvideStateBus` | Provides StateBus to the composition tree |
| `LocalStateBus.current` | Gets the current StateBus instance |
| `observeState<T>()` | Observes state with automatic listener tracking |
| `setState<T>()` | Sets state value |
| `removeState<T>()` | Manually removes state |
| `hasState()` | Checks if state exists |

### Architecture

```
Activity/Fragment ViewModelStore
  └─ StateBus (ViewModel)
      └─ stateDataMap (thread-safe state cache)
      └─ persistence (platform-specific, SavedStateHandle on Android)

NavBackStackEntry (per screen)
  └─ StateBusListenerViewModel
      └─ Registers/unregisters listener on lifecycle
```

### Platform Notes

| Platform | Process Death Recovery |
|----------|----------------------|
| Android | ✅ Supported (SavedStateHandle) |
| iOS | ❌ Not supported |
| Desktop | ❌ Not supported |
| Web | ❌ Not supported |

---

## SharedViewModel

Share ViewModels across multiple screens with automatic cleanup when all sharing screens are removed from the navigation stack.

### When to Use

- Multiple screens need to share the same ViewModel instance
- Data should persist while navigating between related screens
- ViewModel should be cleared when leaving the entire flow

### Comparison with StateBus

| Feature | SharedViewModel | StateBus |
|---------|-----------------|----------|
| **Purpose** | Share ViewModel instances | Share state values |
| **Scope** | Defined by `SharedScope` | Global within StateBus |
| **Cleanup** | When all `includedRoutes` leave stack | When no observers remain |
| **Use Case** | Complex business logic sharing | Simple data passing |

### Quick Start

#### 1. Define a Scope

```kotlin
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### 2. Provide Registry at Root

```kotlin
@Composable
fun App() {
    ProvideSharedViewModelRegistry {
        MyNavHost()
    }
}
```

#### 3. Register Scope in NavHost

```kotlin
@Composable
fun MyNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { entry ->
            entry.destination.route?.let { getRouteClass(it) }
        }.toSet()
    }

    RegisterSharedScope(routesInStack, OrderFlowScope)

    NavHost(navController, startDestination = Route.Cart) {
        composable<Route.Cart> { CartScreen() }
        composable<Route.Checkout> { CheckoutScreen() }
        composable<Route.Payment> { PaymentScreen() }
    }
}
```

#### 4. Use in Screens

```kotlin
@Composable
fun CartScreen() {
    val orderVm = sharedViewModel<OrderFlowScope, OrderViewModel> {
        OrderViewModel()
    }
}

@Composable
fun CheckoutScreen() {
    // Same ViewModel instance as CartScreen
    val orderVm = sharedViewModel<OrderFlowScope, OrderViewModel> {
        OrderViewModel()
    }
}
```

### Usage Scenarios

#### Scenario 1: Sibling Screens Sharing ViewModel

```kotlin
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### Scenario 2: Parent Route with Nested Navigation

```kotlin
// IMPORTANT: Use PARENT route, not nested routes
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // Parent route only!
)
```

---

## License

```
Apache License 2.0
```
