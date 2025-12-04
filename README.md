# ViewModelInCompose

A Kotlin Multiplatform library for sharing ViewModels across multiple screens in Compose Multiplatform.

## Platforms

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## SharedViewModel

SharedViewModel allows multiple screens to share the same ViewModel instance, with automatic cleanup when all sharing screens are removed from the navigation stack.

### Features

- **Automatic Lifecycle Management**: The shared ViewModel is automatically cleared when all screens that use it are removed from the navigation stack
- **No Manual Passing Required**: Access the shared ViewModel from any `@Composable` function without passing it through parameters
- **Type-Safe Scope Definition**: Use `object` to define scopes with compile-time type safety
- **Configuration Change Survival**: Shared ViewModels survive configuration changes (like screen rotation)

### Comparison with Route-Scoped ViewModel

| Feature | SharedViewModel | Route-Scoped ViewModel |
|---------|-----------------|----------------------|
| **Scope Definition** | Custom scope with `SharedScope` | Tied to navigation route lifecycle |
| **Cleanup Timing** | When all `includedRoutes` leave the stack | When the route is popped |
| **Access Method** | `getSharedViewModelStore<Scope>()` anywhere | Must pass from route entry point |
| **Use Case** | Share data across multiple related screens | Single screen or parent-child hierarchy |
| **Nested Navigation** | Supports sharing across nested nav graphs | Limited to single nav graph |

### Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ocnyang:viewmodelincompose:<version>")
}
```

### Quick Start

#### 1. Define a Scope

```kotlin
// Define a scope as an object
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### 2. Provide Registry at Root

```kotlin
@Composable
fun App() {
    ProvideSharedViewModelRegistry {
        val navController = rememberNavController()
        MyNavHost(navController)
    }
}
```

#### 3. Register Scope in NavHost

```kotlin
@Composable
fun MyNavHost(navController: NavHostController) {
    // Collect current route stack
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { entry ->
            entry.destination.route?.let { getRouteClass(it) }
        }.toSet()
    }

    // Register scope
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
    // Option 1: Get store and use viewModel
    val store = getSharedViewModelStore<OrderFlowScope>()
    val orderVm: OrderViewModel = viewModel(viewModelStoreOwner = store) {
        OrderViewModel()
    }

    // Option 2: Use convenience function
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

When multiple screens at the same navigation level need to share data:

```kotlin
// Cart -> Checkout -> Payment (linear flow)
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

When the user completes payment and navigates away from all three screens, the `OrderViewModel` is automatically cleared.

#### Scenario 2: Parent Route with Nested Navigation

When a parent route contains nested navigation and needs to share ViewModel with child screens:

```kotlin
// Main route structure:
// - Route.Home
// - Route.Dashboard (parent with nested nav)
//   - Route.Dashboard.Overview (nested)
//   - Route.Dashboard.Analytics (nested)
//   - Route.Dashboard.Settings (nested)

// IMPORTANT: includedRoutes should contain the PARENT route, not the nested routes
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // Parent route only!
)
```

**Why use parent route instead of nested routes?**

In nested navigation, when you navigate between nested destinations:
- The parent route (`Route.Dashboard`) stays in the back stack
- Only the nested content changes

If you included nested routes like `Route.Dashboard.Overview::class`, the scope would be cleared when switching between nested destinations because those specific routes leave and re-enter the stack.

By using the parent route, the scope remains active as long as the user is anywhere within the Dashboard section.

```kotlin
@Composable
fun DashboardNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
    }

    // Register with parent route monitoring
    RegisterSharedScope(routesInStack, DashboardScope)

    NavHost(navController, startDestination = "overview") {
        composable("overview") {
            // Access shared ViewModel
            val dashboardVm = sharedViewModel<DashboardScope, DashboardViewModel> {
                DashboardViewModel()
            }
            OverviewScreen(dashboardVm)
        }
        composable("analytics") {
            val dashboardVm = sharedViewModel<DashboardScope, DashboardViewModel> {
                DashboardViewModel()
            }
            AnalyticsScreen(dashboardVm)
        }
    }
}
```

### API Reference

| API | Description |
|-----|-------------|
| `SharedScope` | Base class for defining a shared scope |
| `ProvideSharedViewModelRegistry` | Provides the registry to the composition tree |
| `RegisterSharedScope` | Registers a scope and monitors route stack for cleanup |
| `getSharedViewModelStore<T>()` | Gets the ViewModelStoreOwner for a scope |
| `sharedViewModel<S, T>()` | Convenience function to get a shared ViewModel |

### Architecture

```
Activity/Fragment ViewModelStore
  └─ SharedViewModelRegistry (ViewModel, survives config changes)
      └─ Map<KClass<SharedScope>, SharedViewModelStore>
          ├─ OrderFlowScope::class → SharedViewModelStore
          │   └─ ViewModelStore
          │       └─ OrderViewModel
          ├─ DashboardScope::class → SharedViewModelStore
          │   └─ ViewModelStore
          │       └─ DashboardViewModel
          └─ ...
```

## License

```
Apache License 2.0
```
