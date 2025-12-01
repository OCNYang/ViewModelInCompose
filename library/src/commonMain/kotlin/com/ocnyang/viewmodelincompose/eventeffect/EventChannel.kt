package com.ocnyang.viewmodelincompose.eventeffect

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A channel-based event emitter for one-time UI events.
 *
 * Use this in your ViewModel to emit events that should only be consumed once,
 * such as navigation, showing toasts, or displaying snackbars.
 *
 * Features:
 * - Events are guaranteed to be delivered (won't be lost during config changes)
 * - Each event is consumed exactly once
 * - Supports suspending emission from coroutines
 *
 * Example usage in ViewModel:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     private val _events = EventChannel<MyEvent>()
 *     val events: Flow<MyEvent> = _events.flow
 *
 *     fun onButtonClick() {
 *         viewModelScope.launch {
 *             _events.send(MyEvent.ShowToast("Hello!"))
 *         }
 *     }
 * }
 * ```
 *
 * @param T The type of events to emit
 * @param capacity The capacity of the underlying channel. Default is [Channel.BUFFERED]
 *                 which provides a buffer to prevent event loss during brief UI unavailability.
 */
class EventChannel<T>(capacity: Int = Channel.BUFFERED) {

    private val channel = Channel<T>(capacity)

    /**
     * A flow that emits events from this channel.
     * Collect this flow in your Composable using [EventEffect].
     */
    val flow: Flow<T> = channel.receiveAsFlow()

    /**
     * Sends an event to the channel.
     * This is a suspending function that will suspend if the channel buffer is full.
     *
     * @param event The event to send
     */
    suspend fun send(event: T) {
        channel.send(event)
    }

    /**
     * Tries to send an event to the channel without suspending.
     * Returns true if the event was successfully sent, false otherwise.
     *
     * Use this when you need to send an event from a non-suspending context,
     * but be aware that events might be dropped if the buffer is full.
     *
     * @param event The event to send
     * @return true if the event was sent, false if the channel buffer is full
     */
    fun trySend(event: T): Boolean {
        return channel.trySend(event).isSuccess
    }
}
