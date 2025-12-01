package com.ocnyang.viewmodelincompose.statebus

import androidx.compose.runtime.Composable

/**
 * WasmJS 平台实现：返回 no-op 实现
 *
 * Web 平台可以在未来通过 localStorage 实现持久化
 */
@Composable
actual fun rememberStatePersistence(): StatePersistence {
    return NoOpStatePersistence
}
