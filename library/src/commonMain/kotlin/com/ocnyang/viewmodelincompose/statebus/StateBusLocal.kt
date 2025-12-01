/*
 * StateBus - CompositionLocal 支持
 *
 * 提供全局访问 StateBus 的便捷方式
 */

package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for StateBus
 *
 * 使用方式：
 * ```kotlin
 * // 1. 在根级别提供
 * @Composable
 * fun MyApp() {
 *     val stateBus = rememberStateBus()
 *
 *     ProvideStateBus(stateBus) {
 *         // 你的 UI
 *         MyNavHost()
 *     }
 * }
 *
 * // 2. 在任意子组件中使用
 * @Composable
 * fun MyScreen() {
 *     val stateBus = LocalStateBus.current
 *     val user = stateBus.observeState<User?>()
 * }
 * ```
 */
val LocalStateBus = compositionLocalOf<StateBus> {
    error("No StateBus provided! Please wrap your app with ProvideStateBus { ... }")
}

/**
 * 提供 StateBus 给整个组合树
 *
 * 推荐在应用根级别使用：
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     val stateBus = rememberStateBus()
 *
 *     ProvideStateBus(stateBus) {
 *         MaterialTheme {
 *             NavHost(...)
 *         }
 *     }
 * }
 * ```
 *
 * @param stateBus StateBus 实例
 * @param content 子组件
 */
@Composable
fun ProvideStateBus(
    stateBus: StateBus,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalStateBus provides stateBus) {
        content()
    }
}

/**
 * 便捷扩展：自动创建并提供 StateBus
 *
 * 使用方式：
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     ProvideStateBus {
 *         MaterialTheme {
 *             NavHost(...)
 *         }
 *     }
 * }
 * ```
 *
 * @param content 子组件
 */
@Composable
fun ProvideStateBus(
    content: @Composable () -> Unit
) {
    val stateBus = rememberStateBus()
    ProvideStateBus(stateBus, content)
}
