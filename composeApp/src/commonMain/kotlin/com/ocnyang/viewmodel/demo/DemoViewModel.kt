package com.ocnyang.viewmodel.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ocnyang.viewmodelincompose.eventeffect.EventChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Demo ViewModel showcasing one-time event handling with EventChannel.
 */
class DemoViewModel : ViewModel() {

    private val _events = EventChannel<UiEvent>()
    val events: Flow<UiEvent> = _events.flow

    private var clickCount = 0

    fun onShowToastClick() {
        clickCount++
        viewModelScope.launch {
            _events.send(UiEvent.ShowToast("Button clicked $clickCount times!"))
        }
    }

    fun onNavigateClick() {
        viewModelScope.launch {
            _events.send(UiEvent.Navigate("details"))
        }
    }

    fun onShowSnackbarClick() {
        viewModelScope.launch {
            _events.send(UiEvent.ShowSnackbar("This is a snackbar message"))
        }
    }

    sealed interface UiEvent {
        data class ShowToast(val message: String) : UiEvent
        data class Navigate(val route: String) : UiEvent
        data class ShowSnackbar(val message: String) : UiEvent
    }
}
