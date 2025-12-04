package com.ocnyang.viewmodelincompose.sharedviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.reflect.KClass

/**
 * Registers a [SharedScope] and monitors the route stack for automatic cleanup.
 *
 * Call this once in your NavHost to register a shared scope. When the route stack no longer
 * contains any routes from [SharedScope.includedRoutes], the scope will be automatically cleared.
 *
 * ## Usage
 *
 * ```kotlin
 * // 1. Define scope as an object
 * object OrderFlowScope : SharedScope(
 *     includedRoutes = setOf(Route.Home::class, Route.Order::class)
 * )
 *
 * object CheckoutScope : SharedScope(
 *     includedRoutes = setOf(Route.Cart::class, Route.Payment::class)
 * )
 *
 * // 2. Register in NavHost
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { entry ->
 *             entry.destination.route?.let { getRouteClass(it) }
 *         }.toSet()
 *     }
 *
 *     // Register scopes (only once at NavHost level)
 *     RegisterSharedScope(routesInStack, OrderFlowScope)
 *     RegisterSharedScope(routesInStack, CheckoutScope)
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> { HomeScreen() }
 *         composable<Route.Order> { OrderScreen() }
 *         composable<Route.Cart> { CartScreen() }
 *     }
 * }
 * ```
 *
 * ## How it works
 *
 * 1. **Registration**: First call registers the store in [SharedViewModelRegistry]
 * 2. **Monitoring**: Watches `routesInStack` for changes
 * 3. **Cleanup**: When none of the [SharedScope.includedRoutes] are in the stack,
 *    the store is cleared
 *
 * ## Configuration Changes
 *
 * The scope survives configuration changes (like screen rotation) because
 * [SharedViewModelRegistry] is a ViewModel that persists across recompositions.
 *
 * @param T The type of [SharedScope] to register
 * @param routesInStack The current routes in the navigation stack (as KClass set)
 * @param sharedScope The scope instance (should be an object)
 */
@Composable
inline fun <reified T : SharedScope> RegisterSharedScope(
    routesInStack: Set<KClass<*>>,
    sharedScope: T
) {
    val registry = LocalSharedViewModelRegistry.current
        ?: error("SharedViewModelRegistry not provided! Please wrap your root composable with ProvideSharedViewModelRegistry { ... }")

    // Get or create the store (only created once per scope)
    remember { registry.getOrPut(T::class, SharedViewModelStore()) }

    // Monitor route stack and clear when no included routes are present
    LaunchedEffect(routesInStack) {
        val shouldKeep = sharedScope.includedRoutes.any { it in routesInStack }

        if (!shouldKeep) {
            registry.clearScope(T::class)
        }
    }
}

/**
 * Registers a [SharedScope] with a custom cleanup condition.
 *
 * Use this when you need more control over when the scope is cleared,
 * or when working with non-standard navigation systems.
 *
 * Note: This overload does not use [SharedScope.includedRoutes] for cleanup logic.
 * The cleanup is controlled entirely by the [shouldKeep] function.
 *
 * ## Usage
 *
 * ```kotlin
 * // Define a scope (includedRoutes can be empty for custom cleanup)
 * object CustomScope : SharedScope(includedRoutes = emptySet())
 *
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val currentRoute = navController.currentBackStackEntryAsState().value
 *         ?.destination?.route
 *
 *     RegisterSharedScope<CustomScope>(
 *         shouldKeep = { currentRoute in listOf("home", "order") },
 *         key = currentRoute
 *     )
 *
 *     NavHost(navController, startDestination = "home") {
 *         composable("home") { HomeScreen() }
 *         composable("order") { OrderScreen() }
 *     }
 * }
 * ```
 *
 * @param T The type of [SharedScope] to register
 * @param shouldKeep Function that returns true when the scope should be kept (not cleared)
 * @param key Key that triggers re-evaluation when changed
 */
@Composable
inline fun <reified T : SharedScope> RegisterSharedScope(
    noinline shouldKeep: () -> Boolean,
    key: Any?
) {
    val registry = LocalSharedViewModelRegistry.current
        ?: error("SharedViewModelRegistry not provided! Please wrap your root composable with ProvideSharedViewModelRegistry { ... }")

    remember { registry.getOrPut(T::class, SharedViewModelStore()) }

    LaunchedEffect(key) {
        if (!shouldKeep()) {
            registry.clearScope(T::class)
        }
    }
}
