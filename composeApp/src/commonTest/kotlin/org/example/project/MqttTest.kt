package org.example.project

import MQTTClient
import kotlinx.coroutines.*
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import socket.tls.TLSClientSettings
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalUnsignedTypes::class)
class MqttManager : CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    companion object {
        private const val address = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
        private const val serverCertificate = "../AmazonRootCA1.pem"
        private const val privateKey = "../private_key.pem.key"
        private const val deviceCertificate = "../device_cert.pem.crt"
        private const val port = 8883
    }

    private var client: MQTTClient? = null

    private fun init() {
        launch {
            client = MQTTClient(
                MQTTVersion.MQTT5,
                address,
                port,
                TLSClientSettings(
                    serverCertificate = serverCertificate,
                    clientCertificate = deviceCertificate,
                    clientCertificateKey = privateKey
                )
            ) {
                println("Payload: ")
                println(it.payload?.toByteArray()?.decodeToString())
            }
        }
    }

    fun subscribe(subscription: String) {
        client?.subscribe(listOf(Subscription(subscription, SubscriptionOptions(Qos.AT_LEAST_ONCE))))
        println("Subscribed to $subscription")
    }

    fun publish(topic: String, message: String) {
        client?.publish(false, Qos.EXACTLY_ONCE, topic, message.encodeToByteArray().toUByteArray())
        println("Published to $topic: $message")
    }

    fun start() {
        launch {
            client?.step()
        }
    }

    fun stop() {
        job.cancel()
    }
}