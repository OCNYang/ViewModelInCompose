package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass

/**
 * Base class for shared ViewModel StoreOwner.
 *
 * Users extend this class to define a scope shared by a group of pages.
 * When the navigation stack no longer contains any routes defined in [includedRoutes],
 * all ViewModels in this scope will be automatically cleared.
 *
 * ## Defining a Scope
 *
 * ```kotlin
 * // Define a scope shared by Home and Order pages
 * class OrderFlowStoreOwner : SharedViewModelStoreOwner(
 *     includedRoutes = setOf(Route.Home::class, Route.Order::class)
 * )
 *
 * // Define a scope for the Checkout flow
 * class CheckoutStoreOwner : SharedViewModelStoreOwner(
 *     includedRoutes = setOf(Route.Cart::class, Route.Payment::class, Route.Confirmation::class)
 * )
 * ```
 *
 * ## Usage Flow
 *
 * 1. Provide [ScopedViewModelStoreOwner] at the root
 * 2. Register using [RegisterSharedStoreOwner] at NavHost level (only once)
 * 3. Use [getSharedStoreOwner] in screens to get the StoreOwner
 *
 * ```kotlin
 * // Register at NavHost level
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
 *     }
 *
 *     // Register scope (only once)
 *     RegisterSharedStoreOwner(routesInStack) { OrderFlowStoreOwner() }
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> { HomeScreen() }
 *         composable<Route.Order> { OrderScreen() }
 *     }
 * }
 *
 * // Use in screens
 * @Composable
 * fun HomeScreen() {
 *     val storeOwner = getSharedStoreOwner<OrderFlowStoreOwner>()
 *     val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = storeOwner)
 * }
 * ```
 *
 * @param includedRoutes Set of route KClasses included in this scope
 * @see ScopedViewModelStoreOwner
 * @see RegisterSharedStoreOwner
 * @see getSharedStoreOwner
 */
abstract class SharedViewModelStoreOwner(
    val includedRoutes: Set<KClass<*>>
) : ViewModelStoreOwner {

    private val _viewModelStore by lazy { ViewModelStore() }

    override val viewModelStore: ViewModelStore get() = _viewModelStore

    /**
     * Checks if any route from this scope is in the navigation stack.
     *
     * @param routesInStack Current routes in the navigation stack as KClass set
     * @return true if the stack contains any route from [includedRoutes]
     */
    fun hasRouteInStack(routesInStack: Set<KClass<*>>): Boolean {
        return includedRoutes.any { it in routesInStack }
    }

    /**
     * Clears all ViewModels in this StoreOwner.
     *
     * After calling this, `onCleared()` will be called on all ViewModels stored in this StoreOwner.
     */
    fun clear() {
        _viewModelStore.clear()
    }
}