package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass

/**
 * 共享 ViewModel 的 StoreOwner 基类
 *
 * 用户通过继承此类来定义一组页面共享的 Scope。
 * 当导航栈中不再包含任何 [includedRoutes] 中定义的页面时，该 Scope 内的所有 ViewModel 会被自动清理。
 *
 * ## 定义 Scope
 *
 * ```kotlin
 * // 定义 Home 和 Order 页面共享的 Scope
 * class OrderFlowStoreOwner : SharedViewModelStoreOwner(
 *     includedRoutes = setOf(Route.Home::class, Route.Order::class)
 * )
 *
 * // 定义 Checkout 流程的 Scope
 * class CheckoutStoreOwner : SharedViewModelStoreOwner(
 *     includedRoutes = setOf(Route.Cart::class, Route.Payment::class, Route.Confirmation::class)
 * )
 * ```
 *
 * ## 使用流程
 *
 * 1. 在根路由提供 [ScopedViewModelStoreOwner]
 * 2. 在 NavHost 层级使用 [RegisterSharedStoreOwner] 注册（只需注册一次）
 * 3. 在页面中通过 [getSharedStoreOwner] 获取并使用
 *
 * ```kotlin
 * // NavHost 层级注册
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val backStack by navController.currentBackStack.collectAsState()
 *     val routesInStack = remember(backStack) {
 *         backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
 *     }
 *
 *     // 注册 Scope（只需一次）
 *     RegisterSharedStoreOwner(routesInStack) { OrderFlowStoreOwner() }
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> { HomeScreen() }
 *         composable<Route.Order> { OrderScreen() }
 *     }
 * }
 *
 * // 页面中使用
 * @Composable
 * fun HomeScreen() {
 *     val storeOwner = getSharedStoreOwner<OrderFlowStoreOwner>()
 *     val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = storeOwner)
 * }
 * ```
 *
 * @param includedRoutes 包含在此 Scope 内的路由 KClass 集合
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
     * 检查路由栈中是否包含此 Scope 的任一路由
     *
     * @param routesInStack 当前路由栈中的路由 KClass 集合
     * @return true 如果路由栈中包含 [includedRoutes] 中的任一路由
     */
    fun hasRouteInStack(routesInStack: Set<KClass<*>>): Boolean {
        return includedRoutes.any { it in routesInStack }
    }

    /**
     * 检查当前路由是否在 Scope 内
     *
     * @param currentRoute 当前路由的 KClass
     * @return true 如果当前路由在 [includedRoutes] 中
     */
    fun isInScope(currentRoute: KClass<*>?): Boolean {
        return currentRoute != null && currentRoute in includedRoutes
    }

    /**
     * 清理所有 ViewModel
     *
     * 调用后，所有存储在此 StoreOwner 中的 ViewModel 的 `onCleared()` 会被调用
     */
    fun clear() {
        _viewModelStore.clear()
    }
}