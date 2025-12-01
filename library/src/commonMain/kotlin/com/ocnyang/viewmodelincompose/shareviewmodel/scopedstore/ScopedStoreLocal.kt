package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.reflect.KClass

/**
 * CompositionLocal for providing a [ScopedViewModelStoreOwner] to the composition tree.
 *
 * This allows child composables to access a shared [ScopedViewModelStoreOwner] without
 * explicitly passing it through function parameters.
 *
 * ## Usage
 *
 * ```kotlin
 * // 1. Provide at a higher level (e.g., NavHost)
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "order_scope")
 *
 *     ProvideScopedViewModelStoreOwner(scopedStoreOwner) {
 *         NavHost(navController, startDestination = Route.Home) {
 *             composable<Route.Home> { HomeScreen() }
 *             composable<Route.Order> { OrderScreen() }
 *         }
 *     }
 * }
 *
 * // 2. Use in child composables
 * @Composable
 * fun HomeScreen() {
 *     val scopedStoreOwner = LocalScopedViewModelStoreOwner.current
 *         ?: error("ScopedViewModelStoreOwner not provided")
 *
 *     val sharedViewModel: SharedViewModel = viewModel(
 *         viewModelStoreOwner = scopedStoreOwner
 *     )
 * }
 * ```
 *
 * @see ProvideScopedViewModelStoreOwner
 * @see ScopedViewModelStoreOwner
 */
val LocalScopedViewModelStoreOwner = staticCompositionLocalOf<ScopedViewModelStoreOwner?> {
    null
}

/**
 * Provides a [ScopedViewModelStoreOwner] to the composition tree.
 *
 * All child composables can access the provided owner via [LocalScopedViewModelStoreOwner].
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "my_scope")
 *
 *     ProvideScopedViewModelStoreOwner(scopedStoreOwner) {
 *         // Child composables can now access scopedStoreOwner
 *         // via LocalScopedViewModelStoreOwner.current
 *         MyNavHost()
 *     }
 * }
 * ```
 *
 * @param owner The [ScopedViewModelStoreOwner] to provide
 * @param content The child composables
 */
@Composable
fun ProvideScopedViewModelStoreOwner(
    owner: ScopedViewModelStoreOwner,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalScopedViewModelStoreOwner provides owner) {
        content()
    }
}

/**
 * Provides a [ScopedViewModelStoreOwner] with automatic cleanup when leaving scope.
 *
 * This is a convenience function that combines [ProvideScopedViewModelStoreOwner] with
 * automatic scope management based on navigation state.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val currentDestination = navController.currentBackStackEntryAsState().value?.destination
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "order_scope")
 *
 *     ProvideScopedViewModelStoreOwner(
 *         owner = scopedStoreOwner,
 *         currentRouteClass = currentDestination?.route?.let { getRouteClass(it) },
 *         includedRoutes = listOf(Route.Home::class, Route.Order::class)
 *     ) {
 *         NavHost(navController, startDestination = Route.Home) {
 *             composable<Route.Home> { HomeScreen() }
 *             composable<Route.Order> { OrderScreen() }
 *         }
 *     }
 * }
 * ```
 *
 * @param owner The [ScopedViewModelStoreOwner] to provide
 * @param currentRouteClass The current route class (from navigation state)
 * @param includedRoutes The list of route classes that share this scope
 * @param content The child composables
 */
@Composable
fun ProvideScopedViewModelStoreOwner(
    owner: ScopedViewModelStoreOwner,
    currentRouteClass: KClass<*>?,
    includedRoutes: List<KClass<*>>,
    content: @Composable () -> Unit
) {
    val currentIncludedRoutes by rememberUpdatedState(includedRoutes)

    // Monitor navigation and clear when leaving scope
    DisposableEffect(currentRouteClass) {
        onDispose {
            val isInScope = currentRouteClass != null && currentIncludedRoutes.any { routeClass ->
                routeClass == currentRouteClass
            }
            // Clear when navigating away from all included routes
            if (!isInScope && currentRouteClass != null) {
                owner.clear()
            }
        }
    }

    CompositionLocalProvider(LocalScopedViewModelStoreOwner provides owner) {
        content()
    }
}

/**
 * Provides a [ScopedViewModelStoreOwner] with automatic cleanup using a custom scope check.
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
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "order_scope")
 *
 *     ProvideScopedViewModelStoreOwner(
 *         owner = scopedStoreOwner,
 *         isInScope = {
 *             currentDestination?.hasRoute<Route.Home>() == true ||
 *             currentDestination?.hasRoute<Route.Order>() == true
 *         },
 *         keys = arrayOf(currentDestination)
 *     ) {
 *         NavHost(navController, startDestination = Route.Home) {
 *             composable<Route.Home> { HomeScreen() }
 *             composable<Route.Order> { OrderScreen() }
 *         }
 *     }
 * }
 * ```
 *
 * @param owner The [ScopedViewModelStoreOwner] to provide
 * @param isInScope A function that returns true if the current destination is within scope
 * @param keys Keys that trigger re-evaluation of [isInScope] when changed
 * @param content The child composables
 */
@Composable
fun ProvideScopedViewModelStoreOwner(
    owner: ScopedViewModelStoreOwner,
    isInScope: () -> Boolean,
    vararg keys: Any?,
    content: @Composable () -> Unit
) {
    val currentIsInScope by rememberUpdatedState(isInScope)

    DisposableEffect(*keys) {
        onDispose {
            // Clear when not in scope
            if (!currentIsInScope()) {
                owner.clear()
            }
        }
    }

    CompositionLocalProvider(LocalScopedViewModelStoreOwner provides owner) {
        content()
    }
}
