package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

/**
 * Scope registry that manages all [SharedViewModelStoreOwner] instances and their lifecycles.
 *
 * Implemented as a ViewModel to ensure state is preserved across configuration changes
 * (like screen rotation).
 *
 * ## Architecture
 *
 * ```
 * Activity/Fragment ViewModelStore
 *   └─ ScopedViewModelStoreOwner (ViewModel, registry)
 *       └─ Map<KClass, SharedViewModelStoreOwner>
 *           ├─ OrderFlowStoreOwner::class → OrderFlowStoreOwner
 *           │   └─ ViewModelStore
 *           │       └─ SharedOrderViewModel
 *           ├─ CheckoutStoreOwner::class → CheckoutStoreOwner
 *           │   └─ ViewModelStore
 *           │       └─ CheckoutViewModel
 *           └─ ...
 * ```
 *
 * ## Usage
 *
 * Provide via [ProvideScopedViewModelStoreOwner] at the root:
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideScopedViewModelStoreOwner {
 *         MyNavHost()
 *     }
 * }
 * ```
 *
 * @see SharedViewModelStoreOwner
 * @see ProvideScopedViewModelStoreOwner
 * @see RegisterSharedStoreOwner
 */
class ScopedViewModelStoreOwner : ViewModel() {

    private val lock = SynchronizedObject()

    /**
     * Registry map of SharedViewModelStoreOwner instances
     */
    private val registry = mutableMapOf<KClass<out SharedViewModelStoreOwner>, SharedViewModelStoreOwner>()

    /**
     * Gets or creates a [SharedViewModelStoreOwner] of the specified type.
     *
     * If already exists, returns the existing instance; otherwise calls factory to create
     * a new instance and registers it.
     *
     * @param key KClass of the StoreOwner
     * @param factory Factory function to create the StoreOwner
     * @return The StoreOwner instance
     */
    fun <T : SharedViewModelStoreOwner> getOrPut(
        key: KClass<T>,
        factory: () -> T
    ): T {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry.getOrPut(key) { factory() } as T
        }
    }

    /**
     * Gets a [SharedViewModelStoreOwner] of the specified type.
     *
     * @param key KClass of the StoreOwner
     * @return The StoreOwner instance
     * @throws IllegalStateException if not registered
     */
    fun <T : SharedViewModelStoreOwner> get(key: KClass<T>): T {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry[key] as? T
                ?: error("${key.simpleName} not registered! Please call RegisterSharedStoreOwner in your NavHost first.")
        }
    }

    /**
     * Gets a [SharedViewModelStoreOwner] of the specified type, or null if not registered.
     *
     * @param key KClass of the StoreOwner
     * @return The StoreOwner instance, or null
     */
    fun <T : SharedViewModelStoreOwner> getOrNull(key: KClass<T>): T? {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry[key] as? T
        }
    }

    /**
     * Clears the specified scope.
     *
     * Clears all ViewModels in the StoreOwner and removes it from the registry.
     *
     * @param key KClass of the StoreOwner
     * @return true if cleared, false if not exists
     */
    @PublishedApi
    internal fun clearScope(key: KClass<out SharedViewModelStoreOwner>): Boolean {
        return synchronized(lock) {
            val owner = registry[key] ?: return@synchronized false
            owner.clear()
            registry.remove(key)
            true
        }
    }

    /**
     * Checks if the specified scope is registered.
     *
     * @param key KClass of the StoreOwner
     * @return true if registered
     */
    fun isRegistered(key: KClass<out SharedViewModelStoreOwner>): Boolean {
        return synchronized(lock) {
            registry.containsKey(key)
        }
    }

    /**
     * Gets information about all registered scopes (for debugging).
     *
     * @return Map<Scope name, Set of included route names>
     */
    fun getAllRegisteredInfo(): Map<String, Set<String>> {
        return synchronized(lock) {
            registry.mapKeys { it.key.simpleName ?: "Unknown" }
                .mapValues { entry ->
                    entry.value.includedRoutes.map { it.simpleName ?: "Unknown" }.toSet()
                }
        }
    }

    /**
     * Called when the ViewModel is cleared. Clears all registered StoreOwners.
     */
    override fun onCleared() {
        synchronized(lock) {
            registry.values.forEach { it.clear() }
            registry.clear()
        }
        super.onCleared()
    }
}

/**
 * Creates and remembers a [ScopedViewModelStoreOwner] instance.
 *
 * Internal use only, called via [ProvideScopedViewModelStoreOwner].
 */
@Composable
fun rememberScopedViewModelStoreOwner(): ScopedViewModelStoreOwner {
    return viewModel { ScopedViewModelStoreOwner() }
}
