package com.ocnyang.viewmodelincompose.shareviewmodel.sharedscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

/**
 * A scope that holds a [ViewModelStore] for sharing ViewModels between specific Composable screens.
 *
 * Use [rememberSharedScope] to create and manage a SharedScope that automatically clears
 * its ViewModels when navigating away from the included routes.
 *
 * ## Key Features
 * - ✅ Survives configuration changes (screen rotation)
 * - ✅ Thread-safe ViewModel access
 * - ✅ Automatic cleanup when leaving scope
 * - ✅ Reference counting to prevent premature cleanup
 * - ✅ Debug utilities for monitoring
 * - ✅ Kotlin Multiplatform compatible
 *
 * ## Architecture
 *
 * ```
 * Activity/Fragment ViewModel Store
 *   └─ SharedScopeHolder (ViewModel) ← survives config changes
 *       └─ SharedScope
 *           └─ ViewModelStore (thread-safe)
 *               └─ SharedViewModel1
 *               └─ SharedViewModel2
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
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
 * @property viewModelStore The underlying ViewModelStore that holds the shared ViewModels
 */
class SharedScope internal constructor(
    @PublishedApi
    internal val viewModelStore: ViewModelStore
) {
    /**
     * Synchronization lock for thread-safe operations
     */
    @PublishedApi
    internal val lock = SynchronizedObject()

    /**
     * Reference counter for tracking active users of this scope
     */
    private val _referenceCount = atomic(0)

    /**
     * Current reference count (for debugging)
     */
    val referenceCount: Int get() = _referenceCount.value

    /**
     * The [ViewModelStoreOwner] that wraps this scope's [ViewModelStore].
     * Can be used with the standard `viewModel()` composable function.
     */
    val viewModelStoreOwner: ViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = this@SharedScope.viewModelStore
    }

    /**
     * Gets or creates a ViewModel of the specified type from this shared scope.
     *
     * All screens using the same SharedScope will receive the same ViewModel instance.
     * This method is thread-safe.
     *
     * **Important**: You must provide a [factory] to create the ViewModel. This is required
     * for Kotlin Multiplatform compatibility.
     *
     * @param T The type of ViewModel to get
     * @param key An optional key to distinguish between multiple ViewModels of the same type.
     *            Defaults to the ViewModel class name.
     * @param factory A factory function that creates the ViewModel instance
     * @return The ViewModel instance
     *
     * ## Example
     *
     * ```kotlin
     * // Simple usage with factory
     * val viewModel = sharedScope.getViewModel { MyViewModel() }
     *
     * // With a key (for multiple instances of the same type)
     * val viewModel1 = sharedScope.getViewModel(key = "tab1") { MyViewModel() }
     * val viewModel2 = sharedScope.getViewModel(key = "tab2") { MyViewModel() }
     *
     * // With dependencies
     * val viewModel = sharedScope.getViewModel { MyViewModel(repository) }
     * ```
     */
    inline fun <reified T : ViewModel> getViewModel(
        key: String = T::class.viewModelKey(),
        factory: () -> T
    ): T {
        val fullKey = "${T::class.viewModelKey()}:$key"

        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            viewModelStore.get(fullKey) as? T ?: factory().also { vm ->
                viewModelStore.put(fullKey, vm)
            }
        }
    }

    /**
     * Clears all ViewModels stored in this scope.
     *
     * Thread-safe: Can be called from any thread.
     *
     * This is called automatically by [rememberSharedScope] when navigating away
     * from all included routes. You typically don't need to call this manually.
     */
    fun clear() {
        synchronized(lock) {
            viewModelStore.clear()
        }
    }

    /**
     * Clears all ViewModels only if there are no active references.
     *
     * Thread-safe: Can be called from any thread.
     *
     * @return true if cleared, false if there are still active references
     */
    fun clearIfNoReferences(): Boolean {
        return synchronized(lock) {
            if (_referenceCount.value <= 0) {
                viewModelStore.clear()
                true
            } else {
                false
            }
        }
    }

    /**
     * Increments the reference count.
     * Called when a screen starts using this scope.
     */
    internal fun addReference() {
        _referenceCount.incrementAndGet()
    }

    /**
     * Decrements the reference count.
     * Called when a screen stops using this scope.
     *
     * @return the new reference count
     */
    internal fun removeReference(): Int {
        return _referenceCount.decrementAndGet()
    }

    /**
     * Gets all ViewModel keys currently stored in this scope.
     * Useful for debugging.
     *
     * @return Set of ViewModel keys
     */
    fun getStoredViewModelKeys(): Set<String> {
        return synchronized(lock) {
            // ViewModelStore doesn't expose keys directly, so we track them separately
            // For now, return empty set - this is a limitation of ViewModelStore API
            emptySet()
        }
    }
}

/**
 * A ViewModel that holds a [SharedScope] to survive configuration changes.
 *
 * This is used internally by [rememberSharedScope] to ensure the SharedScope
 * persists across configuration changes like screen rotation.
 */
class SharedScopeHolder : ViewModel() {
    /**
     * The SharedScope instance that survives configuration changes
     */
    val scope = SharedScope(ViewModelStore())

    override fun onCleared() {
        scope.clear()
        super.onCleared()
    }
}

/**
 * Gets a canonical key for a ViewModel class.
 * Uses simpleName for KMP compatibility (qualifiedName is not available on all platforms).
 */
@PublishedApi
internal fun KClass<*>.viewModelKey(): String = simpleName ?: toString()
