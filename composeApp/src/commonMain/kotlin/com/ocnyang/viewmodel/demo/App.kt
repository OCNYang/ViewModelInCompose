package com.ocnyang.viewmodel.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ocnyang.viewmodelincompose.eventeffect.EventEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Demo navigation screens
 */
private enum class Screen {
    Home,
    EventEffectDemo,
    LaunchedEffectOnceDemo,
    SharedViewModelDemo,
    StateBusDemo
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Home) }

        Scaffold(modifier = Modifier.safeContentPadding()) { paddingValues ->
            when (currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        modifier = Modifier.padding(paddingValues),
                        onNavigateToEventEffect = { currentScreen = Screen.EventEffectDemo },
                        onNavigateToLaunchedEffectOnce = { currentScreen = Screen.LaunchedEffectOnceDemo },
                        onNavigateToSharedViewModel = { currentScreen = Screen.SharedViewModelDemo },
                        onNavigateToStateBus = { currentScreen = Screen.StateBusDemo }
                    )
                }
                Screen.EventEffectDemo -> {
                    EventEffectDemo(
                        modifier = Modifier.padding(paddingValues),
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.LaunchedEffectOnceDemo -> {
                    LaunchedEffectOnceDemo(
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.SharedViewModelDemo -> {
                    SharedViewModelDemo(
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.StateBusDemo -> {
                    StateBusDemo(
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}

/**
 * Home screen with navigation to demos
 */
@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToEventEffect: () -> Unit,
    onNavigateToLaunchedEffectOnce: () -> Unit,
    onNavigateToSharedViewModel: () -> Unit,
    onNavigateToStateBus: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ViewModelInCompose",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Library Demo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onNavigateToEventEffect) {
            Text("EventEffect Demo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToLaunchedEffectOnce) {
            Text("LaunchedEffectOnce Demo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToSharedViewModel) {
            Text("SharedViewModel Demo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToStateBus) {
            Text("StateBus Demo")
        }
    }
}

/**
 * EventEffect Demo screen
 */
@Composable
private fun EventEffectDemo(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val viewModel = viewModel { DemoViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastEvent by remember { mutableStateOf("No events yet") }

    // Use EventEffect to handle one-time events from ViewModel
    EventEffect(viewModel.events) { event ->
        when (event) {
            is DemoViewModel.UiEvent.ShowToast -> {
                lastEvent = "Toast: ${event.message}"
            }
            is DemoViewModel.UiEvent.Navigate -> {
                lastEvent = "Navigate to: ${event.route}"
            }
            is DemoViewModel.UiEvent.ShowSnackbar -> {
                lastEvent = "Snackbar: ${event.message}"
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onBack) {
                Text("← Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "EventEffect Demo",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { viewModel.onShowToastClick() }) {
                Text("Show Toast Event")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { viewModel.onNavigateClick() }) {
                Text("Navigate Event")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { viewModel.onShowSnackbarClick() }) {
                Text("Show Snackbar Event")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Last Event:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = lastEvent,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
