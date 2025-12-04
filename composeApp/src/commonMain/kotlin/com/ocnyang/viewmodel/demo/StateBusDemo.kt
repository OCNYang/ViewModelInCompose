package com.ocnyang.viewmodel.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ocnyang.viewmodelincompose.statebus.LocalStateBus
import com.ocnyang.viewmodelincompose.statebus.ProvideStateBus

/**
 * StateBus Demo
 *
 * Demonstrates cross-screen state sharing using StateBus.
 * Common use case: Select data in Screen B and return to Screen A with the result.
 */

// Demo screens
private enum class StateBusScreen {
    ScreenA,
    ScreenB_PersonPicker,
    ScreenB_ColorPicker
}

// Data classes for the demo
data class Person(
    val id: Int,
    val name: String,
    val age: Int
)

data class ColorChoice(
    val name: String,
    val hex: Long
)

/**
 * Main entry point for StateBus Demo
 */
@Composable
fun StateBusDemo(
    onBack: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(StateBusScreen.ScreenA) }

    // Provide StateBus at the root
    ProvideStateBus {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "StateBus Demo",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            HorizontalDivider()

            // Screen content
            when (currentScreen) {
                StateBusScreen.ScreenA -> ScreenA(
                    onPickPerson = { currentScreen = StateBusScreen.ScreenB_PersonPicker },
                    onPickColor = { currentScreen = StateBusScreen.ScreenB_ColorPicker }
                )
                StateBusScreen.ScreenB_PersonPicker -> PersonPickerScreen(
                    onBack = { currentScreen = StateBusScreen.ScreenA }
                )
                StateBusScreen.ScreenB_ColorPicker -> ColorPickerScreen(
                    onBack = { currentScreen = StateBusScreen.ScreenA }
                )
            }
        }
    }
}

/**
 * Screen A - Main screen that observes state from StateBus
 */
@Composable
private fun ScreenA(
    onPickPerson: () -> Unit,
    onPickColor: () -> Unit
) {
    val stateBus = LocalStateBus.current

    // Observe states from StateBus
    val selectedPerson = stateBus.observeState<Person?>()
    val selectedColor = stateBus.observeState<ColorChoice?>()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Screen A (Receiver)",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This screen observes state from StateBus.\nNavigate to picker screens to select values.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Person selection card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Selected Person",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPerson != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = selectedPerson.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: ${selectedPerson.id}, Age: ${selectedPerson.age}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedButton(onClick = {
                            stateBus.removeState<Person?>()
                        }) {
                            Text("Clear")
                        }
                    }
                } else {
                    Text(
                        text = "No person selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPickPerson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick Person")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color selection card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Selected Color",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedColor != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Color preview box
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                drawRect(androidx.compose.ui.graphics.Color(selectedColor.hex))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = selectedColor.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "#${selectedColor.hex.toString(16).uppercase().padStart(8, '0')}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        OutlinedButton(onClick = {
                            stateBus.removeState<ColorChoice?>()
                        }) {
                            Text("Clear")
                        }
                    }
                } else {
                    Text(
                        text = "No color selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPickColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick Color")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // StateBus info card
        StateBusInfoCard()
    }
}

/**
 * Person Picker Screen - Screen B for selecting a person
 */
@Composable
private fun PersonPickerScreen(
    onBack: () -> Unit
) {
    val stateBus = LocalStateBus.current

    val people = remember {
        listOf(
            Person(1, "Alice", 28),
            Person(2, "Bob", 35),
            Person(3, "Charlie", 42),
            Person(4, "Diana", 31),
            Person(5, "Eve", 25)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Pick a Person",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select a person to pass back to Screen A via StateBus",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            people.forEach { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Set state in StateBus and navigate back
                            stateBus.setState<Person?>(state = person)
                            onBack()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Age: ${person.age}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "ID: ${person.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Color Picker Screen - Screen B for selecting a color
 */
@Composable
private fun ColorPickerScreen(
    onBack: () -> Unit
) {
    val stateBus = LocalStateBus.current

    val colors = remember {
        listOf(
            ColorChoice("Red", 0xFFFF0000),
            ColorChoice("Green", 0xFF00FF00),
            ColorChoice("Blue", 0xFF0000FF),
            ColorChoice("Yellow", 0xFFFFFF00),
            ColorChoice("Purple", 0xFF800080),
            ColorChoice("Orange", 0xFFFFA500),
            ColorChoice("Cyan", 0xFF00FFFF),
            ColorChoice("Pink", 0xFFFFC0CB)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Pick a Color",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select a color to pass back to Screen A via StateBus",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { color ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Set state in StateBus and navigate back
                            stateBus.setState<ColorChoice?>(state = color)
                            onBack()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .width(48.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            drawRect(androidx.compose.ui.graphics.Color(color.hex))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = color.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "#${color.hex.toString(16).uppercase().padStart(8, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card showing StateBus debug info
 */
@Composable
private fun StateBusInfoCard() {
    val stateBus = LocalStateBus.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "StateBus Debug Info",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            val allKeys = stateBus.getAllKeys()
            val listenerCounts = stateBus.getAllListenerCounts()

            if (allKeys.isEmpty()) {
                Text(
                    text = "No states in StateBus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                allKeys.forEach { key ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Listeners: ${listenerCounts[key] ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Note: States are automatically cleaned up when no observers remain.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
