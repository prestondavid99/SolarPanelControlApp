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

    private var _payload = ""
    val payload: String
        get() = _payload

    private val serverCertificate = "files/certs/AmazonRootCA1.pem"
    private val privateKey = "files/certs/private_key.pem.key"
    private val deviceCertificate = "files/certs/device_cert.pem.crt"

    companion object {
        private const val ADDRESS = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
        private const val PORT = 8883
    }

    var client: MQTTClient? = null

    @OptIn(ExperimentalResourceApi::class)
    fun init() {
        launch {
            client = MQTTClient(
                MQTTVersion.MQTT5,
                ADDRESS,
                PORT,
                TLSClientSettings(
                    serverCertificate = Res.readBytes(serverCertificate).decodeToString(),
                    clientCertificate = Res.readBytes(deviceCertificate).decodeToString(),
                    clientCertificateKey = Res.readBytes(privateKey).decodeToString(),
                )
            ) {
                println("Payload: ")
                println(it.payload?.toByteArray()?.decodeToString())
                _payload = it.payload?.toByteArray()?.decodeToString() ?: ""
            }
            client?.runSuspend(Dispatchers.IO)
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
            client?.publish(false, Qos.AT_MOST_ONCE, topic, message.encodeToByteArray().toUByteArray())
            println("Published to $topic: $message")
        }
    }

    fun stop() {
        job.cancel()
    }
}