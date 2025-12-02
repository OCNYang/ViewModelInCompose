package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Gets a ViewModel from the specified [SharedViewModelStoreOwner].
 *
 * This is a convenience function that combines [getSharedStoreOwner] and [viewModel] into a single call.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val sharedVm = sharedViewModel<OrderFlowStoreOwner, SharedOrderViewModel> {
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
 *     val storeOwner = getSharedStoreOwner<OrderFlowStoreOwner>()
 *     val sharedVm: SharedOrderViewModel = viewModel(viewModelStoreOwner = storeOwner) {
 *         SharedOrderViewModel()
 *     }
 * }
 * ```
 *
 * @param S The type of [SharedViewModelStoreOwner] to use
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance
 * @throws IllegalStateException if [LocalScopedViewModelStoreOwner] is not provided
 * @throws IllegalStateException if the specified StoreOwner type is not registered
 */
@Composable
inline fun <reified S : SharedViewModelStoreOwner, reified T : ViewModel> sharedViewModel(
    key: String? = null,
    noinline factory: () -> T
): T {
    val storeOwner = getSharedStoreOwner<S>()

    return viewModel(
        viewModelStoreOwner = storeOwner,
        key = key,
        initializer = { factory() }
    )
}

/**
 * Gets a ViewModel from the specified [SharedViewModelStoreOwner], or null if not registered.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val sharedVm = sharedViewModelOrNull<OrderFlowStoreOwner, SharedOrderViewModel> {
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
 * @param S The type of [SharedViewModelStoreOwner] to use
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance, or null if the StoreOwner is not registered
 */
@Composable
inline fun <reified S : SharedViewModelStoreOwner, reified T : ViewModel> sharedViewModelOrNull(
    key: String? = null,
    noinline factory: () -> T
): T? {
    val storeOwner = getSharedStoreOwnerOrNull<S>() ?: return null

    return viewModel(
        viewModelStoreOwner = storeOwner,
        key = key,
        initializer = { factory() }
    )
}
