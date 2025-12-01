package com.ocnyang.viewmodel.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ocnyang.viewmodelincompose.launchedeffectonce.LaunchedEffectOnce

/**
 * LaunchedEffectOnce Demo
 *
 * 演示 LaunchedEffectOnce 的各种使用场景
 */
@Composable
fun LaunchedEffectOnceDemo(
    onBack: () -> Unit
) {
    val logs = remember { mutableStateListOf<String>() }

    fun addLog(message: String) {
        val timestamp = logs.size + 1
        logs.add(0, "[$timestamp] $message")
        if (logs.size > 20) {
            logs.removeLast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "LaunchedEffectOnce Demo",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Demo 1: Basic usage - execute once
            BasicUsageDemo { addLog(it) }

            // Demo 2: With key - re-execute when key changes
            KeyChangeDemo { addLog(it) }

            // Demo 3: Multiple keys
            MultipleKeysDemo { addLog(it) }

            // Demo 4: Recomposition test
            RecompositionDemo { addLog(it) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Log output
        LogSection(logs = logs, onClear = { logs.clear() })
    }
}

/**
 * Demo 1: Basic usage - only execute once per page lifecycle
 */
@Composable
private fun BasicUsageDemo(onLog: (String) -> Unit) {
    var recomposeCount by remember { mutableIntStateOf(0) }

    // This will only execute once, even when recomposition happens
    LaunchedEffectOnce(viewModelKey = "basic_demo") {
        onLog("BasicUsage: LaunchedEffectOnce executed!")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Basic Usage",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LaunchedEffectOnce executes only once per page lifecycle.\n" +
                    "Click the button to trigger recomposition - it won't re-execute.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { recomposeCount++ }) {
                    Text("Recompose ($recomposeCount)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recomposed $recomposeCount times",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Demo 2: With key - re-execute when key changes
 */
@Composable
private fun KeyChangeDemo(onLog: (String) -> Unit) {
    var userId by remember { mutableStateOf("user_001") }

    // Re-execute when userId changes
    LaunchedEffectOnce(userId, viewModelKey = "key_change_demo") {
        onLog("KeyChange: Loading data for $userId")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "2. With Key",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Re-execute when key changes. Same key value won't trigger re-execution.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Current userId: $userId",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { userId = "user_001" }) {
                    Text("User 1")
                }
                Button(onClick = { userId = "user_002" }) {
                    Text("User 2")
                }
                Button(onClick = { userId = "user_003" }) {
                    Text("User 3")
                }
            }
        }
    }
}

/**
 * Demo 3: Multiple keys - re-execute when any key changes
 */
@Composable
private fun MultipleKeysDemo(onLog: (String) -> Unit) {
    var category by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("name") }

    // Re-execute when any key changes
    LaunchedEffectOnce(category, sortBy, viewModelKey = "multi_keys_demo") {
        onLog("MultiKeys: Loading category=$category, sortBy=$sortBy")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "3. Multiple Keys",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Re-execute when ANY key changes.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Category: $category, SortBy: $sortBy",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { category = "all" }) {
                    Text("All")
                }
                OutlinedButton(onClick = { category = "active" }) {
                    Text("Active")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { sortBy = "name" }) {
                    Text("By Name")
                }
                OutlinedButton(onClick = { sortBy = "date" }) {
                    Text("By Date")
                }
            }
        }
    }
}

/**
 * Demo 4: Recomposition test - prove it doesn't re-execute on recomposition
 */
@Composable
private fun RecompositionDemo(onLog: (String) -> Unit) {
    var counter by remember { mutableIntStateOf(0) }

    // This should only execute once, even with counter changes
    LaunchedEffectOnce(viewModelKey = "recomposition_demo") {
        onLog("Recomposition: Initial load (should appear only once)")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "4. Recomposition Test",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlike standard LaunchedEffect(Unit), this won't re-execute " +
                    "even after configuration changes (rotation).",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { counter++ }) {
                    Text("Counter: $counter")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check logs - no re-execution!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Log display section
 */
@Composable
private fun LogSection(
    logs: List<String>,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Execution Logs:",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedButton(onClick = onClear) {
                Text("Clear")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No logs yet...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    logs.forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
