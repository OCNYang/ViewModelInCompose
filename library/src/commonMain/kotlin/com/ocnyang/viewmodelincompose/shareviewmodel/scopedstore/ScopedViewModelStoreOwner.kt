package com.ocnyang.viewmodelincompose.shareviewmodel.scopedstore

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

/**
 * Scope 注册表
 *
 * 管理所有 [SharedViewModelStoreOwner] 的注册和生命周期。
 * 作为 ViewModel 实现，确保跨配置更改（如屏幕旋转）时保留状态。
 *
 * ## 架构
 *
 * ```
 * Activity/Fragment ViewModelStore
 *   └─ ScopedViewModelStoreOwner (ViewModel, 注册表)
 *       └─ Map<KClass, SharedViewModelStoreOwner>
 *           ├─ OrderFlowStoreOwner::class → OrderFlowStoreOwner
 *           │   └─ ViewModelStore
 *           │       └─ SharedOrderViewModel
 *           ├─ CheckoutStoreOwner::class → CheckoutStoreOwner
 *           │   └─ ViewModelStore
 *           │       └─ CheckoutViewModel
 *           └─ ...
 * ```
 *
 * ## 使用方式
 *
 * 通过 [ProvideScopedViewModelStoreOwner] 在根路由提供：
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideScopedViewModelStoreOwner {
 *         MyNavHost()
 *     }
 * }
 * ```
 *
 * @see SharedViewModelStoreOwner
 * @see ProvideScopedViewModelStoreOwner
 * @see RegisterSharedStoreOwner
 */
class ScopedViewModelStoreOwner : ViewModel() {

    private val lock = SynchronizedObject()

    /**
     * 已注册的 SharedViewModelStoreOwner 映射表
     */
    private val registry = mutableMapOf<KClass<out SharedViewModelStoreOwner>, SharedViewModelStoreOwner>()

    /**
     * 获取或创建指定类型的 [SharedViewModelStoreOwner]
     *
     * 如果已存在，返回现有实例；否则调用 factory 创建新实例并注册。
     *
     * @param key StoreOwner 的 KClass
     * @param factory 创建 StoreOwner 的工厂函数
     * @return StoreOwner 实例
     */
    fun <T : SharedViewModelStoreOwner> getOrPut(
        key: KClass<T>,
        factory: () -> T
    ): T {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry.getOrPut(key) { factory() } as T
        }
    }

    /**
     * 获取指定类型的 [SharedViewModelStoreOwner]
     *
     * @param key StoreOwner 的 KClass
     * @return StoreOwner 实例
     * @throws IllegalStateException 如果未注册
     */
    fun <T : SharedViewModelStoreOwner> get(key: KClass<T>): T {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry[key] as? T
                ?: error("${key.simpleName} not registered! Please call RegisterSharedStoreOwner in your NavHost first.")
        }
    }

    /**
     * 获取指定类型的 [SharedViewModelStoreOwner]，未注册返回 null
     *
     * @param key StoreOwner 的 KClass
     * @return StoreOwner 实例，或 null
     */
    fun <T : SharedViewModelStoreOwner> getOrNull(key: KClass<T>): T? {
        return synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            registry[key] as? T
        }
    }

    /**
     * 清理指定 Scope
     *
     * 清理 StoreOwner 中的所有 ViewModel 并从注册表移除。
     *
     * @param key StoreOwner 的 KClass
     * @return true 如果已清理，false 如果不存在
     */
    @PublishedApi
    internal fun clearScope(key: KClass<out SharedViewModelStoreOwner>): Boolean {
        return synchronized(lock) {
            val owner = registry[key] ?: return@synchronized false
            owner.clear()
            registry.remove(key)
            true
        }
    }

    /**
     * 检查指定 Scope 是否已注册
     *
     * @param key StoreOwner 的 KClass
     * @return true 如果已注册
     */
    fun isRegistered(key: KClass<out SharedViewModelStoreOwner>): Boolean {
        return synchronized(lock) {
            registry.containsKey(key)
        }
    }

    /**
     * 获取所有已注册的 Scope 信息（调试用）
     *
     * @return Map<Scope名称, 包含的路由集合>
     */
    fun getAllRegisteredInfo(): Map<String, Set<String>> {
        return synchronized(lock) {
            registry.mapKeys { it.key.simpleName ?: "Unknown" }
                .mapValues { entry ->
                    entry.value.includedRoutes.map { it.simpleName ?: "Unknown" }.toSet()
                }
        }
    }

    /**
     * 当 ViewModel 被清理时，清理所有注册的 StoreOwner
     */
    override fun onCleared() {
        synchronized(lock) {
            registry.values.forEach { it.clear() }
            registry.clear()
        }
        super.onCleared()
    }
}

/**
 * 创建并记住 [ScopedViewModelStoreOwner] 实例
 *
 * 内部使用，通过 [ProvideScopedViewModelStoreOwner] 调用
 */
@Composable
fun rememberScopedViewModelStoreOwner(): ScopedViewModelStoreOwner {
    return viewModel { ScopedViewModelStoreOwner() }
}
