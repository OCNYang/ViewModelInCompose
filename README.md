# ViewModelInCompose

A Kotlin Multiplatform library providing enhanced ViewModel utilities for Compose Multiplatform.

## Platforms

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## Features

- **LaunchedEffectOnce**: Execute side effects only once per page lifecycle
- **EventEffect**: Lifecycle-aware one-time event handling from ViewModel to UI
- **SharedViewModel**: Share ViewModels across multiple screens with automatic lifecycle management

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ocnyang:viewmodelincompose:<version>")
}
```

---

## LaunchedEffectOnce

A `LaunchedEffect` that executes only once per page lifecycle, surviving recomposition and configuration changes.

### When to Use

- One-time initialization (data loading, analytics tracking)
- Side effects that should not repeat on recomposition or screen rotation

### Comparison with Standard LaunchedEffect

| Behavior | LaunchedEffect | LaunchedEffectOnce |
|----------|---------------|-------------------|
| Recomposition | May re-execute | Does not re-execute |
| Configuration change | Re-executes | Does not re-execute |
| Page recreation | Re-executes | Re-executes |

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

### Multiple Independent Effects

```kotlin
@Composable
fun MyScreen() {
    // Use viewModelKey to distinguish different effects
    LaunchedEffectOnce(viewModelKey = "loadData") {
        viewModel.loadData()
    }

    LaunchedEffectOnce(viewModelKey = "analytics") {
        analytics.trackPageView()
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
            // Do some work...
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

    // UI content...
}
```

### API Reference

| API | Description |
|-----|-------------|
| `EventChannel<T>` | Channel-based event emitter for ViewModel |
| `EventChannel.send()` | Suspending function to emit an event |
| `EventChannel.trySend()` | Non-suspending event emission (may drop if buffer full) |
| `EventEffect()` | Composable to collect and handle events |

---

## SharedViewModel

Share ViewModels across multiple screens with automatic cleanup when all sharing screens are removed from the navigation stack.

### When to Use

- Multiple screens need to share the same ViewModel instance
- Data should persist while navigating between related screens
- ViewModel should be cleared when leaving the entire flow

### Comparison with Route-Scoped ViewModel

| Feature | SharedViewModel | Route-Scoped ViewModel |
|---------|-----------------|----------------------|
| Scope Definition | Custom scope with `SharedScope` | Tied to navigation route lifecycle |
| Cleanup Timing | When all `includedRoutes` leave the stack | When the route is popped |
| Access Method | `getSharedViewModelStore<Scope>()` anywhere | Must pass from route entry point |
| Nested Navigation | Supports sharing across nested nav graphs | Limited to single nav graph |

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
    // Use orderVm...
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
// Cart -> Checkout -> Payment (linear flow)
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

When the user leaves all three screens, the `OrderViewModel` is automatically cleared.

#### Scenario 2: Parent Route with Nested Navigation

```kotlin
// IMPORTANT: Use PARENT route, not nested routes
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // Parent route only!
)
```

In nested navigation, the parent route stays in the back stack while nested content changes. Using the parent route keeps the scope active throughout the entire section.

### API Reference

| API | Description |
|-----|-------------|
| `SharedScope` | Base class for defining a shared scope |
| `ProvideSharedViewModelRegistry` | Provides the registry to the composition tree |
| `RegisterSharedScope` | Registers a scope and monitors route stack for cleanup |
| `getSharedViewModelStore<T>()` | Gets the ViewModelStoreOwner for a scope |
| `sharedViewModel<S, T>()` | Convenience function to get a shared ViewModel |

---

## License

```
Apache License 2.0
```
