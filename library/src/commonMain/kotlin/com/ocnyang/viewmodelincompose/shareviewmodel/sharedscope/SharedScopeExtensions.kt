package com.ocnyang.viewmodelincompose.shareviewmodel.sharedscope

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

/**
 * Gets a ViewModel from the [LocalSharedScope].
 *
 * This is a convenience function that combines [LocalSharedScope.current]
 * and [SharedScope.getViewModel] into a single call.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val sharedViewModel = sharedScopeViewModel { SharedViewModel() }
 *     // Use sharedViewModel...
 * }
 * ```
 *
 * @param T The type of ViewModel to get
 * @param key An optional key to distinguish between multiple ViewModels of the same type
 * @param factory A factory function that creates the ViewModel instance
 * @return The ViewModel instance
 * @throws IllegalStateException if [LocalSharedScope] is not provided
 */
@Composable
inline fun <reified T : ViewModel> sharedScopeViewModel(
    key: String = T::class.viewModelKey(),
    factory: () -> T
): T {
    val sharedScope = LocalSharedScope.current
        ?: error("SharedScope not provided! Please wrap your composables with ProvideSharedScope { ... }")

    return sharedScope.getViewModel(key = key, factory = factory)
}

/**
 * Tries to get a ViewModel from the [LocalSharedScope] if available,
 * otherwise returns null.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val sharedViewModel = sharedScopeViewModelOrNull { SharedViewModel() }
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
 * @return The ViewModel instance, or null if not in a shared scope context
 */
@Composable
inline fun <reified T : ViewModel> sharedScopeViewModelOrNull(
    key: String = T::class.viewModelKey(),
    factory: () -> T
): T? {
    val sharedScope = LocalSharedScope.current
        ?: return null

    return sharedScope.getViewModel(key = key, factory = factory)
}
