package org.example.project.viewmodel

import MQTTClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.example.project.MqttManager
import org.example.project.model.Payload

class AppViewModel(topic: String = "/home"): ViewModel() {
    private val mqttManager = MqttManager()
    private val _payload = MutableStateFlow(Payload())
    val payload: StateFlow<Payload> = _payload.asStateFlow()
    val connectionState: StateFlow<Boolean> = mqttManager.connectionState



    init {
        viewModelScope.launch {
            try {
                println("Initializing MQTT client...")
                mqttManager.init(topic)
                println("MQTT client initialized and listening on topic: $topic")
                mqttManager.setPayloadUpdateCallback { newPayload ->
                    _payload.value = newPayload
                }
                mqttManager.connectionState.collect { connected ->
                    println("Connection state changed to: $connected")
                }
            } catch (e: Exception) {
                println("Failed to initialize MQTT client: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun subscribe(topic: String) {
        mqttManager.subscribe(topic)
    }

    fun publish(topic: String, payload: Payload) {
        viewModelScope.launch(Dispatchers.IO) {  // Use IO dispatcher for network operations
            try {
                mqttManager.publish(topic, Json.encodeToString(payload))
            } catch (e: Exception) {
                // Update UI state with error
                println("Failed to publish message: ${e.message}")
            }
        }
    }

    private fun fiveSecTimer() {
        var delayMillis = 0
        val waitMillis = 10000
        viewModelScope.launch {
            while (mqttManager.clientState.value == null) {
                delay(100)
                delayMillis += 100
                if (delayMillis % 1000 == 0)
                    println("WAITING FOR CLIENT TO INITIALIZE [${delayMillis/1000} seconds]")
                if (delayMillis >= waitMillis)
                    throw Exception("Client not initialized after ${delayMillis/1000} seconds of wait time")
            }
        }
    }
}