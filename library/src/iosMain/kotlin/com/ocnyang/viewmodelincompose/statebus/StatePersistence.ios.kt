package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable

/**
 * iOS 平台实现：返回 no-op 实现
 *
 * iOS 平台的状态恢复由系统管理，
 * 可以在未来通过 NSUserDefaults 或其他机制实现
 */
@Composable
actual fun rememberStatePersistence(): StatePersistence {
    return NoOpStatePersistence
}
