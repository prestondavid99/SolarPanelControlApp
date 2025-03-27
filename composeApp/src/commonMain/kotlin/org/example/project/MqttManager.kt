package org.example.project

import MQTTClient
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import org.example.project.model.Payload
import socket.tls.TLSClientSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.packets.mqtt.MQTTConnack
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.ReasonCode

/**
 * MqttManager: A singleton object responsible for managing MQTT communications with AWS IoT.
 * This manager handles connection, message publishing, subscription, and overall MQTT lifecycle.
 */
object MqttManager : CoroutineScope {

    // The coroutine context for this scope, using a SupervisorJob for error isolation
    // and IO dispatcher for network operations
    // Philip Lackner has a great YouTube playlist for understanding Kotlin Coroutines:
    // https://www.youtube.com/playlist?list=PLQkwcJG4YTCQcFEPuYGuv54nYai_lwil_
    override val coroutineContext = SupervisorJob() + Dispatchers.IO

    private lateinit var resourceLoader: ResourceLoader
    // Mutex specifically for connection-related operations
    private val connectionMutex = Mutex()

    // MutableStateFlow to track the current connection state
    private val _connectionState = MutableStateFlow(false)
    // Public StateFlow exposing the connection state
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    // MutableStateFlow to store the most recently received payload
    private val _receivedPayload = MutableStateFlow<Payload?>(null)
    // Public StateFlow exposing the received payload
    val receivedPayload: StateFlow<Payload?> = _receivedPayload.asStateFlow()

    // The MQTT client instance
    private var client: MQTTClient? = null
    // Flag to prevent multiple initializations
    private var initialized = false
    // The current MQTT topic being subscribed to
    private var currentTopic = ""

    /**Security certificates and keys from AWS**/
    private const val SERVER_CERT = "AmazonRootCA1.pem"
    private const val PRIVATE_KEY = "pkcs8_key.pem"
    private const val DEVICE_CERT = "device_cert.crt"

    /**AWS IoT Endpoint**/
    private const val AWS_ENDPOINT = "a31zhgc1ryhddv-ats.iot.us-west-1.amazonaws.com" // Replace this with YOUR endpoint
    // Standard MQTT over TLS port
    private const val PORT = 8883
    // Delay before attempting to reconnect after a disconnection
    private const val RECONNECT_DELAY = 5000L

    /**
     * Initializes the MQTT manager and establishes a connection to the AWS IoT broker.
     * @param topic The MQTT topic to subscribe to after connection is established.
     */
    suspend fun init(topic: String, customResourceLoader: ResourceLoader = ResourceLoader()) {
        println("Starting MQTT initialization...")
        resourceLoader = customResourceLoader
        if (initialized) return
        initialized = true
        currentTopic = topic

        try {
            val settings = createTlsSettings()
            client = MQTTClient(
                MQTTVersion.MQTT5,
                AWS_ENDPOINT,
                PORT,
                settings,
                onConnected = { handleConnected(it) },
                onDisconnected = { handleDisconnected() },
                publishReceived = { handleMessageReceived(it) },
                debugLog = true
            ).apply {
                runSuspend(Dispatchers.IO)
            }
        } catch (e: Exception) {
            initialized = false
            throw e
        }
    }

    /**
     * Creates TLS settings for secure communication with AWS IoT.
     * @return TLSClientSettings configured with necessary certificates and keys.
     */
    private suspend fun createTlsSettings(): TLSClientSettings {
        val (serverCert, clientCert, clientKey) = loadCertificates()
        return TLSClientSettings(
            serverCertificate = serverCert,
            clientCertificate = clientCert,
            clientCertificateKey = clientKey
        )
    }

    /**
     * Loads the required certificates and private key from resources.
     * @return A Triple containing the server certificate, client certificate, and client key as strings.
     */
    private suspend fun loadCertificates(): Triple<String, String, String> {
        val serverCert = resourceLoader.loadCertificateResource(SERVER_CERT).decodeToString()
        val clientCert = resourceLoader.loadCertificateResource(DEVICE_CERT).decodeToString()
        val clientKey = resourceLoader.loadCertificateResource(PRIVATE_KEY).decodeToString()
        return Triple(serverCert, clientCert, clientKey)
    }

    /**
     * Handles the connected event from the MQTT client.
     * @param flag The MQTT connection acknowledgment packet.
     */
    private fun handleConnected(flag: MQTTConnack) {
        _connectionState.value = true
        launch {
            subscribe(currentTopic)
        }
    }

    /**
     * Handles the disconnected event from the MQTT client.
     */
    private fun handleDisconnected() {
        _connectionState.value = false
    }

    /**
     * Processes incoming MQTT messages.
     * @param publish The MQTT publish packet containing the received message.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun handleMessageReceived(publish: MQTTPublish) {
        println("Received message on topic ${publish.topicName}")
        val payloadString = publish.payload?.toByteArray()?.decodeToString()
        println("Payload Received: $payloadString")
        payloadString?.let { json ->
            try {
                val payload = Json.decodeFromString<Payload>(json)
                _receivedPayload.value = payload
            } catch (e: Exception) {
                println("Failed to parse payload: ${e.message}")
            }
        }
    }

    /**
     * Attempts to reconnect to the MQTT broker after a disconnection.
     */
    private fun tryReconnect() {
        launch {
            connectionMutex.withLock {
                if (!_connectionState.value) {
                    delay(RECONNECT_DELAY)
                    client?.runSuspend(Dispatchers.IO)
                }
            }
        }
    }

    /**
     * Subscribes to a specified MQTT topic.
     * @param topic The MQTT topic to subscribe to.
     */
    private suspend fun subscribe(topic: String) {
        connectionMutex.withLock {
            client?.subscribe(
                listOf(Subscription(topic, SubscriptionOptions(Qos.AT_LEAST_ONCE)))
            )
        }
    }

    /**
     * Publishes a payload to a specified MQTT topic.
     * @param topic The MQTT topic to publish to.
     * @param payload The Payload object to serialize and publish.
     * @throws IllegalStateException If not connected to the MQTT broker.
     */
    suspend fun publish(topic: String, payload: Payload) {
        connectionMutex.withLock {
            if (!_connectionState.value) {
                throw IllegalStateException("Not connected to broker")
            }

            val jsonEncoder = Json { encodeDefaults = true }
            val jsonEncoded = jsonEncoder.encodeToString(Payload.serializer(), payload)

            client?.publish(
                false,
                Qos.AT_MOST_ONCE,
                topic,
                jsonEncoded.encodeToByteArray().toUByteArray()
            )
        }
    }

    /**
     * Shuts down the MQTT manager, disconnecting from the broker and canceling all coroutines.
     */
    fun shutdown() {
        client?.disconnect(ReasonCode.SERVER_SHUTTING_DOWN)
        coroutineContext.cancel()
    }
}
