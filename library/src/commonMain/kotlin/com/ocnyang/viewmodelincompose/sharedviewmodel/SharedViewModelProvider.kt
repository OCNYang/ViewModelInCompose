package com.ocnyang.viewmodelincompose.sharedviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

/**
 * CompositionLocal for providing a [SharedViewModelRegistry] to the composition tree.
 *
 * This allows child composables to access a shared [SharedViewModelRegistry] without
 * explicitly passing it through function parameters.
 *
 * ## Usage
 *
 * ```kotlin
 * // 1. Define scope as an object
 * object OrderFlowScope : SharedScope(
 *     includedRoutes = setOf(Route.Home::class, Route.Order::class)
 * )
 *
 * // 2. Provide at root level
 * @Composable
 * fun App() {
 *     ProvideSharedViewModelRegistry {
 *         MyNavHost()
 *     }
 * }
 *
 * // 3. Register in NavHost
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
 *     }
 *
 *     RegisterSharedScope(routesInStack, OrderFlowScope)
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> { HomeScreen() }
 *         composable<Route.Order> { OrderScreen() }
 *     }
 * }
 *
 * // 4. Use in screens
 * @Composable
 * fun HomeScreen() {
 *     val store = getSharedViewModelStore<OrderFlowScope>()
 *     val sharedVm: SharedViewModel = viewModel(viewModelStoreOwner = store)
 * }
 * ```
 *
 * @see ProvideSharedViewModelRegistry
 * @see RegisterSharedScope
 * @see getSharedViewModelStore
 */
val LocalSharedViewModelRegistry = staticCompositionLocalOf<SharedViewModelRegistry?> {
    null
}

/**
 * Provides a [SharedViewModelRegistry] to the composition tree.
 *
 * Call this at the root of your navigation graph to enable shared ViewModels.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideSharedViewModelRegistry {
 *         val navController = rememberNavController()
 *         MyNavHost(navController)
 *     }
 * }
 * ```
 *
 * @param content The child composables
 */
@Composable
fun ProvideSharedViewModelRegistry(
    content: @Composable () -> Unit
) {
    val registry = rememberSharedViewModelRegistry()

    CompositionLocalProvider(LocalSharedViewModelRegistry provides registry) {
        content()
    }
}

/**
 * Gets the [SharedViewModelStore] for the specified scope.
 *
 * The scope must have been registered via [RegisterSharedScope] before calling this.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val store = getSharedViewModelStore<OrderFlowScope>()
 *
 *     val sharedVm: SharedOrderViewModel = viewModel(
 *         viewModelStoreOwner = store
 *     ) { SharedOrderViewModel() }
 * }
 * ```
 *
 * @param T The type of [SharedScope] to get the store for
 * @return The registered store instance
 * @throws IllegalStateException if [LocalSharedViewModelRegistry] is not provided
 * @throws IllegalStateException if the specified scope is not registered
 */
@Composable
inline fun <reified T : SharedScope> getSharedViewModelStore(): ViewModelStoreOwner {
    val registry = LocalSharedViewModelRegistry.current
        ?: error("SharedViewModelRegistry not provided! Please wrap your root composable with ProvideSharedViewModelRegistry { ... }")

    return registry.get(T::class)
}

/**
 * Gets the [SharedViewModelStore] for the specified scope, or null if not registered.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val store = getSharedViewModelStoreOrNull<OrderFlowScope>()
 *
 *     if (store != null) {
 *         val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = store)
 *         // Use shared data
 *     } else {
 *         // Fallback behavior
 *     }
 * }
 * ```
 *
 * @param T The type of [SharedScope] to get the store for
 * @return The registered store instance, or null
 */
@Composable
inline fun <reified T : SharedScope> getSharedViewModelStoreOrNull(): ViewModelStoreOwner? {
    val registry = LocalSharedViewModelRegistry.current ?: return null
    return registry.getOrNull(T::class)
}
