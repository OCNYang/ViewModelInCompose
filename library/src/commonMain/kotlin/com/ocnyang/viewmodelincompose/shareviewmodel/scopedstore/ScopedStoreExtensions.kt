package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Gets a ViewModel from the [LocalScopedViewModelStoreOwner].
 *
 * This is a convenience function that combines [LocalScopedViewModelStoreOwner.current]
 * and [viewModel] into a single call.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val sharedViewModel = scopedViewModel { SharedViewModel() }
 *     // Use sharedViewModel...
 * }
 * ```
 *
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance
 * @throws IllegalStateException if [LocalScopedViewModelStoreOwner] is not provided
 */
@Composable
inline fun <reified T : ViewModel> scopedViewModel(
    key: String? = null,
    crossinline factory: () -> T
): T {
    val scopedStoreOwner = LocalScopedViewModelStoreOwner.current
        ?: error("ScopedViewModelStoreOwner not provided! Please wrap your composables with ProvideScopedViewModelStoreOwner { ... }")

    return viewModel(
        viewModelStoreOwner = scopedStoreOwner,
        key = key,
        initializer = { factory() }
    )
}

/**
 * Tries to get a ViewModel from the [LocalScopedViewModelStoreOwner] if available,
 * otherwise returns null.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val sharedViewModel = scopedViewModelOrNull { SharedViewModel() }
 *     if (sharedViewModel != null) {
 *         // Use shared data
 *     } else {
 *         // Fallback behavior
 *     }
 * }
 * ```
 *
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance, or null if not in a scoped context
 */
@Composable
inline fun <reified T : ViewModel> scopedViewModelOrNull(
    key: String? = null,
    crossinline factory: () -> T
): T? {
    val scopedStoreOwner = LocalScopedViewModelStoreOwner.current
        ?: return null

    return viewModel(
        viewModelStoreOwner = scopedStoreOwner,
        key = key,
        initializer = { factory() }
    )
}
