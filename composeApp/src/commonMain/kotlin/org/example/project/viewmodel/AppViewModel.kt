package org.example.project.viewmodel

import MQTTClient
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.example.project.MqttManager
import org.example.project.model.Status

class AppViewModel(topic: String = "/home"): ViewModel() {
    private val _mqttManager = MqttManager()
    val mqttManager: MqttManager
        get() = _mqttManager

    private val _client = MutableStateFlow(_mqttManager.client)
    val client: StateFlow<MQTTClient?> = _client.asStateFlow()
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()



    init {
        viewModelScope.launch {
            try {
                println("Initializing MQTT client...")
                _mqttManager.init(topic)
                fiveSecTimer()
                println("MQTT client initialized and listening on topic: $topic")
                _client.value = _mqttManager.client
                _mqttManager.setStatusUpdateCallback { newStatus ->
                    _status.value = newStatus
                }
            } catch (e: Exception) {
                println("Failed to initialize MQTT client: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun subscribe(topic: String) {
        _mqttManager.subscribe(topic)

    }

    fun publish(topic: String, status: Status) {
        viewModelScope.launch {  // Use IO dispatcher for network operations
            try {
                _mqttManager.publish(topic, Json.encodeToString(status))
            } catch (e: Exception) {
                // Update UI state with error
                println("Failed to publish message: ${e.message}")
            }
        }
    }

    private fun fiveSecTimer() {
        var delayMillis = 0
        var waitMillis = 10000
        viewModelScope.launch {
            while (_mqttManager.client == null) {
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