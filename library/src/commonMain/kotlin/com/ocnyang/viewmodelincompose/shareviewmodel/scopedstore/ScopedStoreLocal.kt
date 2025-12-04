package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing a [ScopedViewModelStoreOwner] to the composition tree.
 *
 * This allows child composables to access a shared [ScopedViewModelStoreOwner] without
 * explicitly passing it through function parameters.
 *
 * ## Usage
 *
 * ```kotlin
 * // 1. Provide at root level
 * @Composable
 * fun App() {
 *     ProvideScopedViewModelStoreOwner {
 *         MyNavHost()
 *     }
 * }
 *
 * // 2. Register in NavHost
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
 *     }
 *
 *     RegisterSharedStoreOwner(routesInStack) { OrderFlowStoreOwner() }
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> { HomeScreen() }
 *         composable<Route.Order> { OrderScreen() }
 *     }
 * }
 *
 * // 3. Use in screens
 * @Composable
 * fun HomeScreen() {
 *     val storeOwner = getSharedStoreOwner<OrderFlowStoreOwner>()
 *     val sharedVm: SharedViewModel = viewModel(viewModelStoreOwner = storeOwner)
 * }
 * ```
 *
 * @see ProvideScopedViewModelStoreOwner
 * @see RegisterSharedStoreOwner
 * @see getSharedStoreOwner
 */
val LocalScopedViewModelStoreOwner = staticCompositionLocalOf<ScopedViewModelStoreOwner?> {
    null
}

/**
 * Provides a [ScopedViewModelStoreOwner] to the composition tree.
 *
 * Call this at the root of your navigation graph to enable shared ViewModels.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideScopedViewModelStoreOwner {
 *         val navController = rememberNavController()
 *         MyNavHost(navController)
 *     }
 * }
 * ```
 *
 * @param content The child composables
 */
@Composable
fun ProvideScopedViewModelStoreOwner(
    content: @Composable () -> Unit
) {
    val owner = rememberScopedViewModelStoreOwner()

    CompositionLocalProvider(LocalScopedViewModelStoreOwner provides owner) {
        content()
    }
}

/**
 * Gets the [SharedViewModelStoreOwner] of the specified type.
 *
 * The StoreOwner must have been registered via [RegisterSharedStoreOwner] before calling this.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val storeOwner = getSharedStoreOwner<OrderFlowStoreOwner>()
 *
 *     val sharedVm: SharedOrderViewModel = viewModel(
 *         viewModelStoreOwner = storeOwner
 *     ) { SharedOrderViewModel() }
 * }
 * ```
 *
 * @param T The type of [SharedViewModelStoreOwner] to get
 * @return The registered StoreOwner instance
 * @throws IllegalStateException if [LocalScopedViewModelStoreOwner] is not provided
 * @throws IllegalStateException if the specified type is not registered
 */
@Composable
inline fun <reified T : SharedViewModelStoreOwner> getSharedStoreOwner(): T {
    val registry = LocalScopedViewModelStoreOwner.current
        ?: error("ScopedViewModelStoreOwner not provided! Please wrap your root composable with ProvideScopedViewModelStoreOwner { ... }")

    return registry.get(T::class)
}

/**
 * Gets the [SharedViewModelStoreOwner] of the specified type, or null if not registered.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val storeOwner = getSharedStoreOwnerOrNull<OrderFlowStoreOwner>()
 *
 *     if (storeOwner != null) {
 *         val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = storeOwner)
 *         // Use shared data
 *     } else {
 *         // Fallback behavior
 *     }
 * }
 * ```
 *
 * @param T The type of [SharedViewModelStoreOwner] to get
 * @return The registered StoreOwner instance, or null
 */
@Composable
inline fun <reified T : SharedViewModelStoreOwner> getSharedStoreOwnerOrNull(): T? {
    val registry = LocalScopedViewModelStoreOwner.current ?: return null
    return registry.getOrNull(T::class)
}
