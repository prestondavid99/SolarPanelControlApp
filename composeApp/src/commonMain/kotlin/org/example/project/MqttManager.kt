package org.example.project

import MQTTClient
import kotlinx.coroutines.*
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import org.jetbrains.compose.resources.ExperimentalResourceApi
import socket.tls.TLSClientSettings
import solarpanelcontrolapp.composeapp.generated.resources.Res
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalUnsignedTypes::class)
class MqttManager : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val _payload = ""
    val payload: String
        get() = _payload

    companion object {
        private const val ADDRESS = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
        private const val SERVER_CERTIFICATE = "files/certs/AmazonRootCA1.pem"
        private const val PRIVATE_KEY = "files/certs/private_key.pem.key"
        private const val DEVICE_CERTIFICATE = "files/certs/device_cert.pem.crt"
        private const val PORT = 8883
    }

    private var client: MQTTClient? = null

    @OptIn(ExperimentalResourceApi::class)
    fun init() {
        launch {
            client = MQTTClient(
                MQTTVersion.MQTT5,
                ADDRESS,
                PORT,
                TLSClientSettings(
                    serverCertificate = Res.readBytes(SERVER_CERTIFICATE).decodeToString(),
                    clientCertificate = Res.readBytes(DEVICE_CERTIFICATE).decodeToString(),
                    clientCertificateKey = Res.readBytes(PRIVATE_KEY).decodeToString(),
                )
            ) {
                println("Payload: ")
                println(it.payload?.toByteArray()?.decodeToString())
            }

        }
    }

    fun subscribe(subscription: String) {
        launch {
            client?.subscribe(listOf(Subscription(subscription, SubscriptionOptions(Qos.AT_LEAST_ONCE))))
            println("Subscribed to $subscription")
        }
    }

    fun publish(topic: String, message: String) {
        launch {
            client?.publish(false, Qos.EXACTLY_ONCE, topic, message.encodeToByteArray().toUByteArray())
            println("Published to $topic: $message")
        }
    }

    fun start() {
        launch {
            client?.runSuspend()
        }
    }

    fun stop() {
        job.cancel()
    }
}