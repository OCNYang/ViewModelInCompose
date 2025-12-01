/*
 * StateBus - Production-Ready State Management for Compose Multiplatform
 *
 * 生产级别的状态管理工具，支持：
 * 1. ✅ 自动追踪监听者数量
 * 2. ✅ 自动资源清理（当监听者数量为 0 时）
 * 3. ✅ 线程安全（SynchronizedObject + AtomicInt）
 * 4. ✅ 配置更改恢复（ViewModel 自动保留）
 * 5. ✅ 进程死亡恢复（Android 平台通过 SavedStateHandle）
 * 6. ✅ 生命周期完全对齐（两级 ViewModel 架构）
 * 7. ✅ Kotlin Multiplatform 支持
 */

package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * StateBus - 页面间状态传递和共享的解决方案
 *
 * 基于 ViewModel 实现，特点：
 * - 自动追踪监听者数量，无监听者时自动清理
 * - 线程安全，支持多线程访问
 * - 配置更改时自动保留（ViewModel 特性）
 * - 进程死亡后自动恢复（Android 平台通过 SavedStateHandle）
 * - 两级 ViewModel 架构，生命周期完全对齐
 * - Kotlin Multiplatform 支持
 *
 * 架构设计：
 * ```
 * Activity/Fragment (Android) / Window (Desktop) / etc.
 *   └─ StateBus (ViewModel)  // 顶层级别
 *       └─ stateDataMap (内存缓存，线程安全)
 *       └─ persistence (平台特定持久化，Android 使用 SavedStateHandle)
 *
 * NavBackStackEntry (PageA)
 *   └─ StateBusListenerViewModel  // 页面级别
 *       └─ 注册/取消注册监听者
 * ```
 *
 * 使用示例：
 * ```kotlin
 * // 1. 创建 StateBus
 * val stateBus = rememberStateBus()
 *
 * // 2. 监听状态
 * @Composable
 * fun HomeScreen() {
 *     val person = stateBus.observeState<Person?>()
 *     Text("Person: ${person?.name}")
 * }
 *
 * // 3. 设置状态
 * fun onSubmit(person: Person) {
 *     stateBus.setState<Person>(person)
 * }
 * ```
 *
 * 注意事项：
 * - 泛型类型建议显式指定 stateKey 避免冲突
 * - Android 平台：状态类型必须支持序列化（Parcelable/Serializable/基本类型）才能在进程死亡后恢复
 */
class StateBus(
    @PublishedApi
    internal val persistence: StatePersistence = NoOpStatePersistence
) : ViewModel() {

    /**
     * 同步锁对象，用于保护 stateDataMap 的并发访问
     */
    @PublishedApi
    internal val lock = SynchronizedObject()

    /**
     * 结果数据
     * @param state 实际的状态值
     * @param listenerCount 监听者数量（线程安全的原子计数器）
     */
    @PublishedApi
    internal class StateData(
        val state: MutableState<Any?>,
        listenerCount: Int = 0
    ) {
        private val _listenerCount: AtomicInt = atomic(listenerCount)

        val listenerCount: Int get() = _listenerCount.value

        fun incrementListenerCount(): Int = _listenerCount.incrementAndGet()

        fun decrementListenerCount(): Int = _listenerCount.decrementAndGet()
    }

    /**
     * 线程安全的状态数据 Map（内存缓存）
     *
     * 注意：使用 @PublishedApi internal，允许 inline 函数访问
     */
    @PublishedApi
    internal val stateDataMap = mutableMapOf<String, StateData>()

    // ============ 公共 API ============

    /**
     * 观察状态（自动追踪监听者）
     *
     * 当 Composable 进入时自动注册为监听者，离开时自动取消注册。
     * 当最后一个监听者离开时，自动清理状态数据。
     *
     * 懒加载恢复（Android 平台）：
     * - 首次访问时，如果内存中没有数据，会从 SavedStateHandle 恢复
     * - 进程死亡后重启，数据会自动恢复
     *
     * 注意事项：
     * - ⚠️ 不要在 LazyColumn/LazyRow 的 item 中直接调用
     * - ✅ 应该在外层调用，然后传递给 item
     * - ✅ key 应该保持稳定，避免不必要的重新注册
     * - ✅ 泛型类型建议显式指定 stateKey（如 Result<String> 和 Result<Int>）
     * - ✅ Android 平台：状态类型必须可序列化才能在进程死亡后恢复
     *
     * @param stateKey 状态的唯一标识，默认使用类型名称
     * @return 状态值，如果不存在则返回 null
     */
    @Composable
    inline fun <reified T> observeState(
        stateKey: String = T::class.simpleName ?: T::class.toString()
    ): T? {
        // 创建监听者 ViewModel，绑定到当前 Composable 的作用域（NavBackStackEntry）
        viewModel(
            key = "StateBus_$stateKey",  // 加前缀避免冲突
            initializer = { StateBusListenerViewModel(stateKey = stateKey, stateBus = this@StateBus) }
        )

        // 获取或创建 StateData（支持从持久化恢复）
        val data = getOrCreateStateData(stateKey)
        @Suppress("UNCHECKED_CAST")
        return data.state.value as? T
    }

    /**
     * 观察状态（返回 State 对象）
     *
     * 与 observeState() 类似，但返回 State 对象，可以用于需要 State 类型的场景。
     *
     * @param stateKey 状态的唯一标识
     * @return State<T?> 对象
     */
    @Composable
    inline fun <reified T> observeStateAsState(
        stateKey: String = T::class.simpleName ?: T::class.toString(),
    ): State<T?> {
        viewModel(
            key = "StateBus_$stateKey",
            initializer = { StateBusListenerViewModel(stateKey = stateKey, stateBus = this@StateBus) }
        )

        // 获取或创建 StateData（支持从持久化恢复）
        val data = getOrCreateStateData(stateKey)

        return remember(data.state) {
            object : State<T?> {
                @Suppress("UNCHECKED_CAST")
                override val value: T?
                    get() = data.state.value as? T
            }
        }
    }

    /**
     * 设置状态
     *
     * 线程安全：可以从任何线程调用
     *
     * 如果状态已存在，会更新值；如果不存在，会创建新的状态。
     * 所有监听此状态的 Composable 会自动重组。
     *
     * Android 平台：同时会保存到 SavedStateHandle，支持进程死亡后恢复。
     *
     * @param stateKey 状态的唯一标识
     * @param state 状态值（Android 平台必须支持序列化）
     */
    inline fun <reified T> setState(
        stateKey: String = T::class.simpleName ?: T::class.toString(),
        state: T
    ) {
        val data = synchronized(lock) {
            stateDataMap.getOrPut(stateKey) {
                StateData(state = mutableStateOf(state))
            }
        }

        // 更新内存中的状态值
        data.state.value = state

        // 保存到持久化存储（Android 平台使用 SavedStateHandle）
        persistence.save(stateKey, state)
    }

    /**
     * 手动移除状态（不管是否有监听者）
     *
     * 一般情况下不需要手动调用，系统会在没有监听者时自动清理。
     * 但在某些场景下（如显式清理敏感数据），可以手动调用。
     *
     * 同时会从持久化存储中移除。
     *
     * @param stateKey 状态的唯一标识
     * @return 是否成功移除
     */
    inline fun <reified T> removeState(
        stateKey: String = T::class.simpleName ?: T::class.toString()
    ): Boolean {
        val removed = synchronized(lock) {
            stateDataMap.remove(stateKey) != null
        }
        if (removed) {
            persistence.remove(stateKey)
        }
        return removed
    }

    /**
     * 清空所有状态
     *
     * 慎用：会清空所有状态，包括仍有监听者的状态
     *
     * 注意：只清理内存中的状态，不清理持久化存储
     */
    fun clearAll() {
        synchronized(lock) {
            stateDataMap.clear()
        }
    }

    /**
     * 获取某个 key 的监听者数量
     *
     * @param stateKey 状态的唯一标识
     * @return 监听者数量
     */
    fun getListenerCount(stateKey: String): Int {
        return synchronized(lock) {
            stateDataMap[stateKey]?.listenerCount ?: 0
        }
    }

    /**
     * 获取所有 key 的监听者数量
     *
     * @return Map<状态Key, 监听者数量>
     */
    fun getAllListenerCounts(): Map<String, Int> {
        return synchronized(lock) {
            stateDataMap.mapValues { it.value.listenerCount }
        }
    }

    /**
     * 获取所有状态的 key
     *
     * @return 所有状态的 key 集合
     */
    fun getAllKeys(): Set<String> {
        return synchronized(lock) {
            stateDataMap.keys.toSet()
        }
    }

    /**
     * 检查某个状态是否存在
     *
     * @param stateKey 状态的唯一标识
     * @return 是否存在
     */
    fun hasState(stateKey: String): Boolean {
        return synchronized(lock) {
            stateDataMap.containsKey(stateKey)
        }
    }

    // ============ 内部实现 ============

    /**
     * 获取或创建 StateData（内部辅助方法）
     *
     * 如果状态不存在，会尝试从持久化存储恢复
     *
     * @param stateKey 状态的唯一标识
     * @return StateData 对象
     */
    @PublishedApi
    internal fun getOrCreateStateData(stateKey: String): StateData {
        return synchronized(lock) {
            stateDataMap.getOrPut(stateKey) {
                // 尝试从持久化存储恢复
                val savedValue = persistence.restore<Any?>(stateKey)
                StateData(state = mutableStateOf(savedValue))
            }
        }
    }

    /**
     * 注册监听者（线程安全）
     *
     * 由 StateBusListenerViewModel 调用
     */
    internal fun registerListener(stateKey: String) {
        val data = getOrCreateStateData(stateKey)
        data.incrementListenerCount()
    }

    /**
     * 取消注册监听者（线程安全）
     *
     * 由 StateBusListenerViewModel.onCleared() 调用
     *
     * 当监听者数量为 0 时，清理内存中的状态，但保留持久化存储
     * 这样可以在以下场景中恢复数据：
     * 1. 快速导航返回
     * 2. 进程死亡恢复（Android）
     */
    internal fun unregisterListener(stateKey: String) {
        synchronized(lock) {
            val data = stateDataMap[stateKey] ?: return@synchronized

            val newCount = data.decrementListenerCount()

            // 当没有监听者时，清理内存（保留持久化存储）
            if (newCount <= 0) {
                stateDataMap.remove(stateKey)
            }
        }
    }

    /**
     * StateBus 被清理时的回调
     *
     * 当 Activity/Fragment 销毁时触发
     */
    override fun onCleared() {
        synchronized(lock) {
            stateDataMap.clear()
        }
        super.onCleared()
    }
}

/**
 * 监听者 ViewModel（外部类）
 *
 * 绑定到 NavBackStackEntry 的生命周期
 * 当页面从后退栈中真正移除时，才会触发 onCleared()
 *
 * 注意：这是外部类，不是 inner class，避免持有 StateBus 的隐式引用
 */
class StateBusListenerViewModel(
    private val stateKey: String,
    private val stateBus: StateBus
) : ViewModel() {

    init {
        // 注册监听者
        stateBus.registerListener(stateKey)
    }

    override fun onCleared() {
        // 取消注册监听者
        stateBus.unregisterListener(stateKey)
        super.onCleared()
    }
}

/**
 * 创建或获取 StateBus 实例
 *
 * StateBus 绑定到 Activity/Fragment 级别的 ViewModelStoreOwner
 * 配置更改时（如屏幕旋转）会自动保留
 * Android 平台：进程死亡后会自动恢复状态（通过 SavedStateHandle）
 *
 * @return StateBus 实例
 */
@Composable
fun rememberStateBus(): StateBus {
    val persistence = rememberStatePersistence()
    return viewModel { StateBus(persistence) }
}
