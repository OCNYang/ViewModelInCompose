package com.ocnyang.viewmodelincompose.sharedviewmodel

import kotlin.reflect.KClass

/**
 * Base class for defining a shared scope identifier.
 *
 * Subclasses should be defined as `object` to ensure a single instance per scope type.
 * The [includedRoutes] defines which navigation routes belong to this scope.
 * When all routes in [includedRoutes] are removed from the navigation stack,
 * the scope and its ViewModels will be automatically cleared.
 *
 * ## Usage
 *
 * ```kotlin
 * // Define a scope as an object
 * object OrderFlowScope : SharedScope(
 *     includedRoutes = setOf(Route.Home::class, Route.Order::class)
 * )
 *
 * // Register in NavHost
 * RegisterSharedScope(routesInStack, OrderFlowScope)
 *
 * // Use in screens
 * val store = getSharedViewModelStore<OrderFlowScope>()
 * val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = store)
 * ```
 *
 * @param includedRoutes Set of route KClasses that belong to this scope
 * @see RegisterSharedScope
 * @see getSharedViewModelStore
 */
abstract class SharedScope(val includedRoutes: Set<KClass<*>>)
