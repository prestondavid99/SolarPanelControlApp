package org.example.project.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.MqttManager
import org.example.project.model.Payload

class AppViewModel(private val topic: String = "dryrise"): ViewModel() {
    private val _raiseArray = MutableStateFlow(false)
    val raiseArray: StateFlow<Boolean> = _raiseArray.asStateFlow()
    private val mqttManager = MqttManager
    private val _payload = MutableStateFlow(Payload())
    val payload: StateFlow<Payload> = _payload.asStateFlow()
    val connectionState: StateFlow<Boolean> = mqttManager.connectionState


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

    fun publish(payload: Payload) {
        viewModelScope.launch(Dispatchers.IO) {  // Use IO dispatcher for network operations
            try {
                // Determine the new state explicitly
                val newValue = !_raiseArray.value
                _raiseArray.value = newValue
                // Create a new payload copying the new raise_array value
                val updatedPayload = payload.copy(raise_array = newValue.compareTo(false))
                mqttManager.publish(topic, updatedPayload)
                mqttManager.publish(topic, payload)
            } catch (e: Exception) {
                // Update UI state with error
                println("Failed to publish message: ${e.message}")
            }
        }
    }
}