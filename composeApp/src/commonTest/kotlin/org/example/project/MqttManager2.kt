package org.example.project

import MQTTClient
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.absolutePath
import io.matthewnelson.kmp.file.parentFile
import kotlinx.coroutines.*
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import socket.tls.TLSClientSettings
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalUnsignedTypes::class)
class MqttManager2 : CoroutineScope {
    private val job = Job()
    private val home = SysTempDir.parentFile?.parentFile?.parentFile?.absolutePath
    private val root = "StudioProjects/SolarPanelControlApp/composeApp/src/commonMain/composeResources/files/certs"
    private val serverCertificate = "$home/$root/AmazonRootCA1.pem"
    private val privateKey = "$home/$root/private_key.pem.key"
    private val deviceCertificate = "$home/$root/device_cert.pem.crt"
    private val address = "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com"
    private val port = 8883

    private lateinit var client: MQTTClient

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun subscribe(subscription: String) {
        client.subscribe(listOf(Subscription(subscription, SubscriptionOptions(Qos.AT_LEAST_ONCE))))
        println("Subscribed to $subscription")
    }

    init {
        launch {
            client = MQTTClient(
                MQTTVersion.MQTT5,
                address,
                port,
                TLSClientSettings(
                    serverCertificate = serverCertificate,
                    clientCertificate = deviceCertificate,
                    clientCertificateKey = privateKey
                ),
                keepAlive = 30,
                debugLog = true,
            ) {
                println("Payload: ${it.payload?.toByteArray()?.decodeToString()}")
            }
            client.runSuspend(Dispatchers.IO)
        }
    }

    fun publish(topic: String, message: String) {
        launch {
            client.publish(
                false,
                Qos.AT_LEAST_ONCE,
                topic,
                message.encodeToByteArray().toUByteArray()
            )
            println("Published to $topic: $message")
            delay(2000)
        }
    }

    fun stop() {
        job.cancel()
    }
}