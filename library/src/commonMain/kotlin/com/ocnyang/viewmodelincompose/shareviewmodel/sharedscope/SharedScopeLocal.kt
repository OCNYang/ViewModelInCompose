package com.ocnyang.viewmodelincompose.shareviewmodel.sharedscope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing a [SharedScope] to the composition tree.
 *
 * This is an optional way to access SharedScope from child composables without
 * passing it through function parameters. However, the **recommended** approach
 * is to pass the SharedScope or ViewModel directly as parameters.
 *
 * ## Usage
 *
 * ```kotlin
 * // 1. Provide at a higher level
 * @Composable
 * fun MyNavHost(navController: NavHostController) {
 *     val sharedScope = rememberSharedScope(
 *         isInScope = { ... },
 *         keys = arrayOf(currentDestination)
 *     )
 *
 *     ProvideSharedScope(sharedScope) {
 *         NavHost(navController, startDestination = Route.Home) {
 *             composable<Route.Home> { HomeScreen() }
 *             composable<Route.Order> { OrderScreen() }
 *         }
 *     }
 * }
 *
 * // 2. Use in child composables
 * @Composable
 * fun HomeScreen() {
 *     val sharedScope = LocalSharedScope.current
 *         ?: error("SharedScope not provided")
 *
 *     val sharedViewModel = sharedScope.getViewModel { SharedViewModel() }
 * }
 * ```
 *
 * @see ProvideSharedScope
 * @see SharedScope
 */
val LocalSharedScope = staticCompositionLocalOf<SharedScope?> {
    null
}

/**
 * Provides a [SharedScope] to the composition tree.
 *
 * All child composables can access the provided scope via [LocalSharedScope].
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyNavHost() {
 *     val sharedScope = rememberSharedScope()
 *
 *     ProvideSharedScope(sharedScope) {
 *         // Child composables can now access sharedScope
 *         // via LocalSharedScope.current
 *         NavHost(...)
 *     }
 * }
 * ```
 *
 * @param scope The [SharedScope] to provide
 * @param content The child composables
 */
@Composable
fun ProvideSharedScope(
    scope: SharedScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSharedScope provides scope) {
        content()
    }
}
