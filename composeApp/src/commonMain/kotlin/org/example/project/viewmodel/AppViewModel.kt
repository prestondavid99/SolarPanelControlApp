package org.example.project.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.MqttManager
import org.example.project.model.Payload

/**
 * AppViewModel: A ViewModel class responsible for managing UI-related data and business logic.
 * It handles MQTT communication and maintains the state of the application.
 *
 * @param topic The MQTT topic to subscribe to and publish messages. Defaults to "esp32/test".
 */
class AppViewModel(private val topic: String = "esp32/test"): ViewModel() {
    // MutableStateFlow to track the state of the array raising operation
    private val _raiseArray = MutableStateFlow(false)
    // Public StateFlow exposing the raise array state
    val raiseArray: StateFlow<Boolean> = _raiseArray.asStateFlow()

    // Reference to the MqttManager singleton for MQTT operations
    private val mqttManager = MqttManager

    // MutableStateFlow to store the current payload
    private val _payload = MutableStateFlow(Payload())
    // Public StateFlow exposing the current payload
    val payload: StateFlow<Payload> = _payload.asStateFlow()

    // StateFlow exposing the MQTT connection state from MqttManager
    val connectionState: StateFlow<Boolean> = mqttManager.connectionState

    /**
     * Initialization block for the ViewModel.
     * Sets up MQTT client and starts collecting payload updates.
     */
    init {
        println("CREATED NEW VIEWMODEL INSTANCE")
        viewModelScope.launch {
            try {
                println("Initializing MQTT client...")
                mqttManager.init(topic)
                println("MQTT client initialized and listening on topic: $topic")
                // Setup payload updates
                mqttManager.receivedPayload
                    .collect { payload ->
                        payload?.let {
                            _payload.value = it
                        }
                    }
            } catch (e: Exception) {
                println("Failed to initialize MQTT client: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Publishes a payload to the MQTT topic.
     * This function toggles the raise_array state and publishes the updated payload.
     *
     * @param payload The current payload to be updated and published.
     */
    fun publish(payload: Payload) {
        viewModelScope.launch(Dispatchers.IO) {  // Use IO dispatcher for network operations
            try {
                // Determine the new state explicitly
                val newValue = !_raiseArray.value
                _raiseArray.value = newValue
                // Create a new payload copying the new raise_array value
                val updatedPayload = payload.copy(raise_array = newValue.compareTo(false))
                mqttManager.publish(topic, updatedPayload)
            } catch (e: Exception) {
                // Update UI state with error
                println("Failed to publish message: ${e.message}")
            }
        }
    }
}
