package com.ocnyang.viewmodelincompose.statebus

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Android 平台的 StatePersistence 实现
 *
 * 基于 SavedStateHandle 实现，支持进程死亡后状态恢复
 *
 * 注意事项：
 * - 状态类型必须支持序列化（Parcelable/Serializable/基本类型）
 * - 总状态大小建议 < 1MB（Bundle 限制）
 */
class SavedStateHandlePersistence(
    private val savedStateHandle: SavedStateHandle
) : StatePersistence {

    override fun <T> save(key: String, value: T) {
        try {
            savedStateHandle[key] = value
        } catch (e: Exception) {
            // 序列化失败（类型不支持序列化）
            // 记录警告但不影响功能（配置更改仍然有效）
            Log.w(TAG, "Failed to save state [$key] to SavedStateHandle (type not serializable?)", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> restore(key: String): T? {
        return try {
            savedStateHandle.get<T>(key)
        } catch (e: Exception) {
            // 反序列化失败
            Log.w(TAG, "Failed to restore state [$key] from SavedStateHandle", e)
            null
        }
    }

    override fun remove(key: String) {
        savedStateHandle.remove<Any>(key)
    }

    companion object {
        private const val TAG = "StatePersistence"
    }
}

/**
 * 持有 SavedStateHandle 的 ViewModel
 *
 * 用于通过 Compose 的 viewModel() 获取 SavedStateHandle
 */
internal class StatePersistenceHolder(
    val persistence: SavedStateHandlePersistence
) : ViewModel()

/**
 * Android 平台实现：创建基于 SavedStateHandle 的 StatePersistence
 */
@Composable
actual fun rememberStatePersistence(): StatePersistence {
    val holder = viewModel<StatePersistenceHolder>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val savedStateHandle = extras.createSavedStateHandle()
                @Suppress("UNCHECKED_CAST")
                return StatePersistenceHolder(SavedStateHandlePersistence(savedStateHandle)) as T
            }
        }
    )
    return holder.persistence
}
