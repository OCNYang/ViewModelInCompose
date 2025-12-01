package com.ocnyang.viewmodelincompose.shareviewmodel.sharedscope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

/**
 * Creates and remembers a [SharedScope] that can be used to share ViewModels between specific screens.
 *
 * The SharedScope survives configuration changes and automatically clears its ViewModels when
 * navigating away from all included routes.
 *
 * ## Key Features
 * - ✅ Survives configuration changes (screen rotation)
 * - ✅ Automatic cleanup when leaving scope
 * - ✅ Thread-safe ViewModel access
 * - ✅ Reference counting prevents premature cleanup
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     // Create a shared scope for Home and Order screens
 *     val currentDestination = navController.currentBackStackEntryAsState().value?.destination
 *
 *     val sharedScope = rememberSharedScope(
 *         key = "order_scope",
 *         currentRouteClass = currentDestination?.route?.let { getRouteClass(it) },
 *         includedRoutes = listOf(Route.Home::class, Route.Order::class)
 *     )
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> {
 *             HomeScreen(
 *                 sharedViewModel = sharedScope.getViewModel { SharedOrderViewModel() }
 *             )
 *         }
 *         composable<Route.Order> {
 *             OrderScreen(
 *                 sharedViewModel = sharedScope.getViewModel { SharedOrderViewModel() }
 *             )
 *         }
 *         composable<Route.Product> {
 *             // Product screen doesn't have access to SharedOrderViewModel
 *             // When navigating here, the SharedOrderViewModel will be cleared
 *             ProductScreen()
 *         }
 *     }
 * }
 * ```
 *
 * @param key A unique key to identify this scope. Different keys create different scopes.
 * @param currentRouteClass The current route class (from navigation state). When this changes to a
 *                          route not in [includedRoutes], the scope will be cleared.
 * @param includedRoutes The list of route classes that share this scope
 * @return A [SharedScope] that can be used to get shared ViewModels
 */
@Composable
fun rememberSharedScope(
    key: String,
    currentRouteClass: KClass<*>?,
    includedRoutes: List<KClass<*>>
): SharedScope {
    // Use ViewModel to survive configuration changes
    val holder = viewModel(key = "SharedScope_$key") { SharedScopeHolder() }
    val scope = holder.scope

    val currentIncludedRoutes by rememberUpdatedState(includedRoutes)

    // Track reference and check if in scope
    DisposableEffect(currentRouteClass) {
        scope.addReference()

        onDispose {
            val newCount = scope.removeReference()

            // Check if current route is within scope
            val isInScope = currentRouteClass != null && currentIncludedRoutes.any { routeClass ->
                routeClass == currentRouteClass
            }

            // Clear the scope when navigating away from all included routes
            // and there are no more references
            if (!isInScope && currentRouteClass != null && newCount <= 0) {
                scope.clear()
            }
        }
    }

    return scope
}

/**
 * Creates and remembers a [SharedScope] with a custom scope check function.
 *
 * This is the **recommended** approach as it provides the most flexibility
 * for determining whether the current destination is within scope.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val currentDestination = navController.currentBackStackEntryAsState().value?.destination
 *
 *     val sharedScope = rememberSharedScope(
 *         key = "order_scope",
 *         isInScope = {
 *             currentDestination?.hasRoute<Route.Home>() == true ||
 *             currentDestination?.hasRoute<Route.Order>() == true
 *         },
 *         keys = arrayOf(currentDestination)
 *     )
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> {
 *             HomeScreen(
 *                 sharedViewModel = sharedScope.getViewModel { SharedViewModel() }
 *             )
 *         }
 *         composable<Route.Order> {
 *             OrderScreen(
 *                 sharedViewModel = sharedScope.getViewModel { SharedViewModel() }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param key A unique key to identify this scope. Different keys create different scopes.
 * @param isInScope A function that returns true if the current destination is within the shared scope
 * @param keys Keys that trigger re-evaluation of [isInScope] when changed
 * @return A [SharedScope] that can be used to get shared ViewModels
 */
@Composable
fun rememberSharedScope(
    key: String,
    isInScope: () -> Boolean,
    vararg keys: Any?
): SharedScope {
    // Use ViewModel to survive configuration changes
    val holder = viewModel(key = "SharedScope_$key") { SharedScopeHolder() }
    val scope = holder.scope

    val currentIsInScope by rememberUpdatedState(isInScope)

    DisposableEffect(*keys) {
        scope.addReference()

        onDispose {
            val newCount = scope.removeReference()

            // Clear the scope when not in scope and no more references
            if (!currentIsInScope() && newCount <= 0) {
                scope.clear()
            }
        }
    }

    return scope
}

/**
 * Creates and remembers a simple [SharedScope] without automatic cleanup.
 *
 * Use this when you want full manual control over when the scope is cleared.
 * The scope still survives configuration changes.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val sharedScope = rememberSharedScope(key = "my_scope")
 *
 *     // Manual cleanup when needed
 *     DisposableEffect(Unit) {
 *         onDispose {
 *             sharedScope.clear()
 *         }
 *     }
 *
 *     val viewModel = sharedScope.getViewModel { MyViewModel() }
 * }
 * ```
 *
 * @param key A unique key to identify this scope. Different keys create different scopes.
 * @return A [SharedScope] that can be used to get shared ViewModels
 */
@Composable
fun rememberSharedScope(key: String): SharedScope {
    val holder = viewModel(key = "SharedScope_$key") { SharedScopeHolder() }
    return holder.scope
}

/**
 * Creates and remembers a [SharedScope] with automatic cleanup when the composable is disposed.
 *
 * This is useful for temporary scopes that should be cleared when leaving a screen.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun CheckoutFlow() {
 *     val sharedScope = rememberSharedScopeWithAutoCleanup(key = "checkout")
 *
 *     // ViewModels will be cleared when this composable is disposed
 *     NavHost(...) {
 *         composable("cart") { CartScreen(sharedScope) }
 *         composable("payment") { PaymentScreen(sharedScope) }
 *         composable("confirmation") { ConfirmationScreen(sharedScope) }
 *     }
 * }
 * ```
 *
 * @param key A unique key to identify this scope
 * @return A [SharedScope] that will be cleared when the composable is disposed
 */
@Composable
fun rememberSharedScopeWithAutoCleanup(key: String): SharedScope {
    val holder = viewModel(key = "SharedScope_$key") { SharedScopeHolder() }
    val scope = holder.scope

    DisposableEffect(Unit) {
        scope.addReference()

        onDispose {
            val newCount = scope.removeReference()
            if (newCount <= 0) {
                scope.clear()
            }
        }
    }

    return scope
}
