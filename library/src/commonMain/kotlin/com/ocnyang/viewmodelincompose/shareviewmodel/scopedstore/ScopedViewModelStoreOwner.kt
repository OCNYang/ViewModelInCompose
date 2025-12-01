package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A [ViewModel] that holds a [ViewModelStore] for sharing ViewModels between specific Composable screens.
 *
 * Unlike Activity-scoped or NavGraph-scoped ViewModels, this allows you to define
 * a custom scope that can be shared between specific pages and survives configuration changes.
 *
 * ## Key Features
 * - ✅ Survives configuration changes (screen rotation)
 * - ✅ Thread-safe access to ViewModels
 * - ✅ Automatic cleanup when parent ViewModel is cleared
 * - ✅ Precise lifecycle control: Share ViewModels only between specific screens
 * - ✅ No size limit: Can store any large objects
 * - ✅ Kotlin Multiplatform compatible
 *
 * ## Architecture
 *
 * ```
 * Activity/Fragment ViewModel Store
 *   └─ ScopedViewModelStoreOwner (ViewModel) ← survives config changes
 *       └─ Internal ViewModelStore
 *           └─ SharedViewModel1
 *           └─ SharedViewModel2
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     // Create a scoped store owner - survives configuration changes
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "home_order_scope")
 *
 *     NavHost(navController, startDestination = Route.Home) {
 *         composable<Route.Home> {
 *             val sharedViewModel: SharedViewModel = viewModel(
 *                 viewModelStoreOwner = scopedStoreOwner
 *             )
 *             HomeScreen(sharedViewModel)
 *         }
 *         composable<Route.Order> {
 *             val sharedViewModel: SharedViewModel = viewModel(
 *                 viewModelStoreOwner = scopedStoreOwner
 *             )
 *             OrderScreen(sharedViewModel)
 *         }
 *     }
 * }
 * ```
 *
 * @see rememberScopedViewModelStoreOwner
 */
class ScopedViewModelStoreOwner : ViewModel(), ViewModelStoreOwner {

    /**
     * Synchronization lock for thread-safe operations
     */
    private val lock = SynchronizedObject()

    /**
     * Internal ViewModelStore that holds the shared ViewModels
     */
    private val _viewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    /**
     * Clears all ViewModels stored in this owner.
     *
     * Thread-safe: Can be called from any thread.
     *
     * Call this when the scope is no longer needed, for example when
     * navigating away from all screens that share this ViewModel store.
     *
     * Note: After clearing, new ViewModels can still be created if screens
     * continue to use this owner.
     */
    fun clear() {
        synchronized(lock) {
            _viewModelStore.clear()
        }
    }

    /**
     * Called when this ViewModel is being destroyed.
     * Automatically clears all stored ViewModels.
     */
    override fun onCleared() {
        synchronized(lock) {
            _viewModelStore.clear()
        }
        super.onCleared()
    }
}

/**
 * Creates and remembers a [ScopedViewModelStoreOwner] that survives configuration changes.
 *
 * The owner is stored as a ViewModel in the current ViewModelStoreOwner (Activity/Fragment),
 * so it persists across configuration changes like screen rotation.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     // Create a scoped store owner with a unique key
 *     val scopedStoreOwner = rememberScopedViewModelStoreOwner(key = "home_order_scope")
 *
 *     // Use it to get shared ViewModels
 *     val sharedViewModel: SharedViewModel = viewModel(
 *         viewModelStoreOwner = scopedStoreOwner
 *     )
 * }
 * ```
 *
 * ## Multiple Scopes
 *
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     // Different keys create different scopes
 *     val orderScope = rememberScopedViewModelStoreOwner(key = "order_scope")
 *     val cartScope = rememberScopedViewModelStoreOwner(key = "cart_scope")
 * }
 * ```
 *
 * @param key A unique key to identify this scope. Different keys create different scopes.
 *            Use meaningful names like "order_scope", "checkout_flow", etc.
 * @return A [ScopedViewModelStoreOwner] that survives configuration changes
 */
@Composable
fun rememberScopedViewModelStoreOwner(
    key: String? = null
): ScopedViewModelStoreOwner {
    return viewModel(key = key?.let { "ScopedViewModelStoreOwner_$it" }) {
        ScopedViewModelStoreOwner()
    }
}
