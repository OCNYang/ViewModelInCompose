package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable

/**
 * JVM (Desktop) 平台实现：返回 no-op 实现
 *
 * Desktop 平台没有进程死亡恢复的概念，
 * 应用关闭后状态自然丢失
 */
@Composable
actual fun rememberStatePersistence(): StatePersistence {
    return NoOpStatePersistence
}
