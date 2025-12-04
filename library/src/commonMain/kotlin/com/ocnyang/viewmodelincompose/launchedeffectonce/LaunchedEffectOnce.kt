package com.ocnyang.viewmodelincompose.launchedeffectonce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope

/**
 * A [LaunchedEffect] that executes only once per page lifecycle.
 *
 * Unlike standard [LaunchedEffect], this survives recomposition and configuration changes.
 * The block will only re-execute when the page is destroyed and recreated.
 *
 * @param viewModelKey Optional key to distinguish multiple independent effects
 * @param block The suspend function to execute
 */
@Composable
fun LaunchedEffectOnce(
    viewModelKey: String? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val viewModel: LaunchedOnceViewModel = viewModel(key = viewModelKey)

    LaunchedEffect(Unit) {
        if (viewModel.tryExecute()) {
            block()
        }
    }
}

/**
 * A [LaunchedEffect] that executes only once per key value.
 *
 * Re-executes when [key1] changes. For the same key value, executes only once per page lifecycle.
 *
 * @param key1 Key to control re-execution
 * @param viewModelKey Optional key to distinguish multiple independent effects
 * @param block The suspend function to execute
 */
@Composable
fun LaunchedEffectOnce(
    key1: Any?,
    viewModelKey: String? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val viewModel: LaunchedOnceViewModel = viewModel(key = viewModelKey)
    val keyHash = remember(key1) { key1.hashCode() }

    LaunchedEffect(key1) {
        if (viewModel.tryExecute(keyHash)) {
            block()
        }
    }
}

/**
 * A [LaunchedEffect] that executes only once per key combination.
 *
 * Re-executes when any key changes.
 */
@Composable
fun LaunchedEffectOnce(
    key1: Any?,
    key2: Any?,
    viewModelKey: String? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val viewModel: LaunchedOnceViewModel = viewModel(key = viewModelKey)
    val keyHash = remember(key1, key2) { arrayOf(key1, key2).contentHashCode() }

    LaunchedEffect(key1, key2) {
        if (viewModel.tryExecute(keyHash)) {
            block()
        }
    }
}

/**
 * A [LaunchedEffect] that executes only once per key combination.
 *
 * Re-executes when any key changes.
 */
@Composable
fun LaunchedEffectOnce(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    viewModelKey: String? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val viewModel: LaunchedOnceViewModel = viewModel(key = viewModelKey)
    val keyHash = remember(key1, key2, key3) { arrayOf(key1, key2, key3).contentHashCode() }

    LaunchedEffect(key1, key2, key3) {
        if (viewModel.tryExecute(keyHash)) {
            block()
        }
    }
}

/**
 * A [LaunchedEffect] that executes only once per key combination.
 *
 * Re-executes when any key changes.
 */
@Composable
fun LaunchedEffectOnce(
    vararg keys: Any?,
    viewModelKey: String? = null,
    block: suspend CoroutineScope.() -> Unit,
) {
    val viewModel: LaunchedOnceViewModel = viewModel(key = viewModelKey)
    val keyHash = remember(*keys) { keys.contentHashCode() }

    LaunchedEffect(*keys) {
        if (viewModel.tryExecute(keyHash)) {
            block()
        }
    }
}

/**
 * Internal ViewModel to track execution state across recompositions.
 */
internal class LaunchedOnceViewModel : ViewModel() {

    private val executedKeyHash = atomic<Int?>(null)

    fun tryExecute(): Boolean = tryExecute(NO_KEY_HASH)

    fun tryExecute(keyHash: Int): Boolean {
        while (true) {
            val current = executedKeyHash.value
            if (current == keyHash) return false
            if (executedKeyHash.compareAndSet(current, keyHash)) return true
        }
    }

    companion object {
        private const val NO_KEY_HASH = Int.MIN_VALUE
    }
}
