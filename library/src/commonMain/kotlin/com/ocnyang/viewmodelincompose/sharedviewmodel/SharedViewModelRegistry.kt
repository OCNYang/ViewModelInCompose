package com.ocnyang.viewmodelincompose.sharedviewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

/**
 * Registry that manages all [SharedViewModelStore] instances and their lifecycles.
 *
 * Implemented as a ViewModel to ensure state is preserved across configuration changes
 * (like screen rotation).
 *
 * ## Architecture
 *
 * ```
 * Activity/Fragment ViewModelStore
 *   └─ SharedViewModelRegistry (ViewModel, registry)
 *       └─ Map<KClass<SharedScope>, SharedViewModelStore>
 *           ├─ OrderFlowScope::class → SharedViewModelStore
 *           │   └─ ViewModelStore
 *           │       └─ SharedOrderViewModel
 *           ├─ CheckoutScope::class → SharedViewModelStore
 *           │   └─ ViewModelStore
 *           │       └─ CheckoutViewModel
 *           └─ ...
 * ```
 *
 * ## Usage
 *
 * Provide via [ProvideSharedViewModelRegistry] at the root:
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideSharedViewModelRegistry {
 *         MyNavHost()
 *     }
 * }
 * ```
 *
 * @see SharedViewModelStore
 * @see ProvideSharedViewModelRegistry
 * @see RegisterSharedScope
 */
class SharedViewModelRegistry : ViewModel() {

    private val lock = SynchronizedObject()

    /**
     * Registry map of SharedViewModelStore instances
     */
    private val registry = mutableMapOf<KClass<out SharedScope>, SharedViewModelStore>()

    /**
     * Gets or creates a [SharedViewModelStore] for the specified scope.
     *
     * If already exists, returns the existing instance; otherwise registers the provided
     * instance and returns it.
     *
     * @param key KClass of the SharedScope
     * @param sharedViewModelStore The store instance to register if not exists
     * @return The store instance
     */
    fun <T : SharedScope> getOrPut(
        key: KClass<T>,
        sharedViewModelStore: SharedViewModelStore
    ): ViewModelStoreOwner {
        return synchronized(lock) {
            registry.getOrPut(key) { sharedViewModelStore }
        }
    }

    /**
     * Gets a [SharedViewModelStore] for the specified scope.
     *
     * @param key KClass of the SharedScope
     * @return The store instance
     * @throws IllegalStateException if not registered
     */
    fun <T : SharedScope> get(key: KClass<T>): ViewModelStoreOwner {
        return synchronized(lock) {
            registry[key]
                ?: error("${key.simpleName} not registered! Please call RegisterSharedScope in your NavHost first.")
        }
    }

    /**
     * Gets a [SharedViewModelStore] for the specified scope, or null if not registered.
     *
     * @param key KClass of the SharedScope
     * @return The store instance, or null
     */
    fun <T : SharedScope> getOrNull(key: KClass<T>): ViewModelStoreOwner? {
        return synchronized(lock) {
            registry[key]
        }
    }

    /**
     * Clears the specified scope.
     *
     * Clears all ViewModels in the store and removes it from the registry.
     *
     * @param key KClass of the SharedScope
     * @return true if cleared, false if not exists
     */
    @PublishedApi
    internal fun clearScope(key: KClass<out SharedScope>): Boolean {
        return synchronized(lock) {
            val store = registry[key] ?: return@synchronized false
            store.clear()
            registry.remove(key)
            true
        }
    }

    /**
     * Checks if the specified scope is registered.
     *
     * @param key KClass of the SharedScope
     * @return true if registered
     */
    fun isRegistered(key: KClass<out SharedScope>): Boolean {
        return synchronized(lock) {
            registry.containsKey(key)
        }
    }


    /**
     * Called when the ViewModel is cleared. Clears all registered stores.
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
 * Creates and remembers a [SharedViewModelRegistry] instance.
 *
 * Internal use only, called via [ProvideSharedViewModelRegistry].
 */
@Composable
fun rememberSharedViewModelRegistry(): SharedViewModelRegistry {
    return viewModel { SharedViewModelRegistry() }
}
