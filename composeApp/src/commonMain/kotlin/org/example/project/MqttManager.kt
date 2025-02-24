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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.packets.mqtt.MQTTConnack
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.ReasonCode

object MqttManager : CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.IO

    private val mutex = Mutex()
    private val connectionMutex = Mutex()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _receivedPayload = MutableStateFlow<Payload?>(null)
    val receivedPayload: StateFlow<Payload?> = _receivedPayload.asStateFlow()

    private var client: MQTTClient? = null
    private var initialized = false
    private var currentTopic = ""

    private val serverCertificatePath = "AmazonRootCA1.pem"
    private val privateKeyPath = "private_key.pem.key"
    private val deviceCertificatePath = "device_cert.pem.crt"

    private const val ADDRESS = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
    private const val PORT = 8883
    private const val RECONNECT_DELAY = 5000L


    @OptIn(ExperimentalResourceApi::class)
    suspend fun init(topic: String) {
        println("Starting MQTT initialization...")
        if (initialized) return
        initialized = true
        currentTopic = topic

        try {

            val settings = createTlsSettings()
            client = MQTTClient(
                MQTTVersion.MQTT5,
                ADDRESS,
                PORT,
                settings,
//                clientId = "solarPanelControl",
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

    private suspend fun createTlsSettings(): TLSClientSettings {
        val (serverCert, clientCert, clientKey) = loadCertificates()
        return TLSClientSettings(
            serverCertificate = serverCert,
            clientCertificate = clientCert,
            clientCertificateKey = clientKey
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadCertificates(): Triple<String, String, String> {
        val serverCert = loadCertificateResource(serverCertificatePath).decodeToString()
        val clientCert = loadCertificateResource(deviceCertificatePath).decodeToString()
        val clientKey = loadCertificateResource(privateKeyPath).decodeToString()
        return Triple(serverCert, clientCert, clientKey)
    }

    private fun handleConnected(flag: MQTTConnack) {
        _connectionState.value = true
        launch {
            subscribe(currentTopic)
        }
    }

    private fun handleDisconnected() {
        _connectionState.value = false
    }

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

    private suspend fun subscribe(topic: String) {
        connectionMutex.withLock {
            client?.subscribe(
                listOf(Subscription(topic, SubscriptionOptions(Qos.AT_LEAST_ONCE)))
            )
        }
    }

    suspend fun publish(topic: String, payload: Payload) {
        connectionMutex.withLock {
            if (!_connectionState.value) {
                throw IllegalStateException("Not connected to broker")
            }

            client?.publish(
                false,
                Qos.AT_MOST_ONCE,
                topic,
                Json.encodeToString(payload).encodeToByteArray().toUByteArray()
            )
        }
    }

    fun shutdown() {
        client?.disconnect(ReasonCode.SERVER_SHUTTING_DOWN)
        coroutineContext.cancel()
    }
}
