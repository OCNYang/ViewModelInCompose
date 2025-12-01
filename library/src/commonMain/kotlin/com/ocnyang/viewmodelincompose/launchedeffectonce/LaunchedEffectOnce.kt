package com.ocnyang.viewmodelincompose.launchedeffectonce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope

/**
 * 页面生命周期内只执行一次的 LaunchedEffect
 *
 * 与标准 [LaunchedEffect] 的区别：
 * - 标准 LaunchedEffect: 每次重组都可能执行（取决于 key 是否变化）
 * - LaunchedEffectOnce: 在页面生命周期内只执行一次，即使重组也不会重复执行
 *
 * 使用场景：
 * - 页面初始化时的一次性操作（如加载数据、埋点上报）
 * - 只需执行一次的副作用
 *
 * ## 基本用法
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     // 页面生命周期内只执行一次
 *     LaunchedEffectOnce {
 *         viewModel.loadData()
 *         analytics.trackPageView()
 *     }
 * }
 * ```
 *
 * ## 使用 key 控制重新执行
 *
 * ```kotlin
 * @Composable
 * fun UserScreen(userId: String) {
 *     // userId 变化时会重新执行
 *     LaunchedEffectOnce(userId) {
 *         viewModel.loadUser(userId)
 *     }
 * }
 * ```
 *
 * ## 多个独立的一次性逻辑
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     // 使用不同的 viewModelKey 区分不同的逻辑
 *     LaunchedEffectOnce(viewModelKey = "loadData") {
 *         viewModel.loadData()
 *     }
 *
 *     LaunchedEffectOnce(viewModelKey = "analytics") {
 *         analytics.trackPageView()
 *     }
 * }
 * ```
 *
 * ## 配置更改行为
 *
 * - ✅ 屏幕旋转后不会重复执行（ViewModel 保留状态）
 * - ✅ 页面从后退栈恢复时不会重复执行
 * - ✅ 页面销毁后重新进入会执行（ViewModel 被清理）
 *
 * @param viewModelKey 用于区分不同的一次性逻辑，默认为 null
 * @param block 要执行的挂起函数
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
 * 带有一个 key 的 LaunchedEffectOnce
 *
 * 当 key 发生变化时，会重新执行 block。
 * 对于相同的 key 值，在页面生命周期内只执行一次。
 *
 * ```kotlin
 * @Composable
 * fun UserScreen(userId: String) {
 *     LaunchedEffectOnce(userId) {
 *         // userId 变化时重新执行
 *         viewModel.loadUser(userId)
 *     }
 * }
 * ```
 *
 * @param key1 用于控制执行的 key，key 变化时会重新执行
 * @param viewModelKey 用于区分不同的一次性逻辑
 * @param block 要执行的挂起函数
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
 * 带有两个 key 的 LaunchedEffectOnce
 *
 * 当任一 key 发生变化时，会重新执行 block。
 *
 * @param key1 第一个 key
 * @param key2 第二个 key
 * @param viewModelKey 用于区分不同的一次性逻辑
 * @param block 要执行的挂起函数
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
 * 带有三个 key 的 LaunchedEffectOnce
 *
 * 当任一 key 发生变化时，会重新执行 block。
 *
 * @param key1 第一个 key
 * @param key2 第二个 key
 * @param key3 第三个 key
 * @param viewModelKey 用于区分不同的一次性逻辑
 * @param block 要执行的挂起函数
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
 * 带有可变数量 key 的 LaunchedEffectOnce
 *
 * 当任一 key 发生变化时，会重新执行 block。
 *
 * ```kotlin
 * @Composable
 * fun ComplexScreen(id: String, type: String, version: Int) {
 *     LaunchedEffectOnce(id, type, version) {
 *         viewModel.loadData(id, type, version)
 *     }
 * }
 * ```
 *
 * @param keys 用于控制执行的 keys
 * @param viewModelKey 用于区分不同的一次性逻辑
 * @param block 要执行的挂起函数
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
 * 内部 ViewModel，用于跟踪执行状态
 *
 * 使用原子操作确保线程安全
 */
internal class LaunchedOnceViewModel : ViewModel() {
    /**
     * 已执行的 key 哈希值
     * - null: 从未执行过
     * - 其他值: 上次执行时的 key 哈希值
     */
    private val executedKeyHash = atomic<Int?>(null)

    /**
     * 尝试执行（无 key 版本）
     *
     * @return true 如果应该执行，false 如果已经执行过
     */
    fun tryExecute(): Boolean {
        // 使用特殊值表示无 key 的情况
        return tryExecute(NO_KEY_HASH)
    }

    /**
     * 尝试执行（带 key 版本）
     *
     * 原子操作：检查当前 hash 是否与上次相同，如果不同则更新并返回 true
     *
     * @param keyHash 当前 key 的哈希值
     * @return true 如果应该执行（首次或 key 变化），false 如果 key 相同已执行过
     */
    fun tryExecute(keyHash: Int): Boolean {
        // compareAndSet 循环，确保原子性
        while (true) {
            val current = executedKeyHash.value
            if (current == keyHash) {
                // 已经用相同的 key 执行过
                return false
            }
            // 尝试更新，如果成功则执行
            if (executedKeyHash.compareAndSet(current, keyHash)) {
                return true
            }
            // 如果 CAS 失败，说明其他线程修改了，重试
        }
    }

    /**
     * 重置状态，允许再次执行
     *
     * 调用后，下次 tryExecute 会返回 true
     */
    fun reset() {
        executedKeyHash.value = null
    }

    companion object {
        /**
         * 无 key 情况的特殊哈希值
         * 使用 Int.MIN_VALUE 避免与实际 key 冲突
         */
        private const val NO_KEY_HASH = Int.MIN_VALUE
    }
}