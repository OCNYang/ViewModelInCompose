package com.ocnyang.viewmodelincompose.sharedviewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Gets a ViewModel from the specified scope.
 *
 * This is a convenience function that combines [getSharedViewModelStore] and [viewModel] into a single call.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val sharedVm = sharedViewModel<OrderFlowScope, SharedOrderViewModel> {
 *         SharedOrderViewModel()
 *     }
 *     // Use sharedVm...
 * }
 * ```
 *
 * This is equivalent to:
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val store = getSharedViewModelStore<OrderFlowScope>()
 *     val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = store) {
 *         SharedOrderViewModel()
 *     }
 * }
 * ```
 *
 * @param S The type of [SharedScope] to use
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance
 * @throws IllegalStateException if [LocalSharedViewModelRegistry] is not provided
 * @throws IllegalStateException if the specified scope is not registered
 */
@Composable
inline fun <reified S : SharedScope, reified T : ViewModel> sharedViewModel(
    key: String? = null,
    noinline factory: () -> T
): T {
    val store = getSharedViewModelStore<S>()

    return viewModel(
        viewModelStoreOwner = store,
        key = key,
        initializer = { factory() }
    )
}

/**
 * Gets a ViewModel from the specified scope, or null if not registered.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val sharedVm = sharedViewModelOrNull<OrderFlowScope, SharedOrderViewModel> {
 *         SharedOrderViewModel()
 *     }
 *
 *     if (sharedVm != null) {
 *         // Use shared data
 *     } else {
 *         // Fallback behavior
 *     }
 * }
 * ```
 *
 * @param S The type of [SharedScope] to use
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance, or null if the scope is not registered
 */
@Composable
inline fun <reified S : SharedScope, reified T : ViewModel> sharedViewModelOrNull(
    key: String? = null,
    noinline factory: () -> T
): T? {
    val store = getSharedViewModelStoreOrNull<S>() ?: return null

    return viewModel(
        viewModelStoreOwner = store,
        key = key,
        initializer = { factory() }
    )
}
