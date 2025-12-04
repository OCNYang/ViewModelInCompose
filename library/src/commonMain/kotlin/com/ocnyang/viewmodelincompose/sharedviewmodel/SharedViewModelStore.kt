package com.ocnyang.viewmodelincompose.sharedviewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * A [ViewModelStoreOwner] that holds ViewModels for a shared scope.
 *
 * This class is used internally by the shared ViewModel system to hold ViewModels.
 * Users should not use this class directly. Instead, define a [SharedScope]
 * to identify the scope.
 *
 * ## Architecture
 *
 * ```
 * SharedScope (scope identifier with includedRoutes)
 *     └─ SharedViewModelStore (ViewModelStore holder)
 *         └─ ViewModelStore
 *             └─ ViewModels
 * ```
 *
 * @see SharedScope
 * @see SharedViewModelRegistry
 */
class SharedViewModelStore : ViewModelStoreOwner {

    private val _viewModelStore by lazy { ViewModelStore() }

    override val viewModelStore: ViewModelStore get() = _viewModelStore

    /**
     * Clears all ViewModels in this store.
     *
     * After calling this, `onCleared()` will be called on all ViewModels stored here.
     */
    fun clear() {
        _viewModelStore.clear()
    }
}
