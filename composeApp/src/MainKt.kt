package org.example.project

import MQTTClient
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val client = MQTTClient(
        MQTTVersion.MQTT5,
        "test.mosquitto.org",
        1883,
        null
    ) {
        println(it.payload?.toByteArray()?.decodeToString())
    }
    client.subscribe(listOf(Subscription("/randomTopic", SubscriptionOptions(Qos.EXACTLY_ONCE))))
    client.publish(false, Qos.EXACTLY_ONCE, "/randomTopic", "hello".encodeToByteArray().toUByteArray())
    client.run() // Blocking method, use step() if you don't want to block the thread
}