package com.ocnyang.viewmodelincompose.eventeffect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects events from a [Flow] and invokes the [onEvent] callback for each event.
 *
 * This composable is designed for handling one-time UI events from ViewModels,
 * such as navigation, showing toasts, or displaying snackbars.
 *
 * Features:
 * - Lifecycle-aware: only collects events when the lifecycle is at least [minActiveState]
 * - Automatically restarts collection when coming back from background
 * - Each event is consumed exactly once
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun MyScreen(viewModel: MyViewModel) {
 *     EventEffect(viewModel.events) { event ->
 *         when (event) {
 *             is MyEvent.ShowToast -> showToast(event.message)
 *             is MyEvent.Navigate -> navigator.navigate(event.route)
 *         }
 *     }
 * }
 * ```
 *
 * @param flow The flow of events to collect
 * @param minActiveState The minimum lifecycle state in which events should be collected.
 *                       Default is [Lifecycle.State.STARTED], which means events are only
 *                       processed when the UI is visible.
 * @param onEvent The callback to invoke for each event
 */
@Composable
@NonRestartableComposable
fun <T> EventEffect(
    flow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect { event ->
                onEvent(event)
            }
        }
    }
}

/**
 * Collects events from a [Flow] and invokes the [onEvent] callback for each event,
 * with additional keys to control recomposition.
 *
 * Use this overload when you need the effect to restart based on additional keys,
 * such as a screen ID or user ID.
 *
 * @param flow The flow of events to collect
 * @param key1 Additional key to control when the effect restarts
 * @param minActiveState The minimum lifecycle state in which events should be collected
 * @param onEvent The callback to invoke for each event
 */
@Composable
@NonRestartableComposable
fun <T> EventEffect(
    flow: Flow<T>,
    key1: Any?,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, key1, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect { event ->
                onEvent(event)
            }
        }
    }
}

/**
 * Collects events from a [Flow] and invokes the [onEvent] callback for each event,
 * with additional keys to control recomposition.
 *
 * @param flow The flow of events to collect
 * @param key1 First additional key to control when the effect restarts
 * @param key2 Second additional key to control when the effect restarts
 * @param minActiveState The minimum lifecycle state in which events should be collected
 * @param onEvent The callback to invoke for each event
 */
@Composable
@NonRestartableComposable
fun <T> EventEffect(
    flow: Flow<T>,
    key1: Any?,
    key2: Any?,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, key1, key2, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect { event ->
                onEvent(event)
            }
        }
    }
}

/**
 * Collects events from a [Flow] and invokes the [onEvent] callback for each event,
 * with additional keys to control recomposition.
 *
 * @param flow The flow of events to collect
 * @param keys Additional keys to control when the effect restarts
 * @param minActiveState The minimum lifecycle state in which events should be collected
 * @param onEvent The callback to invoke for each event
 */
@Composable
@NonRestartableComposable
fun <T> EventEffect(
    flow: Flow<T>,
    vararg keys: Any?,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(flow, *keys, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect { event ->
                onEvent(event)
            }
        }
    }
}
