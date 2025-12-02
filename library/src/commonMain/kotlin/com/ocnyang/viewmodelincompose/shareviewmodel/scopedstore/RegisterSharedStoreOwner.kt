package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.reflect.KClass

/**
 * Registers a [SharedViewModelStoreOwner] and monitors the route stack for automatic cleanup.
 *
 * Call this once in your NavHost to register a shared scope. When the route stack no longer
 * contains any routes from [SharedViewModelStoreOwner.includedRoutes], the scope will be
 * automatically cleared.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     // Collect the current route stack
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { entry ->
 *             entry.destination.route?.let { getRouteClass(it) }
 *         }.toSet()
 *     }
 *
 *     // Register scopes (only once at NavHost level)
 *     RegisterSharedStoreOwner(routesInStack) { OrderFlowStoreOwner() }
 *     RegisterSharedStoreOwner(routesInStack) { CheckoutStoreOwner() }
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
 * 1. **Registration**: First call registers the StoreOwner in [ScopedViewModelStoreOwner]
 * 2. **Monitoring**: Watches `routesInStack` for changes
 * 3. **Cleanup**: When none of the [SharedViewModelStoreOwner.includedRoutes] are in the stack,
 *    the StoreOwner is cleared
 *
 * ## Configuration Changes
 *
 * The scope survives configuration changes (like screen rotation) because
 * [ScopedViewModelStoreOwner] is a ViewModel that persists across recompositions.
 *
 * @param T The type of [SharedViewModelStoreOwner] to register
 * @param routesInStack The current routes in the navigation stack (as KClass set)
 * @param factory Factory function to create the StoreOwner (only called once per registration)
 */
@Composable
inline fun <reified T : SharedViewModelStoreOwner> RegisterSharedStoreOwner(
    routesInStack: Set<KClass<*>>,
    noinline factory: () -> T
) {
    val registry = LocalScopedViewModelStoreOwner.current
        ?: error("ScopedViewModelStoreOwner not provided! Please wrap your root composable with ProvideScopedViewModelStoreOwner { ... }")

    // Get or create the StoreOwner (factory only called once)
    val owner = remember { registry.getOrPut(T::class, factory) }

    // Monitor route stack and clear when no included routes are present
    LaunchedEffect(routesInStack) {
        if (!owner.hasRouteInStack(routesInStack)) {
            registry.clearScope(T::class)
        }
    }
}

/**
 * Registers a [SharedViewModelStoreOwner] with a custom cleanup condition.
 *
 * Use this when you need more control over when the scope is cleared,
 * or when working with non-standard navigation systems.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val currentRoute = navController.currentBackStackEntryAsState().value
 *         ?.destination?.route
 *
 *     RegisterSharedStoreOwner(
 *         shouldKeep = { currentRoute in listOf("home", "order") },
 *         key = currentRoute,
 *         factory = { OrderFlowStoreOwner() }
 *     )
 *
 *     NavHost(navController, startDestination = "home") {
 *         composable("home") { HomeScreen() }
 *         composable("order") { OrderScreen() }
 *     }
 * }
 * ```
 *
 * @param T The type of [SharedViewModelStoreOwner] to register
 * @param shouldKeep Function that returns true when the scope should be kept (not cleared)
 * @param key Key that triggers re-evaluation when changed
 * @param factory Factory function to create the StoreOwner
 */
@Composable
inline fun <reified T : SharedViewModelStoreOwner> RegisterSharedStoreOwner(
    noinline shouldKeep: () -> Boolean,
    key: Any?,
    noinline factory: () -> T
) {
    val registry = LocalScopedViewModelStoreOwner.current
        ?: error("ScopedViewModelStoreOwner not provided! Please wrap your root composable with ProvideScopedViewModelStoreOwner { ... }")

    val owner = remember { registry.getOrPut(T::class, factory) }

    LaunchedEffect(key) {
        if (!shouldKeep()) {
            registry.clearScope(T::class)
        }
    }
}
