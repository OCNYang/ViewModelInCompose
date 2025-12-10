package com.ocnyang.viewmodelincompose.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A [DisposableEffect] that executes only once per page lifecycle.
 *
 * Unlike standard [DisposableEffect], this survives recomposition and configuration changes.
 * The effect block will only execute once when the page is first created.
 * The [onDispose][DisposableEffectScope.onDispose] block will only execute once when the page
 * is destroyed (ViewModel cleared), not on recomposition or configuration changes.
 *
 * Usage:
 * ```kotlin
 * DisposableEffectOnce {
 *     // Effect code runs once
 *     onDispose {
 *         // Cleanup code runs once when page is destroyed
 *     }
 * }
 * ```
 *
 * @param effect The effect to execute, must return [DisposableEffectResult] via [onDispose][DisposableEffectScope.onDispose]
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun DisposableEffectOnce(
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    val key = rememberSaveable { Uuid.random().toString() }
    val viewModel: DisposableEffectOnceViewModel = viewModel(key = key)

    DisposableEffect(Unit) {
        if (viewModel.tryExecute()) {
            val result = effect()
            viewModel.onDisposeAction = { result.dispose() }
        }
        onDispose { }
    }
}

internal class DisposableEffectOnceViewModel : ViewModel() {
    private var executed = false
    var onDisposeAction: (() -> Unit)? = null

    fun tryExecute(): Boolean {
        if (executed) return false
        executed = true
        return true
    }

    override fun onCleared() {
        super.onCleared()
        onDisposeAction?.invoke()
        onDisposeAction = null
    }
}
