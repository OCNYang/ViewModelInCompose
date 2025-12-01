package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable

/**
 * 状态持久化接口
 *
 * 用于在进程死亡后恢复状态（Android 平台通过 SavedStateHandle 实现）
 * 其他平台返回 no-op 实现
 */
interface StatePersistence {
    /**
     * 保存状态
     *
     * @param key 状态的唯一标识
     * @param value 状态值（必须支持序列化，如 Parcelable/Serializable/基本类型）
     */
    fun <T> save(key: String, value: T)

    /**
     * 恢复状态
     *
     * @param key 状态的唯一标识
     * @return 恢复的状态值，如果不存在或恢复失败则返回 null
     */
    fun <T> restore(key: String): T?

    /**
     * 移除状态
     *
     * @param key 状态的唯一标识
     */
    fun remove(key: String)
}

/**
 * 创建平台特定的 StatePersistence 实例
 *
 * - Android: 返回基于 SavedStateHandle 的实现，支持进程死亡恢复
 * - 其他平台: 返回 no-op 实现
 */
@Composable
expect fun rememberStatePersistence(): StatePersistence

/**
 * 空实现的 StatePersistence
 *
 * 用于不支持状态持久化的平台
 */
object NoOpStatePersistence : StatePersistence {
    override fun <T> save(key: String, value: T) {
        // no-op
    }

    override fun <T> restore(key: String): T? {
        return null
    }

    override fun remove(key: String) {
        // no-op
    }
}
