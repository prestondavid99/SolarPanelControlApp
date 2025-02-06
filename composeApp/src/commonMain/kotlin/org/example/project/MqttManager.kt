package org.example.project

import MQTTClient
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import org.example.project.model.Payload
import org.jetbrains.compose.resources.ExperimentalResourceApi
import socket.tls.TLSClientSettings
import solarpanelcontrolapp.composeapp.generated.resources.Res
import kotlin.coroutines.CoroutineContext
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalUnsignedTypes::class)
class MqttManager : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val _clientState = MutableStateFlow<MQTTClient?>(null)
    val clientState: StateFlow<MQTTClient?> = _clientState.asStateFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private var statusUpdateCallback: ((Payload) -> Unit)? = null

    private var initialized = false

    fun setPayloadUpdateCallback(callback: (Payload) -> Unit) {
        statusUpdateCallback = callback
    }

    private val serverCertificate = "files/certs/AmazonRootCA1.pem"
    private val privateKey = "files/certs/private_key.pem.key"
    private val deviceCertificate = "files/certs/device_cert.pem.crt"

    companion object {
        private const val ADDRESS = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
        private const val PORT = 8883
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun init(topic: String) {
        println("Starting MQTT initialization...")
        if (initialized) return
        initialized = true
        try {
            println("Reading certificates...")
            val serverCert = Res.readBytes(serverCertificate).decodeToString()
            val clientCert = Res.readBytes(deviceCertificate).decodeToString()
            val clientKey = Res.readBytes(privateKey).decodeToString()
            println("Certificates loaded successfully.")

            try {
                println("Resolving hostname...")
                val address = resolveHostname(ADDRESS)
                println("Hostname resolved: $address")

                // Rest of the initialization code...
            } catch (e: Exception) {
                println("Failed to initialize MQTT client: ${e.message}")
                e.printStackTrace()
            }

            println("Creating MQTT client...")
            _clientState.value = MQTTClient(
                MQTTVersion.MQTT5,
                ADDRESS,
                PORT,
                TLSClientSettings(
                    serverCertificate = serverCert,
                    clientCertificate = clientCert,
                    clientCertificateKey = clientKey,
                ),
                onConnected = {
                    println("Connected to MQTT broker successfully.")
                    _connectionState.value = true
                    subscribe(topic)
                },
                onDisconnected = {
                    println("Connection to MQTT broker lost.")
                    _connectionState.value = false
                },
                debugLog = true, // Enable detailed packet logging
                publishReceived = { publish ->
                    // Log incoming messages
                    println("Received message on topic ${publish.topicName}")
                    val payloadString = publish.payload?.toByteArray()?.decodeToString()
                    println("Payload Received: $payloadString")
                    payloadString?.let { json ->
                        try {
                            val payload = Json.decodeFromString<Payload>(json)
                            statusUpdateCallback?.invoke(payload)
                        } catch (e: Exception) {
                            println("Failed to parse payload: ${e.message}")
                        }
                    }
                }
            )
            println("MQTT client created successfully.")

            println("Connecting to broker at $ADDRESS:$PORT...")
            _clientState.value?.runSuspend(Dispatchers.IO)

        } catch (e: Exception) {
            println("Failed to initialize MQTT client: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun resolveHostname(hostname: String): String {
        val client = HttpClient(CIO)
        return try {
            val response: HttpResponse = client.get("https://dns.google/resolve?name=$hostname")
            response.bodyAsText()
        } catch (e: Exception) {
            "Failed to resolve hostname: ${e.message}"
        } finally {
            client.close()
        }
    }

    fun subscribe(subscription: String) {
        try {
            println("Attempting to subscribe to topic: $subscription")
            _clientState.value?.subscribe(listOf(Subscription(subscription, SubscriptionOptions(Qos.AT_LEAST_ONCE))))
            println("Successfully subscribed to $subscription")
        } catch (e: Exception) {
            println("Failed to subscribe to topic $subscription: ${e.message}")
            e.printStackTrace()
        }
    }

    fun publish(topic: String, jsonString: String) {
        try {
            println("Attempting to publish message to topic $topic...")
            if (_connectionState.value) {
                _clientState.value?.publish(false, Qos.AT_MOST_ONCE, topic, jsonString.encodeToByteArray().toUByteArray())
                println("Successfully published message to $topic: $jsonString")
            }
        } catch (e: Exception) {
            _connectionState.value = false
            println("Failed to publish message to topic $topic: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            println("Stopping MQTT manager...")
            job.cancel()
            println("MQTT manager stopped successfully.")
        } catch (e: Exception) {
            println("Error while stopping MQTT manager: ${e.message}")
            e.printStackTrace()
        }
    }
}
